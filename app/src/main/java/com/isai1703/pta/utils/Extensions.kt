package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType

/**
 * Extensiones para conversión entre tipos de red y nuestro modelo interno
 */
data class NetDevice(
    val ip: String?,
    val port: Int?,
    val mac: String?,
    val name: String?,
    val type: DeviceType
)

fun NetDevice.asDeviceInfo(): DeviceInfo {
    return DeviceInfo(
        ip = this.ip,
        port = this.port,
        macAddress = this.mac,
        name = this.name,
        type = this.type
    )
}
