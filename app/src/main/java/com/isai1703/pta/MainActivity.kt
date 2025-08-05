package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.util.*

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var statusText: TextView
    private lateinit var btnSendCommand: Button
    private var deviceIP: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val statusUpdateInterval = 5000L // 5 segundos

    // 🟦 Bluetooth
    private var bluetoothSocket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.textEstadoESP32)
        btnSendCommand = findViewById(R.id.btnSendCommand)

        checkAndRequestPermissions()

        // Cargar IP de SharedPreferences o assets
        deviceIP = loadIPFromPreferences()
        if (deviceIP.isEmpty()) {
            deviceIP = readIpFromAssets()
        }

        if (deviceIP.isEmpty()) {
            scanNetworkForESP32()
        } else {
            Toast.makeText(this, "IP leída: $deviceIP", Toast.LENGTH_SHORT).show()
            startPeriodicStatusUpdate()
        }

        connectToESP32Bluetooth()

        btnSendCommand.setOnClickListener {
            val command = "ACTIVAR"
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                sendCommandBluetooth(command)
            } else {
                sendCommandToESP32(command)
            }
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

    private fun saveIPToPreferences(ip: String) {
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        prefs.edit().putString("deviceIP", ip).apply()
    }

    private fun loadIPFromPreferences(): String {
        val prefs = getSharedPreferences("appPrefs", MODE_PRIVATE)
        return prefs.getString("deviceIP", "") ?: ""
    }

    private fun scanNetworkForESP32() {
        Thread {
            val subnet = getSubnet()
            if (subnet.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "No se pudo obtener la subred", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            var foundIP = ""
            for (i in 1..254) {
                val testIP = "$subnet.$i"
                try {
                    val url = URL("http://$testIP/status")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    conn.requestMethod = "GET"
                    val code = conn.responseCode
                    if (code == 200) {
                        foundIP = testIP
                        conn.disconnect()
                        break
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }

            runOnUiThread {
                if (foundIP.isNotEmpty()) {
                    deviceIP = foundIP
                    saveIPToPreferences(foundIP)
                    Toast.makeText(this, "ESP32 encontrado en: $deviceIP", Toast.LENGTH_LONG).show()
                    statusText.text = "Estado: Buscando..."
                    startPeriodicStatusUpdate()
                } else {
                    Toast.makeText(this, "No se encontró ESP32 en la red", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun getSubnet(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            val ip = String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
            ip.substringBeforeLast(".")
        } catch (e: Exception) {
            ""
        }
    }

    private fun startPeriodicStatusUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                    readBluetoothData()
                } else {
                    fetchStatusFromESP32()
                }
                handler.postDelayed(this, statusUpdateInterval)
            }
        })
    }

    private fun fetchStatusFromESP32() {
        Thread {
            try {
                if (deviceIP.isEmpty()) {
                    runOnUiThread { statusText.text = "IP no configurada" }
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
                        Toast.makeText(this, "IP no configurada", Toast.LENGTH_SHORT).show()
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

    // ✅ NUEVO: Conexión Bluetooth
    private fun connectToESP32Bluetooth() {
        if (bluetoothAdapter == null) {
            runOnUiThread { statusText.text = "Bluetooth no disponible" }
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            runOnUiThread { statusText.text = "Bluetooth está desactivado" }
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        if (pairedDevices != null) {
            for (device in pairedDevices) {
                if (device.name.contains("ESP32", ignoreCase = true)) {
                    Thread {
                        try {
                            val uuid = device.uuids?.get(0)?.uuid ?: return@Thread
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                            bluetoothSocket?.connect()
                            runOnUiThread {
                                statusText.text = "Conectado por Bluetooth a ${device.name}"
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread { statusText.text = "Error al conectar por Bluetooth" }
                        }
                    }.start()
                    return
                }
            }
            runOnUiThread { statusText.text = "ESP32 no emparejado aún" }
        }
    }

    // ✅ NUEVO: Enviar comando por Bluetooth
    private fun sendCommandBluetooth(command: String) {
        try {
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                val outputStream = bluetoothSocket!!.outputStream
                outputStream.write(command.toByteArray())
                outputStream.flush()
                runOnUiThread {
                    Toast.makeText(this, "Comando enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Bluetooth no está conectado", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error al enviar comando por Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ NUEVO: Leer datos por Bluetooth
    private fun readBluetoothData() {
        try {
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                val inputStream = bluetoothSocket!!.inputStream
                if (inputStream.available() > 0) {
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)
                    val message = String(buffer, 0, bytesRead)
                    runOnUiThread {
                        statusText.text = "Estado: $message"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
