package com.isai1703.pta.model

/**
 * Representa un dispositivo detectado o configurado manualmente
 * en la aplicación (ESP32, Raspberry Pi, dispositivos genéricos, etc.).
 *
 * Se conecta mediante el tipo definido en [DeviceType].
 */
data class DeviceInfo(
    val ip: String? = null,            // Dirección IP del dispositivo (si aplica)
    val port: Int? = null,             // Puerto de conexión (HTTP, TCP, SSH)
    val macAddress: String? = null,    // Dirección MAC (cuando esté disponible)
    val name: String? = null,          // Nombre amigable (ej: "Vending ESP32", "Raspberry")
    val type: DeviceType = DeviceType.UNKNOWN, // Tipo de dispositivo
    val user: String? = null,          // Usuario (para conexiones SSH)
    val password: String? = null       // Contraseña (para conexiones SSH)
) {

    /**
     * Devuelve una descripción amigable del dispositivo.
     */
    fun getDisplayName(): String {
        return name ?: ip ?: "Dispositivo desconocido"
    }

    /**
     * Genera la URL base para conexiones HTTP.
     * Ejemplo: http://192.168.1.50:80
     */
    fun getHttpBaseUrl(): String? {
        return if (ip != null && port != null) {
            "http://$ip:$port"
        } else if (ip != null) {
            "http://$ip"
        } else null
    }

    /**
     * Indica si el dispositivo soporta HTTP (ESP32, Raspberry, genéricos HTTP).
     */
    fun supportsHttp(): Boolean {
        return type == DeviceType.ESP32 ||
                type == DeviceType.RASPBERRY_PI ||
                type == DeviceType.GENERIC_HTTP
    }

    /**
     * Indica si el dispositivo soporta SSH (Raspberry Pi, genéricos SSH).
     */
    fun supportsSsh(): Boolean {
        return type == DeviceType.RASPBERRY_PI ||
                type == DeviceType.GENERIC_SSH
    }

    /**
     * Indica si el dispositivo soporta Bluetooth (ESP32, genéricos Bluetooth).
     */
    fun supportsBluetooth(): Boolean {
        return type == DeviceType.ESP32 ||
                type == DeviceType.GENERIC_BLUETOOTH
    }

    /**
     * Indica si el dispositivo soporta conexiones TCP genéricas.
     */
    fun supportsTcp(): Boolean {
        return type == DeviceType.GENERIC_TCP
    }
}
