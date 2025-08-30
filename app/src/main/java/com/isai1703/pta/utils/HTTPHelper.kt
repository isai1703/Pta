package com.isai1703.pta.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object HTTPHelper {
    private const val TIMEOUT_MS = 1000

    suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            conn.disconnect()
            response
        } catch (e: Exception) {
            null
        }
    }

    suspend fun post(url: String, jsonBody: String): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            val os: OutputStream = conn.outputStream
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
            os.flush()
            os.close()
            val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
            conn.disconnect()
            response
        } catch (e: Exception) {
            null
        }
    }
}
