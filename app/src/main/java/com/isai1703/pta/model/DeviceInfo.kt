package com.tu_paquete.model

data class DeviceInfo(
    val ip: String,
    val type: String,
    val openPorts: List<Int> = emptyList(),
    val details: String = ""
)
