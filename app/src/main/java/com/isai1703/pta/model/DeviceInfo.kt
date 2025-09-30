package com.isai1703.pta.model

/**
 * Representa un dispositivo detectado en red o por Bluetooth.
 * Compatible con ESP32, Raspberry Pi, Android embebido y otros.
 */
data class DeviceInfo(
    val ip: String?,              // IP para dispositivos WiFi o dirección MAC en Bluetooth
    val name: String,             // Nombre visible (ej. "ESP32", "Raspberry", etc.)
    val type: DeviceType          // Tipo de dispositivo
) {
    override fun toString(): String {
        return "$name (${ip ?: "sin dirección"})"
    }

    /**
     * Método de ayuda para convertir información detectada
     * desde el escaneo de red en un DeviceInfo.
     */
    companion object {
        fun fromWifi(ip: String, name: String = "Dispositivo WiFi"): DeviceInfo {
            return DeviceInfo(ip = ip, name = name, type = DeviceType.WIFI)
        }

        fun fromBluetooth(mac: String, name: String = "Dispositivo BT"): DeviceInfo {
            return DeviceInfo(ip = mac, name = name, type = DeviceType.BLUETOOTH)
        }
    }
}
