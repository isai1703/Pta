package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceType

object DeviceIdentifier {
    
    /**
     * Identifica el dispositivo por su IP y devuelve un nombre descriptivo
     */
    fun identifyDevice(ip: String): DeviceIdentification {
        return when {
            // IPs específicas conocidas (personaliza según tus dispositivos)
            ip == "192.168.1.100" -> DeviceIdentification(
                name = "Máquina Nochebuena Principal",
                type = DeviceType.GENERIC_HTTP,
                description = "Expendedora 60 espirales"
            )
            ip == "192.168.1.101" -> DeviceIdentification(
                name = "Máquina Nochebuena #2",
                type = DeviceType.GENERIC_HTTP,
                description = "Expendedora secundaria"
            )
            ip == "192.168.0.100" -> DeviceIdentification(
                name = "Control Principal ESP32",
                type = DeviceType.GENERIC_HTTP,
                description = "Controlador de espirales"
            )
            
            // Identificación por rango de IP
            ip.startsWith("192.168.1.") -> {
                val lastOctet = ip.substringAfterLast(".").toIntOrNull() ?: 0
                when {
                    lastOctet in 100..110 -> DeviceIdentification(
                        name = "Máquina Nochebuena",
                        type = DeviceType.GENERIC_HTTP,
                        description = "Expendedora en red principal"
                    )
                    lastOctet in 200..210 -> DeviceIdentification(
                        name = "Raspberry Pi Control",
                        type = DeviceType.RASPBERRY_PI,
                        description = "Sistema de gestión"
                    )
                    else -> DeviceIdentification(
                        name = "Dispositivo en Red 1",
                        type = DeviceType.DESCONOCIDO,
                        description = "Red principal 192.168.1.x"
                    )
                }
            }
            
            ip.startsWith("192.168.0.") -> DeviceIdentification(
                name = "Dispositivo en Red 0",
                type = DeviceType.DESCONOCIDO,
                description = "Red secundaria 192.168.0.x"
            )
            
            ip.startsWith("192.168.2.") -> DeviceIdentification(
                name = "Dispositivo en Red 2",
                type = DeviceType.DESCONOCIDO,
                description = "Red extendida 192.168.2.x"
            )
            
            // IPs que parecen ser repetidores
            ip.endsWith(".254") || ip.endsWith(".253") -> DeviceIdentification(
                name = "Repetidor WiFi",
                type = DeviceType.DESCONOCIDO,
                description = "Extensor de red"
            )
            
            // IPs típicas de routers
            ip.endsWith(".1") -> DeviceIdentification(
                name = "Router/Gateway",
                type = DeviceType.DESCONOCIDO,
                description = "Puerta de enlace"
            )
            
            // Dispositivo desconocido
            else -> DeviceIdentification(
                name = "Dispositivo Desconocido",
                type = DeviceType.DESCONOCIDO,
                description = "IP: $ip"
            )
        }
    }
    
    /**
     * Obtiene un nombre amigable que incluye tanto la identificación como la IP
     */
    fun getDisplayName(ip: String): String {
        val identification = identifyDevice(ip)
        return "${identification.name} ($ip)"
    }
    
    /**
     * Verifica si la IP corresponde a una máquina expendedora conocida
     */
    fun isVendingMachine(ip: String): Boolean {
        return ip.startsWith("192.168.1.1") && 
               ip.substringAfterLast(".").toIntOrNull() in 100..110
    }
}

/**
 * Clase que contiene la información de identificación de un dispositivo
 */
data class DeviceIdentification(
    val name: String,
    val type: DeviceType,
    val description: String
)
