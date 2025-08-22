package com.isai1703.pta.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket

object NetworkScanner {

    /**
     * Escaneo profundo de la subred local (1-254)
     * Devuelve lista de NetDevice con IP y nombre básico
     */
    suspend fun scanSubnetDeep(subnet: String = "192.168.1"): List<NetDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NetDevice>()

        for (host in 1..254) {
            val ip = "$subnet.$host"
            try {
                val reachable = InetAddress.getByName(ip).isReachable(300)
                if (reachable) {
                    // Intentamos puerto 80, si está abierto asumimos tipo "Mini-PC" (puedes ajustar)
                    val typeGuess = try {
                        Socket().use { socket ->
                            socket.connect(java.net.InetSocketAddress(ip, 80), 300)
                        }
                        "Mini-PC"
                    } catch (_: Exception) {
                        "ESP32/Raspberry"
                    }

                    devices.add(NetDevice(ip = ip, type = typeGuess, name = ip))
                }
            } catch (_: Exception) {
                // ignorar IPs no alcanzables
            }
        }

        devices
    }
}
