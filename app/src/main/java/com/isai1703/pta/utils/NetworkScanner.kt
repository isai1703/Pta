package com.isai1703.pta.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.*
import kotlin.math.min

data class NetDevice(
    val ip: String,
    val name: String,
    val type: String
)

object NetworkScanner {

    private val commonPorts = listOf(80, 8080, 5000, 1883) // HTTP, HTTP-alt, Flask/ESP, MQTT (para hints)
    private const val CONNECT_TIMEOUT_MS = 550
    private const val HTTP_READ_TIMEOUT_MS = 700

    suspend fun scanSubnetDeep(context: Context): List<NetDevice> = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet() ?: return@withContext emptyList()
        val ips = expandSubnet(subnet)
        // limitar a /24 si llega más grande
        val candidates = if (ips.size > 256) ips.take(256) else ips

        val chunks = candidates.chunked(32)
        val results = mutableListOf<NetDevice>()

        for (chunk in chunks) {
            val deferred = chunk.map { ip ->
                async(Dispatchers.IO) { probeHost(ip) }
            }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

    private fun probeHost(ip: String): NetDevice? {
        // Intentar conectar a puertos comunes: si algún puerto abre, lo consideramos activo
        var open = false
        var guessedName: String? = null
        var guessedType: String? = null

        for (p in commonPorts) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, p), CONNECT_TIMEOUT_MS)
                    open = true
                }
                // Si abre puerto HTTP, tratar de inferir tipo por respuesta
                if (p == 80 || p == 8080) {
                    val guess = httpFingerprint(ip, p)
                    guessedName = guessedName ?: guess.first
                    guessedType = guessedType ?: guess.second
                }
            } catch (_: Exception) {
                // puerto cerrado
            }
        }

        if (!open) {
            // fallback: ICMP/ping (suele estar bloqueado, pero intentamos)
            try {
                val addr = InetAddress.getByName(ip)
                if (addr.isReachable(CONNECT_TIMEOUT_MS)) open = true
            } catch (_: Exception) { }
        }

        if (!open) return null

        val type = guessedType ?: guessTypeByHeuristics(ip)
        val name = guessedName ?: reverseDns(ip) ?: "Dispositivo"
        return NetDevice(ip = ip, name = name, type = type)
    }

    private fun httpFingerprint(ip: String, port: Int): Pair<String?, String?> {
        return try {
            val url = URL("http://$ip:$port/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = HTTP_READ_TIMEOUT_MS
                requestMethod = "GET"
            }
            val server = conn.getHeaderField("Server") ?: ""
            val body = try {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } catch (_: Exception) { "" }

            conn.disconnect()

            val type = when {
                body.contains("ESP32", true) || server.contains("ESP", true) -> "ESP32"
                body.contains("Raspberry", true) || server.contains("raspberry", true) -> "Raspberry"
                server.contains("Apache", true) || server.contains("nginx", true) -> "Mini-PC"
                else -> "Mini-PC"
            }
            val name = when {
                body.contains("<title>", true) -> Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)
                else -> null
            }
            name to type
        } catch (_: Exception) {
            null to null
        }
    }

    private fun reverseDns(ip: String): String? = try {
        val addr = InetAddress.getByName(ip)
        val host = addr.canonicalHostName
        if (host != ip) host else null
    } catch (_: Exception) { null }

    private fun guessTypeByHeuristics(ip: String): String {
        // Heurística simple si no hay HTTP fingerprint
        return when {
            ip.endsWith(".1") -> "Gateway"
            else -> "Mini-PC"
        }
    }

    // -------- Subnet helpers --------
    data class Subnet(val base: String, val maskBits: Int)

    private fun getLocalSubnet(): Subnet? {
        // Obtener primera IPv4 privada
        val nifs = NetworkInterface.getNetworkInterfaces() ?: return null
        for (ni in Collections.list(nifs)) {
            if (!ni.isUp || ni.isLoopback) continue
            val addrs = ni.inetAddresses
            for (addr in Collections.list(addrs)) {
                if (addr is Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.168.")) {
                        // asumimos /24 si no podemos leer máscara (Android no expone fácilmente)
                        return Subnet(base = ip.substringBeforeLast("."), maskBits = 24)
                    }
                }
            }
        }
        return null
    }

    private fun expandSubnet(subnet: Subnet): List<String> {
        // Para /24:  base.x -> x=1..254
        val base = subnet.base
        val hosts = 254
        return (1..hosts).map { "$base.$it" }
    }
}
