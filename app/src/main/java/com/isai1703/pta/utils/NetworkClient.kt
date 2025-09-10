package com.isai1703.pta.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.*

/**
 * Cliente de red unificado: HTTP, TCP y Bluetooth.
 * (SSH queda como placeholder para futuras mejoras).
 */
object NetworkClient {

    // ----------- HTTP
    fun sendHttpRequest(device: DeviceInfo, command: String): String {
        return try {
            val url = URL("http://${device.ip}:${device.port ?: 80}/$command")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Error HTTP: ${e.message}"
        }
    }

    // ----------- TCP
    fun sendTcpRequest(device: DeviceInfo, command: String): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(device.ip, device.port ?: 23), 3000)
            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.write(command + "\n")
            writer.flush()
            val response = reader.readLine()

            socket.close()
            response ?: "Sin respuesta"
        } catch (e: Exception) {
            "Error TCP: ${e.message}"
        }
    }

    // ----------- Bluetooth (RFCOMM)
    fun sendBluetoothRequest(device: DeviceInfo, command: String): String {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val btDevice: BluetoothDevice = adapter.getRemoteDevice(device.macAddress ?: device.ip)

            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SerialPortServiceClass_UUID
            val socket: BluetoothSocket = btDevice.createRfcommSocketToServiceRecord(uuid)
            socket.connect()

            val output = socket.outputStream
            val input = socket.inputStream

            output.write((command + "\n").toByteArray())
            output.flush()

            val buffer = ByteArray(1024)
            val bytes = input.read(buffer)
            val response = String(buffer, 0, bytes)

            socket.close()
            response
        } catch (e: Exception) {
            "Error BT: ${e.message}"
        }
    }

    // ----------- SSH (Placeholder)
    fun sendSshRequest(device: DeviceInfo, command: String): String {
        return "SSH aún no implementado para ${device.ip}"
    }
}
