package com.isai1703.pta

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

data class DetectedDevice(val name: String, val ip: String, val type: DeviceType)
enum class DeviceType { ESP32, RASPBERRY_PI, STM32, MINI_PC, DESCONOCIDO }

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "pta_prefs"
        private const val PREF_IP = "esp32_ip"
        private const val PREF_COMMAND_HISTORY = "command_history"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var statusTextView: TextView
    private lateinit var connectionIcon: ImageView
    private lateinit var tvIp: TextView
    private lateinit var btnConectar: Button
    private lateinit var btnEditarIp: Button
    private lateinit var btnScanIp: Button
    private lateinit var btnEnviarATodos: Button
    private lateinit var btnScanDevices: Button
    private lateinit var btnEnviarATodosDetectados: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var switchSimulacion: Switch

    private lateinit var prefs: SharedPreferences
    private var esp32Ip: String? = null
    private var connectedWifi = false
    private var connectedBluetooth = false
    private var btDevice: BluetoothDevice? = null
    private var modoSimulacion = true

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _permissions -> }

    private val commandHistory = mutableListOf<String>()
    private val detectedDevices = mutableListOf<DetectedDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Views ---
        statusTextView = findViewById(R.id.statusTextView)
        connectionIcon = findViewById(R.id.connectionIcon)
        tvIp = findViewById(R.id.tvIp)
        btnConectar = findViewById(R.id.btnConectar)
        btnEditarIp = findViewById(R.id.btnEditarIp)
        btnScanIp = findViewById(R.id.btnScanIp)
        btnEnviarATodos = findViewById(R.id.btnEnviarATodos)
        recyclerView = findViewById(R.id.recyclerView)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnEnviarATodosDetectados = findViewById(R.id.btnEnviarATodosDetectados)
        switchSimulacion = findViewById(R.id.switchSimulacion)

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        esp32Ip = prefs.getString(PREF_IP, null)
        tvIp.text = "IP: ${esp32Ip ?: "---"}"

        val historyStr = prefs.getString(PREF_COMMAND_HISTORY, "")
        if (!historyStr.isNullOrEmpty()) {
            commandHistory.addAll(historyStr.split(";"))
        }

        switchSimulacion.isChecked = modoSimulacion
        switchSimulacion.setOnCheckedChangeListener { _, isChecked ->
            modoSimulacion = isChecked
            val msg = if (modoSimulacion) "Modo Simulación Activado" else "Modo Real Activado"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            if (modoSimulacion) activarSimulacion()
        }

        val productos = listOf(
            Producto("Coca Cola", R.drawable.cocacola, "CMD_COCA", "$15"),
            Producto("Pepsi", R.drawable.pepsi, "CMD_PEPSI", "$15"),
            Producto("Agua", R.drawable.agua, "CMD_AGUA", "$10"),
            Producto("Paleta", R.drawable.paleta, "CMD_PALETA", "$5"),
            Producto("Arizona", R.drawable.arizona, "CMD_ARIZONA", "$20"),
            Producto("Palomitas", R.drawable.palomitas, "CMD_PALOMITAS", "$25"),
            Producto("Chocolate", R.drawable.chocolate, "CMD_CHOCOLATE", "$12"),
            Producto("Papaaas", R.drawable.papaaas, "CMD_PAPAAAS", "$18"),
            Producto("Chocolate Kinder", R.drawable.chocolatekinder, "CMD_CHOCKINDER", "$15"),
            Producto("Papaas", R.drawable.papaas, "CMD_PAPAAS", "$18"),
            Producto("Chocolate KitKat", R.drawable.chocolatekitkat, "CMD_KITKAT", "$15"),
            Producto("Papas", R.drawable.papas, "CMD_PAPAS", "$12"),
            Producto("Chocolate M&M's", R.drawable.chocolatemms, "CMD_MMS", "$15"),
            Producto("Papass", R.drawable.papass, "CMD_PAPASS", "$18"),
            Producto("Penafiel", R.drawable.penafiel, "CMD_PENA", "$10"),
            Producto("Galletas", R.drawable.galletas, "CMD_GALLETA", "$8"),
            Producto("Galletas Barritas Fre", R.drawable.galletasbarritasfre, "CMD_BARFRE", "$10"),
            Producto("Galletas Principe", R.drawable.galletasprincipe, "CMD_PRINCIPE", "$12"),
            Producto("Volt", R.drawable.volt, "CMD_VOLT", "$22"),
            Producto("Volt B", R.drawable.voltb, "CMD_VOLTB", "$22"),
            Producto("Jugo", R.drawable.jugo, "CMD_JUGO", "$15"),
            Producto("Yogurt", R.drawable.yogurt, "CMD_YOGURT", "$18")
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(productos) { comando ->
            detectedDevices.forEach { sendCommandToDevice(it, comando) }
        }

        btnConectar.setOnClickListener { connectNow() }
        btnEditarIp.setOnClickListener { showEditIpDialog() }
        btnScanIp.setOnClickListener { scanForEsp32InNetwork() }
        btnEnviarATodos.setOnClickListener { productos.forEach { sendCommand(it.comando) } }

        btnScanDevices.setOnClickListener { scanForDevicesInNetwork() }
        btnEnviarATodosDetectados.setOnClickListener {
            val comando = "CMD_GENERAL"
            detectedDevices.forEach { sendCommandToDevice(it, comando) }
        }

        requestBluetoothPermissionsIfNeeded()
        scheduler.scheduleWithFixedDelay({ checkConnections() }, 0, 8, TimeUnit.SECONDS)

        if (modoSimulacion) activarSimulacion()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { scheduler.shutdownNow(); executor.shutdownNow() } catch (e: Exception) {}
    }

    private fun activarSimulacion() {
        executor.execute {
            detectedDevices.clear()
            detectedDevices.add(DetectedDevice("ESP32 Simulado", "192.168.1.50", DeviceType.ESP32))
            detectedDevices.add(DetectedDevice("Raspberry Pi Simulada", "192.168.1.51", DeviceType.RASPBERRY_PI))
            detectedDevices.add(DetectedDevice("Mini-PC Simulado", "192.168.1.52", DeviceType.MINI_PC))
            connectedWifi = true
            connectedBluetooth = false
            handler.post { updateUi(); Toast.makeText(this, "Simulación activada", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            btPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 111)
        }
    }

    private fun saveIp(ip: String) { prefs.edit().putString(PREF_IP, ip).apply(); esp32Ip = ip; runOnUiThread { tvIp.text = "IP: $ip" } }

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

    private fun checkConnections() {
        if (modoSimulacion) { connectedWifi = true; connectedBluetooth = false; updateUi(); return }
        try {
            val ip = esp32Ip
            connectedWifi = if (!ip.isNullOrEmpty()) tryPingHttp(ip) else false
            connectedBluetooth = checkPairedEsp32()
            updateUi()
        } catch (e: Exception) { updateUi() }
    }

    private fun updateUi() {
        runOnUiThread {
            when {
                connectedWifi -> { connectionIcon.setImageResource(R.drawable.ic_connected); statusTextView.text = if (modoSimulacion) "Simulación WiFi" else "Conectado por WiFi"; statusTextView.setTextColor(getColor(R.color.green)) }
                connectedBluetooth -> { connectionIcon.setImageResource(R.drawable.ic_connected); statusTextView.text = if (modoSimulacion) "Simulación Bluetooth" else "Conectado por Bluetooth"; statusTextView.setTextColor(getColor(R.color.green)) }
                else -> { connectionIcon.setImageResource(R.drawable.ic_disconnected); statusTextView.text = "Desconectado"; statusTextView.setTextColor(getColor(R.color.red)) }
            }
        }
    }

    private fun sendCommand(comando: String) {
        executor.execute {
            if (modoSimulacion) { detectedDevices.forEach { addToHistory("$comando [Simulado] -> ${it.name}"); handler.post { Toast.makeText(this, "Simulación: '$comando' -> ${it.name}", Toast.LENGTH_SHORT).show() } }; return@execute }
            val ip = esp32Ip
            if (!ip.isNullOrEmpty() && connectedWifi) { val ok = sendCommandWifi(ip, comando); if (ok) addToHistory(comando); handler.post { Toast.makeText(this, if (ok) "Comando enviado (WiFi)" else "Error WiFi", Toast.LENGTH_SHORT).show() } ; return@execute }
            if (connectedBluetooth && btDevice != null) { val ok = sendCommandBluetooth(btDevice!!, comando); if (ok) addToHistory(comando); handler.post { Toast.makeText(this, if (ok) "Comando enviado (Bluetooth)" else "Error Bluetooth", Toast.LENGTH_SHORT).show() }; return@execute }
            handler.post { Toast.makeText(this, "No hay conexión", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun addToHistory(comando: String) {
        commandHistory.add("${System.currentTimeMillis()}: $comando")
        prefs.edit().putString(PREF_COMMAND_HISTORY, commandHistory.joinToString(";")).apply()
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
        } catch (e: Exception) { false }
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
        } catch (e: Exception) { false }
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
        } catch (e: Exception) { false }
    }

    @SuppressLint("MissingPermission")
    private fun checkPairedEsp32(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false
        val device = adapter.bondedDevices.find { it.name.contains("ESP32", true) }
        btDevice = device
        return device != null
    }

    private fun connectNow() { checkConnections() }

    private fun scanForEsp32InNetwork() {
        executor.execute {
            val baseIp = esp32Ip?.substringBeforeLast('.') ?: "192.168.1"
            for (i in 1..254) {
                val testIp = "$baseIp.$i"
                if (tryPingHttp(testIp)) { saveIp(testIp); handler.post { Toast.makeText(this, "ESP32 encontrado: $testIp", Toast.LENGTH_LONG).show() }; break }
            }
        }
    }

    private fun scanForDevicesInNetwork() {
        executor.execute {
            detectedDevices.clear()
            val baseIp = esp32Ip?.substringBeforeLast('.') ?: "192.168.1"
            for (i in 1..254) {
                val testIp = "$baseIp.$i"
                if (tryPingHttp(testIp)) detectedDevices.add(DetectedDevice("Dispositivo $i", testIp, if (testIp == esp32Ip) DeviceType.ESP32 else DeviceType.DESCONOCIDO))
            }
            handler.post { Toast.makeText(this, "Dispositivos detectados: ${detectedDevices.size}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun sendCommandToDevice(device: DetectedDevice, comando: String) {
        executor.execute {
            if (modoSimulacion) { handler.post { Toast.makeText(this, "Simulación: '$comando' -> ${device.name} (${device.ip})", Toast.LENGTH_SHORT).show() }; addToHistory("$comando [Simulado] -> ${device.name}"); return@execute }
            val ok = when(device.type) {
                DeviceType.ESP32 -> if(connectedWifi && esp32Ip==device.ip) sendCommandWifi(device.ip, comando) else if(connectedBluetooth && btDevice!=null) sendCommandBluetooth(btDevice!!, comando) else false
                else -> sendCommandWifi(device.ip, comando)
            }
            if(ok) addToHistory("$comando -> ${device.name}")
            handler.post { Toast.makeText(this, if(ok) "Comando enviado a ${device.name}" else "Error enviando a ${device.name}", Toast.LENGTH_SHORT).show() }
        }
    }
}
