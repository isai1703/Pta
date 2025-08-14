package com.isai1703.pta

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.math.pow

object NetworkUtils {

    fun localIPv4(context: Context): String? {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wm.connectionInfo.ipAddress
            if (ipInt == 0) return null
            val ip = String.format("%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
            return ip
        } catch (_: Exception) { }
        // Fallback: enumerar interfaces
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is InetAddress) {
                        val host = a.hostAddress ?: continue
                        if (host.count { it == '.' } == 3 && !host.contains(':')) return host
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    fun subnetCandidates(ip: String): List<String> {
        val parts = ip.split(".")
        if (parts.size != 4) return (1..254).map { "192.168.1.$it" }
        val base = parts.take(3).joinToString(".")
        return (1..254).map { "$base.$it" }
    }

    fun isTcpOpen(host: String, port: Int, timeoutMs: Int = 200): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
