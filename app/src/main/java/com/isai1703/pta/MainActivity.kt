package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var txtIp: TextView
    private lateinit var btnConectar: Button
    private lateinit var imgEstado: ImageView

    private var ipESP32: String? = null
    private var conectadoWiFi = false
    private var conectadoBluetooth = false
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            if (granted) {
                iniciarBluetooth()
            } else {
                Toast.makeText(this, "Permiso Bluetooth denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtIp = findViewById(R.id.txtIp)
        btnConectar = findViewById(R.id.btnConectar)
        imgEstado = findViewById(R.id.imgEstado)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        leerConfig()

        btnConectar.setOnClickListener {
            if (ipESP32.isNullOrEmpty()) {
                Toast.makeText(this, "No se encontró IP en config.txt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            conectarWiFi(ipESP32!!)
        }

        // Inicia permisos Bluetooth si es necesario
        if (bluetoothAdapter != null &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestBluetoothPermission.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        }
    }

    private fun leerConfig() {
        try {
            val file = File(filesDir, "config.txt")
            if (file.exists()) {
                ipESP32 = file.readText().trim()
                txtIp.text = "IP: $ipESP32"
            } else {
                // Si no existe en almacenamiento interno, buscar en assets
                assets.open("config.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        ipESP32 = reader.readLine()?.trim()
                        txtIp.text = "IP: $ipESP32"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error leyendo config.txt", e)
            Toast.makeText(this, "Error leyendo config.txt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun conectarWiFi(ip: String) {
        Thread {
            try {
                runOnUiThread { Toast.makeText(this, "Conectando a $ip...", Toast.LENGTH_SHORT).show() }
                val url = URL("http://$ip")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "GET"
                conn.connect()
                val responseCode = conn.responseCode
                conn.disconnect()

                if (responseCode == 200) {
                    conectadoWiFi = true
                    runOnUiThread { actualizarEstadoConexion() }
                } else {
                    conectadoWiFi = false
                    runOnUiThread { actualizarEstadoConexion() }
                }
            } catch (e: Exception) {
                conectadoWiFi = false
                runOnUiThread { actualizarEstadoConexion() }
            }
        }.start()
    }

    private fun iniciarBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth desactivado", Toast.LENGTH_SHORT).show()
            return
        }
        // Aquí va la lógica para buscar y conectar al ESP32 por Bluetooth
        // conectadoBluetooth = true si la conexión es exitosa
    }

    private fun actualizarEstadoConexion() {
        when {
            conectadoWiFi -> {
                imgEstado.setImageResource(R.drawable.ic_online)
                Toast.makeText(this, "Conectado por WiFi", Toast.LENGTH_SHORT).show()
            }
            conectadoBluetooth -> {
                imgEstado.setImageResource(R.drawable.ic_online)
                Toast.makeText(this, "Conectado por Bluetooth", Toast.LENGTH_SHORT).show()
            }
            else -> {
                imgEstado.setImageResource(R.drawable.ic_offline)
                Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
