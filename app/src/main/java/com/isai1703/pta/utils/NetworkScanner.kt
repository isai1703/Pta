package com.isai1703.pta.utils

import com.isai1703.pta.model.DeviceType
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Semaphore
import kotlin.math.min

/**
 * Escanea múltiples subredes (ej: "192.168.0", "192.168.1") y detecta hosts con puertos abiertos (80,22...).
 * Llamada:
 *   val found = NetworkScanner.scanMultipleSubnetsWithProgress(listOf("192.168.0","192.168.1"), chunkSize=32) { scanned,total,netDevice -> ... }
 */
object NetworkScanner {
    private val defaultPorts = listOf(80, 22, 8080, 23)

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
                    for (port in defaultPorts) {
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(ip, port), timeoutMs)
                            socket.close()
                            // Device found on ip:port
                            val type = when (port) {
                                22 -> DeviceType.RASPBERRY_PI
                                80, 8080 -> DeviceType.GENERIC_HTTP
                                else -> DeviceType.GENERIC_TCP
                            }
                            val nd = NetDevice(ip = ip, port = port, name = ip, mac = null, type = type)
                            found = nd
                            break
                        } catch (_: Exception) {
                            // not open
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
}
