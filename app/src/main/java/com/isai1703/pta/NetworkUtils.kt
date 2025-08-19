package com.isai1703.pta

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {

    /** Verifica si el dispositivo tiene conexión WiFi */
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    /** Obtiene la IP del dispositivo actual en la red WiFi */
    fun getDeviceIp(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff),
            (ip shr 24 and 0xff)
        )
    }

    /** Escanea la red local buscando dispositivos en el rango /24 */
    suspend fun scanNetwork(context: Context): List<DeviceInfo> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress

        val subnet = String.format(
            "%d.%d.%d",
            (ip and 0xff),
            (ip shr 8 and 0xff),
            (ip shr 16 and 0xff)
        )

        val devices = mutableListOf<DeviceInfo>()
        val portsToCheck = listOf(80, 443, 22, 1883) // HTTP, HTTPS, SSH, MQTT

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
                    // Nombre amigable: intenta hostname; si no, usa el tipo
                    val displayName = resolveHostName(inetAddress) ?: type
                    devices.add(DeviceInfo(host, type, displayName))
                }
            } catch (_: IOException) {
                // Ignorar host inaccesible/errores puntuales
            }
        }
        return@withContext devices
    }

    /** Intenta conectar a un puerto para saber si está abierto */
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

    /** Identifica tipo de dispositivo según puertos */
    private fun identifyDevice(openPorts: List<Int>): String {
        return when {
            22 in openPorts -> "Raspberry Pi / Linux (SSH)"
            1883 in openPorts -> "Dispositivo IoT (MQTT)"
            80 in openPorts || 443 in openPorts -> "ESP32 / WebServer"
            else -> "Desconocido"
        }
    }

    /** Intenta resolver nombre de host; devuelve null si no hay */
    private fun resolveHostName(inetAddress: InetAddress): String? {
        val hostName = inetAddress.hostName ?: return null
        val hostAddr = inetAddress.hostAddress ?: ""
        return if (hostName.isNotBlank() && hostName != hostAddr) hostName else null
    }
}
