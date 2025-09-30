package com.isai1703.pta.model

/**
 * Tipos de dispositivos soportados en el proyecto:
 * - WIFI: Dispositivos accesibles por IP/HTTP genéricos
 * - BLUETOOTH: Dispositivos accesibles por Bluetooth clásico (ESP32, STM32, etc.)
 * - RASPBERRY_PI: Raspberry Pi detectado por puerto SSH (22)
 * - GENERIC_HTTP: Dispositivos HTTP en puertos 80/8080 (ESP32, servidores web)
 * - GENERIC_TCP: Dispositivos TCP genéricos
 * - DESCONOCIDO: Otros dispositivos no categorizados
 */
enum class DeviceType {
    WIFI,
    BLUETOOTH,
    RASPBERRY_PI,
    GENERIC_HTTP,
    GENERIC_TCP,
    DESCONOCIDO
}
