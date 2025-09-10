package com.isai1703.pta.utils

import java.net.NetworkInterface
import java.util.*

/**
 * Utilidades de red: obtener IP local, etc.
 */
object NetworkUtils {

    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                for (addr in Collections.list(intf.inetAddresses)) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (ex: Exception) {
            null
        }
    }
}
