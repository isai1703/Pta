package com.isai1703.pta.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.Collections

object NetworkScanner {

    private val commonPorts = listOf(80, 8080, 5000, 1883)
    private const val CONNECT_TIMEOUT_MS = 550
    private const val HTTP_READ_TIMEOUT_MS = 700

    // ---------------- Versión con parámetro de subred (compatible con MainActivity) ----------------
    suspend fun scanSubnetDeep(subnet: String): List<NetDevice> = withContext(Dispatchers.IO) {
        val ips = expandSubnet(subnet)
        val chunks = ips.chunked(32)
        val results = mutableListOf<NetDevice>()

        for (chunk in chunks) {
            val deferred = chunk.map { ip -> async { probeHost(ip) } }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

    // ---------------- Versión original sin parámetros ----------------
    suspend fun scanSubnetDeep(): List<NetDevice> = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet()?.base ?: return@withContext emptyList()
        scanSubnetDeep(subnet)
    }

    // ---------------- Escaneo de host individual ----------------
    private fun probeHost(ip: String): NetDevice? {
        var open = false
        var guessedName: String? = null
        var guessedType: String? = null

        for (p in commonPorts) {
            try {
                Socket().use { s -> s.connect(InetSocketAddress(ip, p), CONNECT_TIMEOUT_MS) }
                open = true

                if (p == 80 || p == 8080) {
                    val (name, type) = httpFingerprint(ip, p)
                    guessedName = guessedName ?: name
                    guessedType = guessedType ?: type
                }
            } catch (_: Exception) {}
        }

        if (!open) {
            try {
                val addr = InetAddress.getByName(ip)
                if (addr.isReachable(CONNECT_TIMEOUT_MS)) open = true
            } catch (_: Exception) {}
        }

        if (!open) return null

        return NetDevice(
            ip = ip,
            name = guessedName ?: reverseDns(ip) ?: "Dispositivo",
            type = guessedType ?: guessTypeByHeuristics(ip)
        )
    }

    // ---------------- Fingerprint HTTP ----------------
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

            val name = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.getOrNull(1)

            name to type
        } catch (_: Exception) {
            null to null
        }
    }

    // ---------------- Reverse DNS ----------------
    private fun reverseDns(ip: String): String? = try {
        val addr = InetAddress.getByName(ip)
        val host = addr.canonicalHostName
        if (host != ip) host else null
    } catch (_: Exception) { null }

    // ---------------- Heurística tipo dispositivo ----------------
    private fun guessTypeByHeuristics(ip: String): String {
        return if (ip.endsWith(".1")) "Gateway" else "Mini-PC"
    }

    // ---------------- Expandir subred ----------------
    private fun expandSubnet(subnetBase: String): List<String> {
        val base = subnetBase.trimEnd('.')
        return (1..254).map { "$base.$it" }
    }

    // ---------------- Obtener subred local ----------------
    private fun getLocalSubnet(): Subnet? {
        val nifs = NetworkInterface.getNetworkInterfaces() ?: return null
        for (ni in Collections.list(nifs)) {
            if (!ni.isUp || ni.isLoopback) continue
            for (addr in Collections.list(ni.inetAddresses)) {
                if (addr is Inet4Address) {
                    val ip = addr.hostAddress ?: continue
                    if (ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.168.")) {
                        return Subnet(base = ip.substringBeforeLast("."), maskBits = 24)
                    }
                }
            }
        }
        return null
    }

    private data class Subnet(val base: String, val maskBits: Int)
}
