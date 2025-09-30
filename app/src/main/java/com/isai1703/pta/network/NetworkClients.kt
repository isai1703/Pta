package com.isai1703.pta.network

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
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
 * Interfaz unificada para clientes de dispositivo.
 * Implementaciones: HTTP, SSH, TCP socket, Bluetooth RFCOMM.
 */
interface DeviceClient {
    /**
     * Establece conexión si aplica (por ejemplo SSH, Bluetooth o TCP persistente).
     * Devuelve true si la conexión está lista.
     */
    suspend fun connect(): Boolean

    /**
     * Cierra recursos / desconecta.
     */
    fun disconnect()

    /**
     * Envía un comando y recibe respuesta textual (o mensaje de error).
     */
    suspend fun sendCommand(command: String): String
}

/* ------------------------------ HTTP client (stateless) ------------------------------ */
class Esp32HttpClient(
    private val host: String,
    private val port: Int = 80,
    private val timeoutMs: Int = 5000
) : DeviceClient {

    override suspend fun connect(): Boolean {
        // Stateless HTTP: siempre "conectado"
        return true
    }

    override fun disconnect() {
        // Nothing to do
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$host:$port/$command")
            Log.d("Esp32HttpClient", "GET $url")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            return@withContext conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("Esp32HttpClient", "Error HTTP: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

/* ------------------------------ SSH client (Raspberry / generic SSH) ------------------------------ */
/**
 * Requiere añadir dependencia en Gradle: implementation("com.jcraft:jsch:0.1.55")
 */
class RaspberryPiSshClient(
    private val host: String,
    private val user: String,
    private val password: String,
    private val port: Int = 22,
    private val connectTimeoutMs: Int = 5000
) : DeviceClient {
    private var session: com.jcraft.jsch.Session? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (session?.isConnected == true) return@withContext true
            val jsch = JSch()
            val s = jsch.getSession(user, host, port)
            s.setPassword(password)
            s.setConfig("StrictHostKeyChecking", "no")
            s.timeout = connectTimeoutMs
            s.connect(connectTimeoutMs)
            session = s
            return@withContext s.isConnected
        } catch (e: Exception) {
            Log.e("RaspberryPiSshClient", "SSH connect error: ${e.message}")
            session = null
            return@withContext false
        }
    }

    override fun disconnect() {
        try {
            session?.disconnect()
        } catch (_: Exception) {
        } finally {
            session = null
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = session ?: run {
                // try one-shot connect
                val ok = connect()
                if (!ok) return@withContext "Error: no SSH session"
                session ?: return@withContext "Error: session null"
            }

            val channel = s.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.inputStream = null
            channel.errStream = System.err
            channel.connect()

            val reader = BufferedReader(InputStreamReader(channel.inputStream))
            val out = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                out.append(line).append("\n")
            }

            channel.disconnect()
            return@withContext out.toString().trim()
        } catch (e: Exception) {
            Log.e("RaspberryPiSshClient", "SSH exec error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

/* ------------------------------ TCP socket client (persistent) ------------------------------ */
class TcpSocketClient(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 5000
) : DeviceClient {
    private var socket: Socket? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected == true) return@withContext true
            val s = Socket()
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket = s
            return@withContext true
        } catch (e: Exception) {
            Log.e("TcpSocketClient", "TCP connect error: ${e.message}")
            socket = null
            return@withContext false
        }
    }

    override fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {
        } finally {
            socket = null
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = socket ?: run {
                val ok = connect()
                if (!ok) return@withContext "Error: cannot connect TCP"
                socket ?: return@withContext "Error: socket null"
            }
            val out: OutputStream = s.getOutputStream()
            out.write((command + "\n").toByteArray())
            out.flush()

            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val response = reader.readLine()
            return@withContext response ?: ""
        } catch (e: Exception) {
            Log.e("TcpSocketClient", "TCP send error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

/* ------------------------------ Bluetooth RFCOMM client (SPP) ------------------------------ */
class BluetoothClient(
    private val remoteDevice: BluetoothDevice,
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"),
    private val connectTimeoutMs: Int = 8000
) : DeviceClient {
    private var socket: BluetoothSocket? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (socket?.isConnected == true) return@withContext true
            val s = remoteDevice.createRfcommSocketToServiceRecord(uuid)
            // Cancel discovery on adapter should be handled by caller.
            s.connect() // blocking
            socket = s
            return@withContext s.isConnected
        } catch (e: Exception) {
            Log.e("BluetoothClient", "BT connect error: ${e.message}")
            try {
                socket?.close()
            } catch (_: Exception) {}
            socket = null
            return@withContext false
        }
    }

    override fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {
        } finally {
            socket = null
        }
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val s = socket ?: run {
                val ok = connect()
                if (!ok) return@withContext "Error: BT connect failed"
                socket ?: return@withContext "Error: bt socket null"
            }
            val out = s.outputStream
            out.write(command.toByteArray())
            out.flush()
            val reader = BufferedReader(InputStreamReader(s.inputStream))
            val response = StringBuilder()
            var line: String?
            // attempt to read available lines (with timeout risk). We'll read one line to avoid blocking forever.
            line = reader.readLine()
            if (line != null) response.append(line)
            return@withContext response.toString()
        } catch (e: Exception) {
            Log.e("BluetoothClient", "BT send error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

