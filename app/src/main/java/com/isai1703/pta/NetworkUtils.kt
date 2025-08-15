package com.isai1703.pta

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors

data class Device(val ip: String, val tipo: String)

object NetworkUtils {

    fun scanDevices(context: Context, onProgress: (Int) -> Unit): List<Device> {
        val encontrados = mutableListOf<Device>()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipBase = getIpBase(wifiManager)

        val executor = Executors.newFixedThreadPool(20)
        for (i in 1..254) {
            val ip = "$ipBase.$i"
            executor.execute {
                val tipo = detectDeviceType(ip)
                if (tipo.isNotEmpty()) synchronized(encontrados) { encontrados.add(Device(ip, tipo)) }
                onProgress((i * 100) / 254)
            }
        }
        executor.shutdown()
        while (!executor.isTerminated) Thread.sleep(50)
        return encontrados
    }

    private fun detectDeviceType(ip: String): String {
        try {
            if (isPortOpen(ip, 80, 200)) return "ESP32"
            if (isPortOpen(ip, 22, 200)) return "Raspberry Pi"
            if (isPortOpen(ip, 502, 200)) return "STM32/Industrial"
            val url = URL("http://$ip")
            val conn = url.openConnection()
            conn.connectTimeout = 200
            conn.getInputStream().use { }
            return "Mini-PC embebido"
        } catch (e: Exception) {
            return ""
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress(ip, port), timeout); true }
        } catch (e: IOException) { false }
    }

    private fun getIpBase(wifiManager: WifiManager): String {
        val dhcp = wifiManager.dhcpInfo
        val ip = dhcp.ipAddress
        val byte1 = ip and 0xFF
        val byte2 = (ip shr 8) and 0xFF
        val byte3 = (ip shr 16) and 0xFF
        return "$byte3.$byte2.$byte1"
    }
}
