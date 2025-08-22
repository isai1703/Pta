package com.isai1703.pta.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.*

object NetworkScanner {

    private val commonPorts = listOf(80, 8080, 5000, 1883)
    private const val CONNECT_TIMEOUT_MS = 550
    private const val HTTP_READ_TIMEOUT_MS = 700

    suspend fun scanSubnetDeep(): List<NetDevice> = withContext(Dispatchers.IO) {
        val subnet = getLocalSubnet() ?: return@withContext emptyList()
        val ips = expandSubnet(subnet)
        val chunks = ips.chunked(32)
        val results = mutableListOf<NetDevice>()

        for (chunk in chunks) {
            val deferred = chunk.map { ip -> async { probeHost(ip) } }
            results += deferred.awaitAll().filterNotNull()
        }
        results
    }

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
                body.contains("Raspberry", true) || server.contains("raspberry", true) -> "Raspberry"
                server.contains("Apache", true) || server.contains("nginx", true) -> "Mini-PC"
                else -> "Mini-PC"
            }

            val name = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)
            name to type
        } catch (_: Exception) { null to null }
    }

    private fun reverseDns(ip: String): String? = try {
        val addr = InetAddress.getByName(ip)
        val host = addr.canonicalHostName
        if (host != ip) host else null
    } catch (_: Exception) { null }

    private fun guessTypeByHeuristics(ip: String): String {
        return if (ip.endsWith(".1")) "Gateway" else "Mini-PC"
    }

    private data class Subnet(val base: String, val maskBits: Int)

    private fun getLocalSubnet(): Subnet? {
        val nifs = NetworkInterface.getNetworkInterfaces() ?: return null
        for (ni in Collections.list(nifs)) {
            if (!ni.isUp || ni.isLoopback) continue
            for (addr in Collections.list(ni.inetAddresses)) {
                if (addr is java.net.Inet4Address) {
                    val ip = addr.hostAddress
                    if (ip.startsWith("10.") || ip.startsWith("172.") || ip.startsWith("192.168.")) {
                        return Subnet(base = ip.substringBeforeLast("."), maskBits = 24)
                    }
                }
            }
        }
        return null
    }

    private fun expandSubnet(subnet: Subnet): List<String> {
        val base = subnet.base
        return (1..254).map { "$base.$it" }
    }
}
