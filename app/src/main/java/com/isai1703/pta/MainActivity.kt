package com.isai1703.pta

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var rvDispositivos: RecyclerView
    private lateinit var rvProductos: RecyclerView
    private lateinit var btnEscanear: Button
    private lateinit var btnEnviarTodos: Button
    private lateinit var iconoEstadoGeneral: ImageView

    private val dispositivos = mutableListOf<Device>()
    private val deviceAdapter by lazy { DeviceAdapter(dispositivos) { d -> onConnectDevice(d) } }

    private val productos = mutableListOf<Producto>()
    private val productosAdapter by lazy { ProductoAdapter(productos) { p -> onSendProduct(p) } }

    private lateinit var sharedPreferences: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ui = Handler(Looper.getMainLooper())

    private val reScanMs = 15000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionsHelper.ensure(this)

        sharedPreferences = getSharedPreferences("Historial", Context.MODE_PRIVATE)

        rvDispositivos = findViewById(R.id.rvDispositivos)
        rvProductos = findViewById(R.id.rvProductos)
        btnEscanear = findViewById(R.id.btnEscanear)
        btnEnviarTodos = findViewById(R.id.btnEnviarTodos)
        iconoEstadoGeneral = findViewById(R.id.iconoEstadoGeneral)

        rvDispositivos.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvDispositivos.adapter = deviceAdapter

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = productosAdapter

        initProductosDesdeDrawables()
        autoScanLoop()

        btnEscanear.setOnClickListener { scanAll() }
        btnEnviarTodos.setOnClickListener { enviarATodos() }
    }

    private fun autoScanLoop() {
        scanAll()
        ui.postDelayed({ autoScanLoop() }, reScanMs)
    }

    private fun scanAll() {
        scope.launch {
            val list = mutableListOf<Device>()
            // --- Escaneo WiFi ---
            val ipLocal = NetworkUtils.localIPv4(this@MainActivity) ?: "192.168.1.100"
            val candidatos = NetworkUtils.subnetCandidates(ipLocal)
            val portsToCheck = listOf(80, 502, 1883, 22) // HTTP, Modbus, MQTT, SSH
            val jobs = candidatos.map { host ->
                async {
                    for (p in portsToCheck) {
                        if (NetworkUtils.isTcpOpen(host, p, 150)) {
                            val tipo = classifyByPort(host, p)
                            val nombre = "${tipo.name} @ $host:$p"
                            return@async Device(nombre = nombre, address = host, tipo = tipo, conectado = false)
                        }
                    }
                    null
                }
            }
            val found = jobs.awaitAll().filterNotNull()
            list.addAll(found.distinctBy { it.address })

            // --- Escaneo Bluetooth (emparejados) ---
            try {
                val bt = BluetoothAdapter.getDefaultAdapter()
                if (bt != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == 0)) {
                    bt.bondedDevices?.forEach { dev ->
                        val tipo = classifyBluetooth(dev)
                        list.add(Device(nombre = dev.name ?: "BT", address = dev.address, tipo = tipo, conectado = false))
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                deviceAdapter.replaceAll(list)
                // Conexión automática al primero compatible
                list.firstOrNull { it.tipo != TipoDispositivo.DESCONOCIDO }?.let { auto ->
                    onConnectDevice(auto)
                }
            }
        }
    }

    private fun classifyByPort(host: String, port: Int): TipoDispositivo {
        return when (port) {
            80 -> {
                // Intento de leer cabecera rápida para distinguir ESP32 vs web genérica
                try {
                    val url = URL("http://$host/")
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 250
                        readTimeout = 250
                        requestMethod = "GET"
                        inputStream.use { ins ->
                            val first = BufferedReader(InputStreamReader(ins)).readLine() ?: ""
                            return if (first.contains("ESP", true) || first.contains("Arduino", true)) TipoDispositivo.ESP32 else TipoDispositivo.MINIPC
                        }
                    }
                } catch (_: Exception) {
                    TipoDispositivo.MINIPC
                }
            }
            22 -> TipoDispositivo.RASPBERRY   // SSH muy común en Pi
            1883 -> TipoDispositivo.MINIPC    // Broker MQTT suele correr en SBC/PC
            502 -> TipoDispositivo.STM32      // Modbus/TCP (típico PLC/MCU)
            else -> TipoDispositivo.DESCONOCIDO
        }
    }

    private fun classifyBluetooth(dev: BluetoothDevice): TipoDispositivo {
        val n = (dev.name ?: "").lowercase()
        return when {
            "esp32" in n || "esp" in n   -> TipoDispositivo.ESP32
            "rasp" in n || "pi" in n     -> TipoDispositivo.RASPBERRY
            "stm" in n                   -> TipoDispositivo.STM32
            else                         -> TipoDispositivo.DESCONOCIDO
        }
    }

    private fun onConnectDevice(d: Device) {
        // Selección de protocolo simple por tipo
        scope.launch {
            val ok = when (d.tipo) {
                TipoDispositivo.ESP32, TipoDispositivo.MINIPC, TipoDispositivo.RASPBERRY -> testHttp(d.address)
                TipoDispositivo.STM32 -> testTcp(d.address, 502)
                else -> false
            }
            withContext(Dispatchers.Main) {
                deviceAdapter.markConnected(d.address, ok)
                iconoEstadoGeneral.setImageResource(if (ok) R.drawable.ic_connected else R.drawable.ic_disconnected)
                Toast.makeText(this@MainActivity, if (ok) "Conectado a ${d.nombre}" else "No se pudo conectar a ${d.nombre}", Toast.LENGTH_SHORT).show()
                if (ok) savePreferredAddress(d.address)
            }
        }
    }

    private fun onSendProduct(p: Producto) {
        // Enviar comando al dispositivo preferido si existe; si no, a todos los conectados HTTP
        val dest = getPreferredAddress()
        if (dest != null) {
            sendCommandWifi("COMANDO_${p.id}", dest)
        } else {
            dispositivos.filter { it.conectado }.forEach { sendCommandWifi("COMANDO_${p.id}", it.address) }
        }
        saveHistorial(p)
        Toast.makeText(this, "Comando enviado: ${p.nombre}", Toast.LENGTH_SHORT).show()
    }

    private fun enviarATodos() {
        productos.forEach { onSendProduct(it) }
    }

    // --- Utilidades de conexión ---
    private fun testHttp(host: String): Boolean {
        return try {
            val url = URL("http://$host/")
            (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 500
                readTimeout = 500
                requestMethod = "GET"
                inputStream.use { /* ok */ }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun testTcp(host: String, port: Int): Boolean {
        return NetworkUtils.isTcpOpen(host, port, 300)
    }

    private fun sendCommandWifi(command: String, host: String) {
        // Ejemplo genérico: GET /cmd?c=...
        try {
            val url = URL("http://$host/cmd?c=$command")
            (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 800
                readTimeout = 1000
                requestMethod = "GET"
                inputStream.close()
            }
        } catch (_: Exception) { }
    }

    // --- Historial y preferencia de destino ---
    private fun saveHistorial(p: Producto) {
        val sp = getSharedPreferences("Historial", Context.MODE_PRIVATE)
        val set = sp.getStringSet("historial", mutableSetOf()) ?: mutableSetOf()
        set.add("${p.nombre} - ${System.currentTimeMillis()}")
        sp.edit().putStringSet("historial", set).apply()
    }

    private fun savePreferredAddress(address: String) {
        val sp = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        sp.edit().putString("preferred_host", address).apply()
    }

    private fun getPreferredAddress(): String? {
        val sp = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        return sp.getString("preferred_host", null)
    }

    // --- Carga de productos usando drawables existentes (si están) ---
    private fun initProductosDesdeDrawables() {
        productos.clear()
        val ctx = this
        fun id(name: String): Int = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        val candidatos = listOf(
            "agua","cocacola","pepsi","galletas","galletasprincipe","galletasbarritasfre",
            "chocolate","chocolatekinder","chocolatekitkat","chocolatemms",
            "palomitas","papas","papaas","papass","papaaas",
            "jugo","jugoo","yogurt","arizona","penafiel","penafieel","penafiiel",
            "sopamaruhaba","volt","voltb","dulceskwinkles"
        )
        var idx = 1
        candidatos.forEach { n ->
            val res = id(n)
            if (res != 0) {
                productos.add(Producto(idx++, n.replaceFirstChar { it.uppercase() }, res, disponible = true))
            }
        }
        if (productos.isEmpty()) {
            // Fallback si no hay imágenes: al menos 3
            val fallback = listOf(
                Producto(1, "Producto A", R.drawable.ic_producto, true),
                Producto(2, "Producto B", R.drawable.ic_producto, true),
                Producto(3, "Producto C", R.drawable.ic_producto, false)
            )
            productos.addAll(fallback)
        }
        productosAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
