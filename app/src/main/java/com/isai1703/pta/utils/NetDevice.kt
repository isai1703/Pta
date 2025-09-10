package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceType

/**
 * Representación rápida y genérica de un dispositivo detectado en la red (resultado del scanner).
 * Usar nullable para poder representar distintos orígenes (ARP, mDNS, SNMP, ping, BT).
 */
data class NetDevice(
    val ip: String? = null,
    val port: Int? = null,
    val mac: String? = null,
    val name: String? = null,
    val type: DeviceType = DeviceType.UNKNOWN
)
