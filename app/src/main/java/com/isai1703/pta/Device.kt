package com.isai1703.pta

enum class TipoDispositivo { ESP32, RASPBERRY, STM32, MINIPC, DESCONOCIDO }

data class Device(
    val nombre: String,
    val address: String,        // IP o MAC
    val tipo: TipoDispositivo,
    var conectado: Boolean = false
)
