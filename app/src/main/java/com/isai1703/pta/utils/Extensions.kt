package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType

/**
 * Representación temporal de dispositivo detectado en la red (scanner).
 */
data class NetDevice(
    val ip: String?,
    val port: Int? = null,
    val mac: String? = null,
    val name: String? = null,
    val type: DeviceType = DeviceType.UNKNOWN
)

/**
 * Convierte a nuestro modelo persistente DeviceInfo
 * Ten en cuenta que DeviceInfo debe contener los campos usados.
 */
fun NetDevice.asDeviceInfo(): DeviceInfo {
    return DeviceInfo(
        ip = this.ip,
        port = this.port,
        macAddress = this.mac,
        name = this.name,
        type = this.type
    )
}
