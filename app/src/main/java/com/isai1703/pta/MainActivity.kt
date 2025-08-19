package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.model.Producto
import com.isai1703.pta.model.ProductoAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnScanDevices: Button

    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<DeviceInfo>()
    private var dispositivoSeleccionado: DeviceInfo? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val REQUEST_CODE_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)

        // Verificar permisos
        checkAndRequestPermissions()

        // Lista productos
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(listaProductos) { producto ->
            sendCommand(producto)
        }

        // Productos de ejemplo
        listaProductos.add(Producto("Producto 1", "$10", R.drawable.icon_prueba))
        listaProductos.add(Producto("Producto 2", "$20", R.drawable.icon_prueba))
        recyclerView.adapter?.notifyDataSetChanged()

        // Lista dispositivos
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = DeviceAdapter(dispositivosDetectados) { selected ->
            dispositivoSeleccionado = selected
            Toast.makeText(this, "Dispositivo seleccionado: ${selected.ip}", Toast.LENGTH_SHORT).show()
        }

        btnScanDevices.setOnClickListener { scanDevices() }
    }

    // -------- Permisos dinámicos --------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permisos específicos
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            // Android <12 sigue necesitando ubicación para escaneo BLE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Algunos permisos fueron denegados, ciertas funciones podrían no funcionar", Toast.LENGTH_LONG).show()
            }
        }
    }

    // -------- Enviar comandos --------
    private fun sendCommand(producto: Producto) {
        val dispositivo = dispositivoSeleccionado
        if (dispositivo != null) {
            when {
                dispositivo.type.contains("ESP32", true) ||
                        dispositivo.type.contains("Mini-PC", true) -> {
                    sendCommandWifi(dispositivo.ip, producto.nombre)
                }
                dispositivo.type.contains("STM32", true) ||
                        dispositivo.type.contains("Raspberry", true) -> {
                    sendCommandBluetooth(producto.nombre)
                }
                else -> Toast.makeText(this, "Dispositivo no soportado", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay dispositivo seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommandWifi(ip: String, comando: String) {
        Thread {
            try {
                val url = java.net.URL("http://$ip/command?cmd=$comando")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode

                runOnUiThread {
                    Toast.makeText(this, "Comando enviado por WiFi: $responseCode", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error WiFi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendCommandBluetooth(comando: String) {
        bluetoothAdapter?.bondedDevices?.firstOrNull()?.let { device: BluetoothDevice ->
            Thread {
                try {
                    val socket = device.createRfcommSocketToServiceRecord(device.uuids.first().uuid)
                    socket.connect()
                    val outStream = socket.outputStream
                    outStream.write(comando.toByteArray())
                    outStream.flush()
                    socket.close()

                    runOnUiThread {
                        Toast.makeText(this, "Comando enviado por Bluetooth", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error Bluetooth: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } ?: run {
            Toast.makeText(this, "No hay dispositivos Bluetooth emparejados", Toast.LENGTH_SHORT).show()
        }
    }

    // -------- Escaneo --------
    private fun scanDevices() {
        progressBar.progress = 0
        dispositivosDetectados.clear()
        dispositivoSeleccionado = null
        tvProgress.text = "Escaneando..."

        CoroutineScope(Dispatchers.Main).launch {
            val devices = NetworkUtils.scanNetwork(this@MainActivity)
            dispositivosDetectados.addAll(devices)
            recyclerViewDevices.adapter?.notifyDataSetChanged()

            progressBar.max = devices.size
            progressBar.progress = devices.size
            tvProgress.text = "Dispositivos detectados: ${devices.size}"
        }
    }
}
