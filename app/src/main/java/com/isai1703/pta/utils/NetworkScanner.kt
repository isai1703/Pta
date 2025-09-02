package com.isai1703.pta.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.*

object NetworkScanner {

    private val commonPorts = listOf(80, 8080, 5000, 1883)
    private const val CONNECT_TIMEOUT_MS = 550
    private const val HTTP_READ_TIMEOUT_MS = 700

    // Subredes comunes fallback (puedes modificar/agregar)
    private val commonSubnets = listOf("192.168.0", "192.168.1", "192.168.100", "10.0.0")

    /**
     * Escanea la subred indicada (base: "192.168.1") y devuelve NetDevice list.
     * Internamente hace chunked y usa coroutines para paralelizar.
     */
    suspend fun scanSubnetDeep(subnet: String, chunkSize: Int = 32): List<NetDevice> = withContext(Dispatchers.IO) {
        val ips = expandSubnet(subnet)
        val chunks = ips.chunked(chunkSize)
        val results = mutableListOf<NetDevice>()
        for (chunk in chunks) {
            val deferred = chunk.map { ip -> async { probeHost(ip) } }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

    /**
     * Escaneo con reporte de progreso por chunk. progress(scanned, total)
     */
    suspend fun scanSubnetDeepWithProgress(
        subnet: String,
        chunkSize: Int = 32,
        progress: (scanned: Int, total: Int) -> Unit
    ): List<NetDevice> = withContext(Dispatchers.IO) {
        val ips = expandSubnet(subnet)
        val total = ips.size
        var scanned = 0
        val chunks = ips.chunked(chunkSize)
        val results = mutableListOf<NetDevice>()
        for (chunk in chunks) {
            val deferred = chunk.map { ip -> async { probeHost(ip) } }
            val found = deferred.awaitAll().filterNotNull()
            results += found
            scanned += chunk.size
            try { progress(scanned, total) } catch (_: Exception) {}
        }
        results
    }

    /**
     * Escanea subred local + subredes comunes buscando la máquina.
     * progress(scannedTotal, totalToScan, found)
     */
    suspend fun scanForMachineWithProgress(chunkSize: Int = 32,
                                           progress: (scanned: Int, total: Int, found: NetDevice?) -> Unit
    ): NetDevice? = withContext(Dispatchers.IO) {
        val subnets = mutableListOf<String>()
        getLocalSubnet()?.base?.let { subnets.add(it) }
        for (s in commonSubnets) if (!subnets.contains(s)) subnets.add(s)
        val totalToScan = subnets.size * 254
        var scannedTotal = 0

        for (subnet in subnets) {
            val ips = expandSubnet(subnet)
            val chunks = ips.chunked(chunkSize)
            for (chunk in chunks) {
                val deferred = chunk.map { ip -> async { probeHost(ip) } }
                val found = deferred.awaitAll().filterNotNull()
                scannedTotal += chunk.size
                val vending = found.firstOrNull { it.type in listOf("ESP32", "STM32") || it.name.contains("Machine", true) }
                try { progress(scannedTotal, totalToScan, vending) } catch (_: Exception) {}
                if (vending != null) return@withContext vending
            }
        }
        try { progress(scannedTotal, totalToScan, null) } catch (_: Exception) {}
        null
    }

    // Intenta puertos comunes y fingerprint HTTP
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

        return NetDevice(ip = ip, name = guessedName ?: reverseDns(ip) ?: "Dispositivo", type = guessedType ?: guessTypeByHeuristics(ip))
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
            val body = try { BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() } } catch (_: Exception) { "" }
            conn.disconnect()

            val type = when {
                body.contains("ESP32", true) || server.contains("ESP", true) -> "ESP32"
                body.contains("STM32", true) -> "STM32"
                body.contains("Raspberry", true) || server.contains("raspberry", true) -> "Raspberry"
                server.contains("Apache", true) || server.contains("nginx", true) -> "Mini-PC"
                else -> "Mini-PC"
            }

            val name = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)
            name to type
        } catch (_: Exception) {
            null to null
        }
    }

    private fun reverseDns(ip: String): String? = try {
        val host = InetAddress.getByName(ip).canonicalHostName
        if (host != ip) host else null
    } catch (_: Exception) { null }

    private fun guessTypeByHeuristics(ip: String): String = if (ip.endsWith(".1")) "Gateway" else "Mini-PC"

    private fun expandSubnet(subnetBase: String): List<String> {
        val base = subnetBase.trimEnd('.')
        return (1..254).map { "$base.$it" }
    }

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
