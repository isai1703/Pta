package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType

/**
 * Representación descubierta en red (temporal) - se convierte a DeviceInfo.
 */
data class NetDevice(
    val ip: String,
    val port: Int? = null,
    val mac: String? = null,
    val name: String? = null,
    val type: DeviceType = DeviceType.GENERIC_HTTP
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
