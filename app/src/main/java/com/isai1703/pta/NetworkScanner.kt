package com.isai1703.pta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object NetworkScanner {

    /**
     * Escanea un rango de IPs en la red local y devuelve una lista de dispositivos disponibles
     */
    suspend fun scanSubnet(subnet: String, port: Int = 80): List<NetDevice> {
        val devices = mutableListOf<NetDevice>()

        withContext(Dispatchers.IO) {
            val jobs = (1..254).map { host ->
                val ip = "$subnet.$host"
                try {
                    val reachable = InetAddress.getByName(ip).isReachable(300)
                    if (reachable) {
                        try {
                            Socket().use { socket ->
                                socket.connect(java.net.InetSocketAddress(ip, port), 300)
                                devices.add(NetDevice(ip, ip, port))
                            }
                        } catch (_: Exception) {
                            // puerto no accesible, igual lo añadimos
                            devices.add(NetDevice(ip, ip, port))
                        }
                    }
                } catch (_: Exception) {
                    // ignorar
                }
            }
        }

        return devices
    }
}
