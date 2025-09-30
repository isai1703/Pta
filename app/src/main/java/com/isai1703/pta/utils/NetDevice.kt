package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType

/**
 * Clase auxiliar usada por NetworkScanner para representar
 * un dispositivo detectado durante el escaneo de red.
 */
data class NetDevice(
    val ip: String,
    val port: Int,
    val name: String,
    val mac: String?,
    val type: DeviceType
) {
    /**
     * Convierte este NetDevice a DeviceInfo para usar en el resto de la app
     */
    fun asDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            ip = ip,
            name = name,
            type = type
        )
    }
}
