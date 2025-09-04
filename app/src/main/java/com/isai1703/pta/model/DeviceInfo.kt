package com.isai1703.pta.model

/**
 * Modelo genérico de dispositivo en red o Bluetooth.
 * Compatible con ESP32, Raspberry Pi, etc.
 */
data class DeviceInfo(
    val ip: String? = null,
    val port: Int? = null,
    val macAddress: String? = null,
    val name: String? = null,
    val type: DeviceType
)

enum class DeviceType {
    WIFI,
    BLUETOOTH
}
