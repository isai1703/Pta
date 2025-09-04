package com.isai1703.pta

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import com.isai1703.pta.utils.NetDevice

/**
 * Utilidades para trabajar con escaneo de red y conversión de dispositivos.
 */
object NetworkUtils {

    fun scanLocalNetwork(): List<DeviceInfo> {
        // TODO: implementar escaneo real
        return listOf(
            DeviceInfo(ip = "192.168.1.50", port = 80, name = "ESP32-Test", type = DeviceType.WIFI),
            DeviceInfo(macAddress = "00:11:22:33:44:55", name = "RaspberryPi-BT", type = DeviceType.BLUETOOTH)
        )
    }

    fun asNetDevice(info: DeviceInfo): NetDevice {
        return NetDevice(
            ip = info.ip,
            port = info.port,
            mac = info.macAddress,
            name = info.name,
            type = info.type
        )
    }
}
