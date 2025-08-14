package com.tu_paquete.utils

import android.util.Log
import com.tu_paquete.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object NetworkScanner {

    // Puertos típicos a verificar
    private val commonPorts = listOf(22, 80, 443, 8080)

    suspend fun scanIP(ip: String): DeviceInfo? = withContext(Dispatchers.IO) {
        try {
            val openPorts = mutableListOf<Int>()

            // 1. Verificar puertos abiertos
            for (port in commonPorts) {
                if (isPortOpen(ip, port, 200)) {
                    openPorts.add(port)
                }
            }

            // 2. Intentar identificación por HTTP
            var type = "Desconocido"
            var details = ""

            if (openPorts.contains(80) || openPorts.contains(8080)) {
                val httpInfo = getHttpHeader(ip)
                details = httpInfo
                type = identifyDevice(httpInfo, openPorts)
            } else if (openPorts.contains(22)) {
                type = "Raspberry Pi (SSH)"
            }

            return@withContext DeviceInfo(ip, type, openPorts, details)

        } catch (e: Exception) {
            Log.e("NetworkScanner", "Error escaneando $ip: ${e.message}")
            return@withContext null
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getHttpHeader(ip: String): String {
        return try {
            val url = URL("http://$ip")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 500
            connection.readTimeout = 500
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader().use(BufferedReader::readText)
        } catch (e: Exception) {
            ""
        }
    }

    private fun identifyDevice(httpResponse: String, ports: List<Int>): String {
        val lower = httpResponse.lowercase()

        return when {
            "raspberry" in lower -> "Raspberry Pi"
            "esp32" in lower -> "ESP32"
            "stm32" in lower -> "STM32"
            "arduino" in lower -> "Arduino"
            ports.contains(22) && ports.contains(80) -> "Mini-PC embebido"
            else -> "Dispositivo desconocido"
        }
    }
}
