package com.isai1703.pta.network

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

/**
 * Interfaz genérica para dispositivos (ESP32, Raspberry Pi, etc.)
 */
interface DeviceClient {
    suspend fun sendCommand(command: String): String
}

/**
 * Cliente HTTP para ESP32 u otros microcontroladores con servidor REST.
 */
class Esp32HttpClient(
    private val ip: String,
    private val port: Int = 80
) : DeviceClient {

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/$command")
            Log.d("Esp32HttpClient", "Enviando a $url")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            return@withContext response
        } catch (e: Exception) {
            Log.e("Esp32HttpClient", "Error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

/**
 * Cliente SSH para Raspberry Pi (ejecuta comandos reales en la terminal).
 * Usa la librería JSch.
 */
class RaspberryPiSshClient(
    private val host: String,
    private val user: String,
    private val password: String,
    private val port: Int = 22
) : DeviceClient {

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val jsch = JSch()
            val session = jsch.getSession(user, host, port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(5000)

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            channel.connect()

            val reader = BufferedReader(InputStreamReader(channel.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            reader.close()
            channel.disconnect()
            session.disconnect()

            return@withContext output.toString().trim()
        } catch (e: Exception) {
            Log.e("RaspberryPiSshClient", "Error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}

/**
 * Cliente TCP genérico (para otros dispositivos que acepten sockets).
 */
class TcpSocketClient(
    private val host: String,
    private val port: Int
) : DeviceClient {

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 5000)

            val output: OutputStream = socket.getOutputStream()
            output.write(command.toByteArray())
            output.flush()

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val response = reader.readLine()

            reader.close()
            socket.close()

            return@withContext response ?: "No response"
        } catch (e: Exception) {
            Log.e("TcpSocketClient", "Error: ${e.message}")
            return@withContext "Error: ${e.message}"
        }
    }
}
