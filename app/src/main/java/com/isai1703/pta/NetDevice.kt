package com.isai1703.pta

data class NetDevice(
    val ip: String,
    val hostname: String? = null,
    val port: Int = 80
)
