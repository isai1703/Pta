package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceType
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.min

object NetworkScanner {
    // Puertos específicos para máquinas expendedoras
    private val vendingMachinePorts = listOf(
        80,    // HTTP estándar
        8080,  // HTTP alternativo
        8081,  // Posible puerto de control
        5000,  // Flask/APIs comunes
        3000,  // Node.js APIs
        9000   // Otro puerto común
    )

    suspend fun scanMultipleSubnetsWithProgress(
        subnetBases: List<String>,
        chunkSize: Int = 32,
        timeoutMs: Int = 300,
        progressCb: (scanned: Int, total: Int, foundDevice: NetDevice?) -> Unit
    ): NetDevice? = withContext(Dispatchers.IO) {
        val ips = mutableListOf<String>()
        for (base in subnetBases) {
            for (i in 1..254) ips += "$base.$i"
        }
        val total = ips.size
        var scanned = 0
        val sem = Semaphore(chunkSize)
        var found: NetDevice? = null
        
        val jobs = ips.map { ip ->
            async {
                sem.acquire()
                try {
                    if (found != null) return@async
                    
                    for (port in vendingMachinePorts) {
                        try {
                            // Intenta conexión TCP primero
                            val socket = Socket()
                            socket.connect(InetSocketAddress(ip, port), 1000)  // 1 segundo                            
                            // Si conecta, verifica si es la máquina expendedora
                            val isVendingMachine = checkIfVendingMachine(ip, port)
                            socket.close()
                            
                            if (isVendingMachine) {
                                // Identifica el dispositivo por su IP
                                val identification = com.isai1703.pta.utils.DeviceIdentifier.identifyDevice(ip)
                                val nd = NetDevice(
                                    ip = ip,
                                    port = port,
                                    name = identification.name,
                                    mac = null,
                                    type = identification.type
                                )
                                found = nd
                                break
                            }
                        } catch (_: Exception) {
                            // Puerto no abierto o timeout
                        }
                    }
                } finally {
                    scanned++
                    progressCb(scanned, total, found)
                    sem.release()
                }
            }
        }
        jobs.forEach { it.join() }
        found
    }
    
    /**
     * Verifica si el dispositivo es una máquina expendedora
     * Busca endpoints típicos o respuestas características
     */
    private fun checkIfVendingMachine(ip: String, port: Int): Boolean {
        return try {
            // Endpoints comunes en máquinas expendedoras
            val testEndpoints = listOf(
                "/api/status",
                "/status",
                "/dispense",
                "/motor",
                "/vending",
                "/"
            )
            
            for (endpoint in testEndpoints) {
                try {
                    val url = URL("http://$ip:$port$endpoint")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    conn.requestMethod = "GET"
                    conn.connect()
                    
                    val responseCode = conn.responseCode
                    val response = if (responseCode in 200..299) {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    } else ""
                    
                    conn.disconnect()
                    
                    // Busca palabras clave relacionadas con máquinas expendedoras
                    val vendingKeywords = listOf(
                        "vending",
                        "dispense",
                        "motor",
                        "coil",
                        "spiral",
                        "product",
                        "nochebuena",
                        "android"
                    )
                    
                    if (vendingKeywords.any { response.lowercase().contains(it) }) {
                        return true
                    }
                    
                    // Si responde en algún endpoint típico, es probable que sea la máquina
                    if (responseCode in 200..299 && endpoint in listOf("/api/status", "/status", "/dispense")) {
                        return true
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
