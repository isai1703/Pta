package com.isai1703.pta.manager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Cliente Bluetooth para conectarse a dispositivos como ESP32 o Raspberry.
 * Maneja la conexión, envío y recepción de datos.
 */
class BluetoothClient(private val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // UUID SPP estándar para comunicación Bluetooth Serial
    private val sppUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Conectar al dispositivo.
     */
    fun connect(): Boolean {
        return try {
            val tmpSocket = device.createRfcommSocketToServiceRecord(sppUUID)
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            tmpSocket.connect()

            socket = tmpSocket
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Enviar un comando al dispositivo.
     */
    fun sendCommand(command: String): String {
        return try {
            outputStream?.write(command.toByteArray())
            outputStream?.flush()

            // Leer respuesta si la hay
            val buffer = ByteArray(1024)
            val bytes = inputStream?.read(buffer) ?: -1
            if (bytes > 0) {
                String(buffer, 0, bytes)
            } else {
                "OK (sin respuesta)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    /**
     * Desconectar.
     */
    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
}

