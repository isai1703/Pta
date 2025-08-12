package com.isai1703.pta

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "pta_prefs"
        private const val PREF_IP = "esp32_ip"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var statusTextView: TextView
    private lateinit var connectionIcon: ImageView
    private lateinit var tvIp: TextView
    private lateinit var btnConectar: Button
    private lateinit var btnEditarIp: Button
    private lateinit var recyclerView: RecyclerView

    private lateinit var prefs: SharedPreferences
    private var esp32Ip: String? = null
    private var connectedWifi = false
    private var connectedBluetooth = false
    private var btDevice: BluetoothDevice? = null

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // No hacemos nada complejo aquí — el usuario debe conceder para Bluetooth
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        statusTextView = findViewById(R.id.statusTextView)
        connectionIcon = findViewById(R.id.connectionIcon)
        tvIp = findViewById(R.id.tvIp)
        btnConectar = findViewById(R.id.btnConectar)
        btnEditarIp = findViewById(R.id.btnEditarIp)
        recyclerView = findViewById(R.id.recyclerView)

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        esp32Ip = prefs.getString(PREF_IP, null) ?: readIpFromAssets()
        tvIp.text = "IP: ${esp32Ip ?: "---"}"

        // Recycler + productos (ajusta imágenes/comandos reales)
        val productos = listOf(
            Producto("Coca Cola", R.drawable.coca, "CMD_COCA", "$15"),
            Producto("Pepsi", R.drawable.pepsi, "CMD_PEPSI", "$15"),
            Producto("Agua", R.drawable.agua, "CMD_AGUA", "$10")
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(productos) { comando -> sendCommand(comando) }

        btnConectar.setOnClickListener { connectNow() }
        btnEditarIp.setOnClickListener { showEditIpDialog() }

        requestBluetoothPermissionsIfNeeded()

        // Revisar conexiones periódicamente (cada 8s)
        scheduler.scheduleWithFixedDelay({ checkConnections() }, 0, 8, TimeUnit.SECONDS)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            scheduler.shutdownNow()
            executor.shutdownNow()
        } catch (e: Exception) { /* ignore */ }
    }

    // ----------------- Permisos -----------------
    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 111)
            }
        }
    }

    // ----------------- IP prefs / UI -----------------
    private fun readIpFromAssets(): String? {
        return try {
            assets.open("config.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    reader.readLine()?.trim()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveIp(ip: String) {
        prefs.edit().putString(PREF_IP, ip).apply()
        esp32Ip = ip
        runOnUiThread { tvIp.text = "IP: $ip" }
    }

    private fun showEditIpDialog() {
        val edit = EditText(this)
        edit.setText(esp32Ip ?: "")
        AlertDialog.Builder(this)
            .setTitle("Editar IP del ESP32")
            .setView(edit)
            .setPositiveButton("Guardar") { _: DialogInterface, _: Int ->
                val ip = edit.text.toString().trim()
                if (ip.isNotEmpty()) saveIp(ip)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ----------------- Connectivity checks -----------------
    private fun checkConnections() {
        try {
            // 1) Intentar WiFi (si IP definida)
            val ip = esp32Ip
            if (!ip.isNullOrEmpty()) {
                val ok = tryPingHttp(ip)
                connectedWifi = ok
                if (ok) {
                    connectedBluetooth = false
                    updateUi()
                    return
                }
            } else {
                connectedWifi = false
            }

            // 2) Intentar Bluetooth (si adaptador y emparejados)
            connectedBluetooth = checkPairedEsp32()
            updateUi()
        } catch (e: Exception) {
            updateUi()
        }
    }

    private fun updateUi() {
        runOnUiThread {
            when {
                connectedWifi -> {
                    connectionIcon.setImageResource(R.drawable.ic_connected)
                    statusTextView.text = "Conectado por WiFi"
                }
                connectedBluetooth -> {
                    connectionIcon.setImageResource(R.drawable.ic_connected)
                    statusTextView.text = "Conectado por Bluetooth"
                }
                else -> {
                    connectionIcon.setImageResource(R.drawable.ic_disconnected)
                    statusTextView.text = "Desconectado"
                }
            }
        }
    }

    private fun tryPingHttp(ip: String): Boolean {
        return try {
            val url = URL("http://$ip/status")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (e: Exception) {
            false
        }
    }

    // ----------------- Bluetooth helpers -----------------
    @SuppressLint("MissingPermission")
    private fun checkPairedEsp32(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false
        val paired = adapter.bondedDevices
        val device = paired?.find { it.name.contains("ESP32", ignoreCase = true) }
        btDevice = device
        return device != null
    }

    // Try connect now (first WiFi then Bluetooth)
    private fun connectNow() {
        executor.execute {
            try {
                runOnUiThread { statusTextView.text = "Conectando..." }
                val ip = esp32Ip
                if (!ip.isNullOrEmpty() && tryPingHttp(ip)) {
                    connectedWifi = true
                    connectedBluetooth = false
                    runOnUiThread { Toast.makeText(this, "Conectado por WiFi", Toast.LENGTH_SHORT).show() }
                    updateUi()
                    return@execute
                }
                // Bluetooth attempt (simple)
                if (checkPairedEsp32() && btDevice != null) {
                    // Note: here we don't keep persistent socket; this is a simple check
                    connectedBluetooth = true
                    connectedWifi = false
                    runOnUiThread { Toast.makeText(this, "ESP32 emparejado (Bluetooth)", Toast.LENGTH_SHORT).show() }
                    updateUi()
                    return@execute
                }
                connectedWifi = false
                connectedBluetooth = false
                updateUi()
            } catch (e: Exception) {
                connectedWifi = false
                connectedBluetooth = false
                updateUi()
            }
        }
    }

    // ----------------- Send command (WiFi preferred) -----------------
    private fun sendCommand(comando: String) {
        executor.execute {
            try {
                val ip = esp32Ip
                if (!ip.isNullOrEmpty() && connectedWifi) {
                    val ok = sendCommandWifi(ip, comando)
                    runOnUiThread {
                        Toast.makeText(this, if (ok) "Comando enviado (WiFi)" else "Error WiFi", Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }
                if (connectedBluetooth && btDevice != null) {
                    val ok = sendCommandBluetooth(btDevice!!, comando)
                    runOnUiThread {
                        Toast.makeText(this, if (ok) "Comando enviado (Bluetooth)" else "Error Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }
                runOnUiThread { Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Error enviando comando", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun sendCommandWifi(ip: String, comando: String): Boolean {
        return try {
            val url = URL("http://$ip/cmd?data=${comando}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..399
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendCommandBluetooth(device: BluetoothDevice, comando: String): Boolean {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val out: OutputStream? = socket.outputStream
            out?.write((comando + "\n").toByteArray())
            out?.flush()
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
