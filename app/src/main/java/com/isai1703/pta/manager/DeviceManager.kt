package com.isai1703.pta.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.*

/**
 * DeviceManager:
 * Maneja la conexión y comunicación con dispositivos:
 *  - ESP32 (WiFi y Bluetooth)
 *  - Raspberry Pi
 *  - Otros compatibles
 */
class DeviceManager {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var wifiSocket: Socket? = null

    /**
     * Conexión genérica al dispositivo (WiFi o Bluetooth)
     */
    fun connectToDevice(device: DeviceInfo): Boolean {
        return when (device.type) {
            DeviceType.WIFI -> connectWifi(device.ip)
            DeviceType.BLUETOOTH -> connectBluetooth(device.ip)
            else -> false
        }
    }

    /**
     * Enviar comando genérico
     */
    fun sendCommand(command: String): String {
        return try {
            // Prioridad: WiFi primero
            wifiSocket?.let {
                return sendCommandWifi(it, command)
            }
            // Bluetooth si está disponible
            bluetoothSocket?.let {
                return sendCommandBluetooth(it, command)
            }
            "Error: no hay conexión activa"
        } catch (e: Exception) {
            "Error al enviar: ${e.message}"
        }
    }

    // ----------- WiFi -----------
    private fun connectWifi(ip: String?): Boolean {
        return try {
            if (ip.isNullOrEmpty()) return false
            disconnect() // cierra conexiones previas
            val url = URL("http://$ip/") // probamos conexión HTTP
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                wifiSocket = Socket()
                wifiSocket?.connect(InetSocketAddress(ip, 80), 2000)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun sendCommandWifi(socket: Socket, command: String): String {
        return try {
            val out = socket.getOutputStream()
            val request = "GET /$command HTTP/1.1\r\nHost: ${socket.inetAddress.hostAddress}\r\n\r\n"
            out.write(request.toByteArray())
            out.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line).append("\n")
            }
            response.toString()
        } catch (e: Exception) {
            "Error WiFi: ${e.message}"
        }
    }

    // ----------- Bluetooth -----------
    private fun connectBluetooth(macAddress: String?): Boolean {
        return try {
            if (macAddress.isNullOrEmpty()) return false
            disconnect() // cierra conexiones previas
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            val uuid: UUID = device.uuids?.firstOrNull()?.uuid
                ?: UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // SPP clásico
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            adapter.cancelDiscovery()
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sendCommandBluetooth(socket: BluetoothSocket, command: String): String {
        return try {
            val out = socket.outputStream
            val input = socket.inputStream
            out.write((command + "\n").toByteArray())
            out.flush()

            val buffer = ByteArray(1024)
            val bytes = input.read(buffer)
            if (bytes > 0) {
                String(buffer, 0, bytes)
            } else {
                "Sin respuesta del dispositivo"
            }
        } catch (e: Exception) {
            "Error BT: ${e.message}"
        }
    }

    // ----------- Desconexión -----------
    fun disconnect() {
        try {
            wifiSocket?.close()
        } catch (_: Exception) { }
        wifiSocket = null

        try {
            bluetoothSocket?.close()
        } catch (_: Exception) { }
        bluetoothSocket = null

        outputStream = null
    }
}
