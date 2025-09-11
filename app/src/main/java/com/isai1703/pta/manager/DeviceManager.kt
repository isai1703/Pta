package com.isai1703.pta.manager

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import com.isai1703.pta.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DeviceManager: instancia de cliente por dispositivo, conecta y envía comandos.
 * Uso:
 *   val mgr = DeviceManager()
 *   mgr.connectToDevice(device)
 *   mgr.sendCommand("dispense?motor=1")
 */
class DeviceManager {

    private var client: DeviceClient? = null
    private var currentDevice: DeviceInfo? = null

    /**
     * Conectar a un dispositivo según su tipo.
     */
    suspend fun connectToDevice(device: DeviceInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            currentDevice = device
            client = createClientFor(device)
            val ok = client?.connect() ?: false
            ok
        } catch (e: Exception) {
            Log.e("DeviceManager", "connectToDevice error: ${e.message}")
            false
        }
    }

    /**
     * Crear el cliente correcto según DeviceType.
     */
    private fun createClientFor(device: DeviceInfo): DeviceClient? {
        return when (device.type) {
            DeviceType.ESP32,
            DeviceType.GENERIC_HTTP -> {
                if (!device.ip.isNullOrBlank()) Esp32HttpClient(device.ip!!, device.port ?: 80) else null
            }

            DeviceType.RASPBERRY_PI,
            DeviceType.GENERIC_SSH -> {
                if (!device.ip.isNullOrBlank() && !device.user.isNullOrBlank() && !device.password.isNullOrBlank()) {
                    RaspberryPiSshClient(
                        host = device.ip!!,
                        user = device.user!!,
                        password = device.password!!,
                        port = device.port ?: 22
                    )
                } else if (!device.ip.isNullOrBlank()) {
                    // fallback HTTP si no hay credenciales
                    Esp32HttpClient(device.ip!!, device.port ?: 80)
                } else null
            }

            DeviceType.GENERIC_TCP -> {
                if (!device.ip.isNullOrBlank() && device.port != null) {
                    TcpSocketClient(device.ip!!, device.port)
                } else null
            }

            DeviceType.BLUETOOTH -> {
                val mac = device.macAddress ?: return null
                val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
                val btDevice = adapter.getRemoteDevice(mac)
                BluetoothClient(btDevice)
            }

            else -> {
                if (!device.ip.isNullOrBlank()) Esp32HttpClient(device.ip!!, device.port ?: 80) else null
            }
        }
    }

    /**
     * Enviar un comando al cliente ya conectado.
     */
    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val c = client ?: return@withContext "Error: not connected"
            c.sendCommand(command)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Conectar a un dispositivo, enviar un comando y desconectar si falla.
     */
    suspend fun sendCommandToDevice(device: DeviceInfo, command: String): String {
        val ok = connectToDevice(device)
        return if (ok) sendCommand(command) else "Error: connect failed"
    }

    /**
     * Desconectar cliente actual.
     */
    fun disconnect() {
        try {
            client?.disconnect()
        } catch (_: Exception) {
        }
        client = null
        currentDevice = null
    }
}
