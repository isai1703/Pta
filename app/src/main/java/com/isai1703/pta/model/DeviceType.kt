package com.isai1703.pta.model

/**
 * Tipos de dispositivos que la aplicación puede reconocer y manejar.
 * Cada tipo define las capacidades de conexión que tendrá (HTTP, SSH, Bluetooth, TCP).
 */
enum class DeviceType {
    /**
     * Microcontroladores ESP32.
     * Soportan:
     *  - HTTP (REST, endpoints como /command, /status, etc.)
     *  - Bluetooth (Serial Port Profile, comandos AT u otros personalizados).
     */
    ESP32,

    /**
     * Raspberry Pi con soporte de red.
     * Soportan:
     *  - HTTP (servidores Flask, Node.js, etc.)
     *  - SSH (ejecución de comandos reales en la terminal remota).
     */
    RASPBERRY_PI,

    /**
     * Dispositivos genéricos accesibles por HTTP.
     * Ejemplos:
     *  - ESP8266
     *  - Módulos IoT con servidor REST
     *  - Controladores industriales con API HTTP
     */
    GENERIC_HTTP,

    /**
     * Dispositivos genéricos accesibles por SSH.
     * Ejemplos:
     *  - Mini-PCs embebidos con Linux
     *  - Servidores locales
     *  - Routers con acceso SSH habilitado
     */
    GENERIC_SSH,

    /**
     * Dispositivos Bluetooth genéricos.
     * Ejemplos:
     *  - Módulos HC-05/HC-06
     *  - Otros controladores seriales Bluetooth
     */
    GENERIC_BLUETOOTH,

    /**
     * Dispositivos accesibles por TCP/IP directo (sockets).
     * Ejemplos:
     *  - PLCs
     *  - Equipos industriales con protocolo propietario
     */
    GENERIC_TCP,

    /**
     * Tipo desconocido.
     * Se asigna cuando no es posible identificar automáticamente
     * el tipo de dispositivo durante el escaneo.
     */
    UNKNOWN
}
