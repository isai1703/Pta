package com.isai1703.pta

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    fun getDeviceIp(context: Context): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ usa LinkProperties
            val network = connectivityManager.activeNetwork
            val linkProperties = connectivityManager.getLinkProperties(network)
            val addresses = linkProperties?.linkAddresses
            val ipv4 = addresses?.firstOrNull { it.address is java.net.Inet4Address }
            ipv4?.address?.hostAddress ?: "0.0.0.0"
        } else {
            // Versiones anteriores usan WifiInfo
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                (ip and 0xff),
                (ip shr 8 and 0xff),
                (ip shr 16 and 0xff),
                (ip shr 24 and 0xff)
            )
        }
    }

    suspend fun scanNetwork(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        @Suppress("DEPRECATION")
        val ip = wifiManager.connectionInfo.ipAddress

        val subnet = String.format(
            "%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff)
        )

        val devices = mutableListOf<DeviceInfo>()
        val portsToCheck = listOf(80, 443, 22, 1883)

        for (i in 1..254) {
            val host = "$subnet.$i"
            try {
                val inetAddress = InetAddress.getByName(host)
                if (inetAddress.isReachable(150)) {
                    val openPorts = mutableListOf<Int>()
                    for (port in portsToCheck) {
                        if (isPortOpen(host, port, 150)) {
                            openPorts.add(port)
                        }
                    }
                    val type = identifyDevice(openPorts)
                    devices.add(DeviceInfo(host, type, ""))
                }
            } catch (_: IOException) {
            }
        }
        return@withContext devices
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (_: IOException) {
            false
        }
    }

    private fun identifyDevice(openPorts: List<Int>): String {
        return when {
            openPorts.contains(22) -> "Raspberry Pi / Linux (SSH)"
            openPorts.contains(1883) -> "Dispositivo IoT (MQTT)"
            openPorts.contains(80) || openPorts.contains(443) -> "ESP32 / WebServer"
            else -> "Desconocido"
        }
    }
}
