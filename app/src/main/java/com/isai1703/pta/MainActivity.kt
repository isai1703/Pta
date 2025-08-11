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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity(), ProductoAdapter.OnProductoClickListener {

    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var tvIp: TextView
    private lateinit var tvEstadoConexion: TextView
    private lateinit var btnConectar: Button
    private lateinit var btnActualizar: Button
    private lateinit var rvProductos: RecyclerView

    private var deviceIP: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val statusUpdateInterval = 5000L // 5 segundos

    // Bluetooth
    private var bluetoothSocket: BluetoothSocket? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvIp = findViewById(R.id.tvIp)
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion)
        btnConectar = findViewById(R.id.btnConectar)
        btnActualizar = findViewById(R.id.btnActualizar)
        rvProductos = findViewById(R.id.rvProductos)

        checkAndRequestPermissions()

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

        btnConectar.setOnClickListener {
            connectToESP32Bluetooth()
        }

        btnActualizar.setOnClickListener {
            fetchStatusFromESP32()
        }

        inicializarCatalogo()
        actualizarEstadoConexion(false)
        actualizarIPUI()
    }

    private fun inicializarCatalogo() {
        val productos = listOf(
            Producto("Coca Cola", "CMD_COCA", R.drawable.refresco_coca),
            Producto("Pepsi", "CMD_PEPSI", R.drawable.refresco_pepsi),
            Producto("Agua", "CMD_AGUA", R.drawable.agua)
        )
        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = ProductoAdapter(productos, this)
    }

    override fun onProductoClick(comando: String) {
        sendCommandToESP32(comando)
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
                    startPeriodicStatusUpdate()
                    actualizarIPUI()
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
                fetchStatusFromESP32()
                handler.postDelayed(this, statusUpdateInterval)
            }
        })
    }

    private fun fetchStatusFromESP32() {
        Thread {
            try {
                if (deviceIP.isEmpty()) {
                    runOnUiThread {
                        tvEstadoConexion.text = "IP no configurada"
                        actualizarEstadoConexion(false)
                    }
                    return@Thread
                }
                val url = URL("http://$deviceIP/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val response = conn.inputStream.bufferedReader().readText()
                runOnUiThread {
                    tvEstadoConexion.text = "Estado: $response"
                    actualizarEstadoConexion(true)
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    tvEstadoConexion.text = "Error al obtener estado"
                    actualizarEstadoConexion(false)
                }
            }
        }.start()
    }

    private fun sendCommandToESP32(command: String) {
        if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
            sendCommandBluetooth(command)
        } else {
            sendCommandHTTP(command)
        }
    }

    private fun sendCommandBluetooth(command: String) {
        Thread {
            try {
                val outputStream: OutputStream? = bluetoothSocket?.outputStream
                outputStream?.write(command.toByteArray())
                outputStream?.flush()
                runOnUiThread {
                    Toast.makeText(this, "Comando enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar por Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendCommandHTTP(command: String) {
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

    private fun connectToESP32Bluetooth() {
        if (bluetoothAdapter == null) {
            runOnUiThread {
                tvEstadoConexion.text = "Bluetooth no disponible"
            }
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            runOnUiThread {
                Toast.makeText(this, "Bluetooth apagado", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val device: BluetoothDevice? = bluetoothAdapter
            .bondedDevices
            .firstOrNull { it.name.contains("ESP32", ignoreCase = true) }

        if (device != null) {
            Thread {
                try {
                    val uuid = device.uuids?.firstOrNull()?.uuid
                        ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket?.connect()
                    runOnUiThread {
                        Toast.makeText(this, "Conectado por Bluetooth", Toast.LENGTH_SHORT).show()
                        actualizarEstadoConexion(true)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error conectando por Bluetooth", Toast.LENGTH_SHORT).show()
                        actualizarEstadoConexion(false)
                    }
                }
            }.start()
        } else {
            runOnUiThread {
                Toast.makeText(this, "No se encontró dispositivo Bluetooth ESP32 emparejado", Toast.LENGTH_SHORT).show()
            }
            actualizarEstadoConexion(false)
        }
    }

    private fun actualizarEstadoConexion(conectado: Boolean) {
        if (conectado) {
            tvEstadoConexion.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            tvEstadoConexion.text = "Conectado"
        } else {
            tvEstadoConexion.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            tvEstadoConexion.text = "Desconectado"
        }
    }

    private fun actualizarIPUI() {
        tvIp.text = "IP: $deviceIP"
    }
}
