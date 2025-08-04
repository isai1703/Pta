package com.isai1703.pta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var statusText: TextView
    private lateinit var btnSendCommand: Button
    private var deviceIP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.textEstadoESP32)
        btnSendCommand = findViewById(R.id.btnSendCommand)

        checkAndRequestPermissions()

        deviceIP = readIpFromAssets()

        Toast.makeText(this, "IP leída: $deviceIP", Toast.LENGTH_SHORT).show()

        fetchStatusFromESP32()

        btnSendCommand.setOnClickListener {
            sendCommandToESP32("ACTIVAR")
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun readIpFromAssets(): String {
        return try {
            val inputStream = assets.open("config.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val ip = reader.readLine()?.trim() ?: ""
            reader.close()
            ip
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun fetchStatusFromESP32() {
        Thread {
            try {
                if (deviceIP.isEmpty()) {
                    runOnUiThread {
                        statusText.text = "IP no configurada en config.txt"
                    }
                    return@Thread
                }

                val url = URL("http://$deviceIP/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                val response = conn.inputStream.bufferedReader().readText()

                runOnUiThread {
                    statusText.text = "Estado: $response"
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error al obtener estado"
                }
            }
        }.start()
    }

    private fun sendCommandToESP32(command: String) {
        Thread {
            try {
                if (deviceIP.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this, "IP no configurada en config.txt", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val url = URL("http://$deviceIP/cmd?accion=$command")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"

                val responseCode = conn.responseCode

                runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Comando enviado con éxito", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al enviar comando", Toast.LENGTH_SHORT).show()
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Fallo de conexión al ESP32", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for ((index, result) in grantResults.withIndex()) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    val deniedPermission = permissions[index]
                    Toast.makeText(this, "Permiso necesario: $deniedPermission", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

