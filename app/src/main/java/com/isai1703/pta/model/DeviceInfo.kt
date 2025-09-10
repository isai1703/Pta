package com.isai1703.pta.model

data class DeviceInfo(
    val ip: String? = null,
    val port: Int? = null,
    val macAddress: String? = null,
    val name: String? = null,
    val type: DeviceType = DeviceType.GENERIC_HTTP,
    val user: String? = null,
    val password: String? = null,
    val openPorts: List<Int> = emptyList(),
    val details: String = ""
) {
    fun getDisplayName(): String = name ?: ip ?: "Desconocido"
}

