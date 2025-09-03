package com.isai1703.pta.model

data class DeviceInfo(
    val ip: String,
    val type: String,
    val openPorts: List<Int> = emptyList(),
    val details: String = ""
)
