package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.*

/**
 * Escáner de red para detectar dispositivos (ESP32, Raspberry Pi, STM32, Mini-PC, etc).
 * Detecta la subred local y prueba conexiones HTTP.
 */
object NetworkScanner {

    /**
     * Escanea toda la subred en bloques (chunkSize).
     * Reporta progreso y devuelve el primer dispositivo encontrado.
     */
    suspend fun scanForMachineWithProgress(
        chunkSize: Int = 32,
        onProgress: (scanned: Int, total: Int, found: DeviceInfo?) -> Unit
    ): DeviceInfo? = withContext(Dispatchers.IO) {
        val localIp = getLocalIp() ?: return@withContext null
        val subnet = localIp.substringBeforeLast(".")
        val total = 254
        var scanned = 0
        var foundDevice: DeviceInfo? = null

        for (i in 1..254 step chunkSize) {
            val end = (i + chunkSize - 1).coerceAtMost(254)
            val range = (i..end)

            val jobs = range.map { host ->
                val target = "$subnet.$host"
                kotlin.runCatching {
                    val ip = InetAddress.getByName(target)
                    if (ip.isReachable(300)) {
                        // Intentar consulta HTTP
                        val url = URL("http://$target/")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 500
                        conn.readTimeout = 500
                        conn.requestMethod = "GET"
                        conn.connect()
                        val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        conn.disconnect()

                        // Identificar dispositivo según la respuesta
                        val type = when {
                            response.contains("ESP32", true) -> "ESP32"
                            response.contains("Raspberry", true) -> "Raspberry Pi"
                            response.contains("STM32", true) -> "STM32"
                            else -> "Dispositivo"
                        }
                        foundDevice = DeviceInfo(ip = target, type = type, name = type)
                    }
                }
            }
            scanned += jobs.size
            onProgress(scanned, total, foundDevice)

            if (foundDevice != null) return@withContext foundDevice
        }
        return@withContext foundDevice
    }

    /**
     * Obtiene la IP local del dispositivo Android.
     */
    private fun getLocalIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }
}
