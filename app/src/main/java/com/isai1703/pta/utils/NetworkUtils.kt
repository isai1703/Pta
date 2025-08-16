package com.isai1703.pta.utils

import java.util.concurrent.Executors

data class Device(val ip: String, val type: String)

object NetworkUtils {

    fun scanDevices(onProgress: (Device, Int) -> Unit) {
        val ips = listOf("192.168.0.101", "192.168.0.102", "192.168.0.103") // ejemplo
        val executor = Executors.newFixedThreadPool(5)
        val total = ips.size
        for ((index, ip) in ips.withIndex()) {
            executor.submit {
                val deviceType = detectDevice(ip)
                onProgress(Device(ip, deviceType), ((index + 1) * 100) / total)
            }
        }
        executor.shutdown()
    }

    private fun detectDevice(ip: String): String {
        // Lógica de detección según puertos o HTTP fingerprint
        return when {
            ip.endsWith("101") -> "ESP32"
            ip.endsWith("102") -> "Raspberry Pi"
            ip.endsWith("103") -> "STM32"
            else -> "Mini-PC"
        }
    }
}
