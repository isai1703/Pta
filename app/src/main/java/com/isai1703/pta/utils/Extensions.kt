package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType

/**
 * Extensiones y utilidades de conversión entre dispositivos de red
 * y nuestro modelo interno DeviceInfo.
 *
 * Soporta ESP32, Raspberry Pi y otros dispositivos detectados.
 */

/**
 * Representa un dispositivo detectado en la red o por Bluetooth.
 */
data class NetDevice(
    val ip: String? = null,
    val port: Int? = null,
    val mac: String? = null,
    val name: String? = null,
    val type: DeviceType = DeviceType.UNKNOWN
)

/**
 * Convierte un NetDevice en nuestro modelo DeviceInfo
 */
fun NetDevice.asDeviceInfo(): DeviceInfo {
    return DeviceInfo(
        ip = this.ip,
        port = this.port,
        macAddress = this.mac,
        name = this.name ?: when (this.type) {
            DeviceType.ESP32 -> "ESP32 Device"
            DeviceType.RASPBERRY_PI -> "Raspberry Pi"
            DeviceType.GENERIC_HTTP -> "Generic HTTP Device"
            DeviceType.GENERIC_SSH -> "Generic SSH Device"
            else -> "Unknown Device"
        },
        type = this.type
    )
}

/**
 * Extensión para identificar si el dispositivo soporta conexión HTTP.
 */
fun NetDevice.supportsHttp(): Boolean {
    return this.type == DeviceType.ESP32 ||
            this.type == DeviceType.RASPBERRY_PI ||
            this.type == DeviceType.GENERIC_HTTP
}

/**
 * Extensión para identificar si el dispositivo soporta conexión SSH.
 */
fun NetDevice.supportsSsh(): Boolean {
    return this.type == DeviceType.RASPBERRY_PI ||
            this.type == DeviceType.GENERIC_SSH
}

/**
 * Extensión para identificar si el dispositivo soporta conexión Bluetooth.
 */
fun NetDevice.supportsBluetooth(): Boolean {
    return this.type == DeviceType.ESP32
}
