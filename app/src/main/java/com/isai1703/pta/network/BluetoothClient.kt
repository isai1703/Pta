package com.isai1703.pta.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Cliente Bluetooth para enviar comandos a dispositivos como ESP32 o Raspberry Pi.
 */
class BluetoothClient(private val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID SPP estándar para comunicación serie
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Conecta al dispositivo Bluetooth.
     */
    fun connect(): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()
            outputStream = socket?.outputStream
            true
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    /**
     * Envía un comando por Bluetooth.
     */
    fun sendCommand(command: String): String {
        return try {
            if (socket == null || outputStream == null) {
                return "Error: No conectado"
            }
            outputStream?.write(command.toByteArray())
            outputStream?.flush()
            "OK"
        } catch (e: IOException) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    /**
     * Desconecta el cliente.
     */
    fun disconnect() {
        try {
            outputStream?.close()
        } catch (_: Exception) { }
        try {
            socket?.close()
        } catch (_: Exception) { }
        outputStream = null
        socket = null
    }
}
