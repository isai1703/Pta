package com.isai1703.pta.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import com.isai1703.pta.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DeviceManager centraliza la lógica de selección de cliente según el tipo de dispositivo.
 * Permite enviar comandos reales por HTTP, SSH, Bluetooth (RFCOMM) o TCP.
 */
object DeviceManager {

    private const val TAG = "DeviceManager"

    /**
     * Crea el cliente adecuado según el tipo de dispositivo.
     */
    private fun createClient(device: DeviceInfo): DeviceClient? {
        return when (device.type) {
            DeviceType.ESP32 -> {
                when {
                    device.ip != null -> Esp32HttpClient(device.ip, device.port ?: 80)
                    device.macAddress != null -> createBluetoothClient(device)
                    else -> null
                }
            }

            DeviceType.RASPBERRY_PI -> {
                when {
                    device.user != null && device.password != null && device.ip != null -> {
                        RaspberryPiSshClient(
                            host = device.ip,
                            user = device.user,
                            password = device.password,
                            port = device.port ?: 22
                        )
                    }
                    device.ip != null -> Esp32HttpClient(device.ip, device.port ?: 80) // fallback HTTP
                    else -> null
                }
            }

            DeviceType.GENERIC_HTTP -> {
                device.ip?.let { Esp32HttpClient(it, device.port ?: 80) }
            }

            DeviceType.GENERIC_SSH -> {
                if (device.user != null && device.password != null && device.ip != null) {
                    RaspberryPiSshClient(
                        host = device.ip,
                        user = device.user,
                        password = device.password,
                        port = device.port ?: 22
                    )
                } else null
            }

            DeviceType.GENERIC_BLUETOOTH -> createBluetoothClient(device)

            DeviceType.GENERIC_TCP -> {
                if (device.ip != null && device.port != null) {
                    TcpSocketClient(device.ip, device.port)
                } else null
            }

            else -> null
        }
    }

    /**
     * Envía un comando real al dispositivo, seleccionando automáticamente el cliente.
     */
    suspend fun sendCommand(device: DeviceInfo, command: String): String =
        withContext(Dispatchers.IO) {
            val client = createClient(device)
            if (client != null) {
                try {
                    Log.d(TAG, "Enviando comando a ${device.getDisplayName()} -> $command")
                    return@withContext client.sendCommand(command)
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando comando: ${e.message}")
                    return@withContext "Error enviando comando: ${e.message}"
                }
            } else {
                return@withContext "Cliente no disponible para ${device.getDisplayName()}"
            }
        }

    /**
     * Crea un cliente Bluetooth usando RFCOMM si el dispositivo tiene MAC registrada.
     */
    private fun createBluetoothClient(device: DeviceInfo): DeviceClient? {
        return try {
            val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || device.macAddress == null) {
                Log.e(TAG, "Bluetooth no disponible o MAC no especificada")
                return null
            }
            val btDevice: BluetoothDevice = adapter.getRemoteDevice(device.macAddress)
            BluetoothClient(btDevice)
        } catch (e: Exception) {
            Log.e(TAG, "Error creando cliente Bluetooth: ${e.message}")
            null
        }
    }
}
