package com.isai1703.pta.utils

data class DeviceInfo(
    val ip: String,
    val mac: String = "",
    val hostname: String = "",
    val type: String = "Desconocido"
)
