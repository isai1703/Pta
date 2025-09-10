package com.isai1703.pta.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.*

/**
 * Interfaz genérica para clientes de dispositivo.
 */
interface DeviceClient {
    suspend fun connect(): Boolean
    suspend fun sendCommand(command: String): String
    fun disconnect()
}

/**
 * Cliente HTTP simple (ESP32 / microcontrolador con REST)
 */
class Esp32HttpClient(private val host: String, private val port: Int = 80) : DeviceClient {
    private val baseUrl = "http://$host:$port"

    override suspend fun connect(): Boolean = true

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/$command")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            resp
        } catch (e: Exception) {
            Log.e("Esp32HttpClient", "sendCommand error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    override fun disconnect() {}
}

/**
 * Cliente SSH para Raspberry Pi (usa JSch).
 * Mantiene la sesión abierta (connect/disconnect).
 */
class RaspberryPiSshClient(
    private val host: String,
    private val user: String,
    private val password: String,
    private val port: Int = 22
) : DeviceClient {
    private var session: Session? = null
    private val jsch = JSch()

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            session = jsch.getSession(user, host, port).apply {
                setPassword(password)
                setConfig("StrictHostKeyChecking", "no")
                connect(5000)
            }
            session?.isConnected == true
        } catch (e: Exception) {
            session = null
            false
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = session ?: run {
                if (!connect()) return@withContext "Error: no session"
                session!!
            }
            val channel = s.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.connect(3000)
            val reader = BufferedReader(InputStreamReader(channel.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            channel.disconnect()
            sb.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun disconnect() {
        try { session?.disconnect() } catch (_: Exception) {}
        session = null
    }
}

/**
 * Cliente TCP simple (para sockets).
 */
class TcpSocketClient(private val host: String, private val port: Int) : DeviceClient {
    private var socket: Socket? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = Socket().apply { connect(InetSocketAddress(host, port), 3000) }
            socket?.isConnected == true
        } catch (e: Exception) {
            socket = null
            false
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = socket ?: run {
                if (!connect()) return@withContext "Error: connect fail"
                socket!!
            }
            val out: OutputStream = s.getOutputStream()
            out.write(command.toByteArray())
            out.flush()
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val resp = reader.readLine() ?: "No response"
            resp
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}

/**
 * Cliente Bluetooth RFCOMM (SPP). Usa UUID SPP por defecto.
 */
class BluetoothClient(private val device: BluetoothDevice) : DeviceClient {
    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket = s
            socket?.connect()
            socket?.isConnected == true
        } catch (e: Exception) {
            try { // fallback reflection hack (sometimes works)
                val m = device.javaClass.getMethod("createRfcommSocket", Integer::class.javaPrimitiveType)
                val s = m.invoke(device, 1) as BluetoothSocket
                socket = s
                socket?.connect()
                socket?.isConnected == true
            } catch (ex: Exception) {
                socket = null
                false
            }
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = socket ?: run {
                if (!connect()) return@withContext "Error: BT connect"
                socket!!
            }
            val out = s.outputStream
            out.write(command.toByteArray())
            out.flush()
            val reader = BufferedReader(InputStreamReader(s.inputStream))
            val resp = reader.readLine() ?: "OK"
            resp
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }
}
