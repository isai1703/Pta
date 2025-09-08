package com.isai1703.pta

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.manager.DeviceManager
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.model.DeviceType
import com.isai1703.pta.utils.*
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnScanDevices: Button
    private lateinit var btnAddProduct: Button
    private lateinit var btnSendAll: Button

    // Data
    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<DeviceInfo>()
    private var dispositivoSeleccionado: DeviceInfo? = null

    // Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    // Device Manager
    private val deviceManager = DeviceManager()

    // Permissions
    private val REQUEST_CODE_PERMISSIONS = 1001

    // Image picker
    private var currentDialogImageView: ImageView? = null
    private var pendingImageUri: Uri? = null
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                pendingImageUri = it
                currentDialogImageView?.setImageURI(it)
            }
        }

    // Bluetooth discovery receiver
    private val bluetoothReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                device?.let {
                    val hasBtConnect =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                    val majorClass = if (hasBtConnect) it.bluetoothClass?.majorDeviceClass else null
                    val typeName = when (majorClass) {
                        BluetoothClass.Device.Major.COMPUTER -> "Mini-PC"
                        BluetoothClass.Device.Major.PERIPHERAL -> "ESP32/STM32"
                        BluetoothClass.Device.Major.PHONE -> "Raspberry"
                        else -> "Desconocido"
                    }

                    val dName = if (hasBtConnect) it.name else null
                    val info = DeviceInfo(
                        ip = it.address,
                        name = dName ?: "Desconocido",
                        type = DeviceType.BLUETOOTH
                    )
                    if (!dispositivosDetectados.any { d -> d.ip == info.ip }) {
                        dispositivosDetectados.add(info)
                        recyclerViewDevices.adapter?.notifyDataSetChanged()
                        if (dispositivoSeleccionado == null) dispositivoSeleccionado = info
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        btnSendAll = findViewById(R.id.btnSendAll)

        checkAndRequestPermissions()

        // Load products
        listaProductos.clear()
        listaProductos.addAll(ProductStorage.loadProducts(this))
        if (listaProductos.isEmpty()) {
            listaProductos += listOf(
                Producto(1, "Producto 1", "$10", null, "dispense?product=1"),
                Producto(2, "Producto 2", "$20", null, "dispense?product=2")
            )
            ProductStorage.saveProducts(this, listaProductos)
        }

        // Recycler Productos
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(
            listaProductos,
            onSendCommandClick = { producto -> sendCommandFromProducto(producto) },
            onEditClick = { producto -> openAddEditDialog(producto) }
        )

        // Recycler Dispositivos
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = DeviceAdapter(dispositivosDetectados) { selected ->
            dispositivoSeleccionado = selected
            Toast.makeText(this, "Seleccionado: ${selected.ip}", Toast.LENGTH_SHORT).show()
        }

        // Listeners
        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
        btnSendAll.setOnClickListener { sendToAllDevices() }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {
        }
        deviceManager.disconnect()
    }

    // -------- PERMISOS
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            )
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // -------- ESCANEO DE DISPOSITIVOS
    private fun scanDevices() {
        dispositivosDetectados.clear()
        recyclerViewDevices.adapter?.notifyDataSetChanged()
        progressBar.progress = 0
        progressBar.max = 100
        tvProgress.text = "Inicializando escaneo..."

        startBluetoothScan()

        CoroutineScope(Dispatchers.Main).launch {
            tvProgress.text = "Escaneando subredes..."
            progressBar.progress = 0
            val found = withContext(Dispatchers.IO) {
                NetworkScanner.scanForMachineWithProgress(chunkSize = 32) { scanned, total, foundDevice ->
                    runOnUiThread {
                        val percent = if (total > 0) (scanned * 100 / total) else 0
                        progressBar.progress = percent
                        tvProgress.text = "Escaneadas $scanned / $total IPs"
                        foundDevice?.let {
                            if (!dispositivosDetectados.any { d -> d.ip == it.ip }) {
                                dispositivosDetectados.add(it.asDeviceInfo())
                                recyclerViewDevices.adapter?.notifyDataSetChanged()
                                dispositivoSeleccionado = dispositivosDetectados.firstOrNull()
                            }
                        }
                    }
                }
            }
            if (found != null) {
                runOnUiThread {
                    tvProgress.text = "Máquina encontrada: ${found.ip}"
                    progressBar.progress = 100
                }
                saveDetectedIp(found.ip)
            } else {
                runOnUiThread {
                    tvProgress.text =
                        "Escaneo finalizado. ${dispositivosDetectados.size} dispositivos detectados."
                    progressBar.progress = 100
                }
            }
        }
    }

    private fun startBluetoothScan() {
        val adapter = bluetoothAdapter ?: run {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!ok) {
            checkAndRequestPermissions()
            return
        }

        if (adapter.isDiscovering) try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {
        }

        try {
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } catch (_: Exception) {
        }

        try {
            adapter.startDiscovery()
        } catch (_: Exception) {
        }
    }

    // -------- CONFIG FILE
    private fun saveDetectedIp(ip: String) {
        try {
            val file = File(filesDir, "config.txt")
            file.writeText(ip.trim())
            Toast.makeText(this, "IP guardada: $ip", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }

    // -------- COMANDOS
    private fun sendCommandFromProducto(producto: Producto) {
        val dispositivo = dispositivoSeleccionado ?: run {
            Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                deviceManager.sendCommandToDevice(
                    dispositivo,
                    producto.comando.ifEmpty { producto.nombre }
                )
            }
            Toast.makeText(this, "Respuesta: $result", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToAllDevices() {
        if (listaProductos.isEmpty() || dispositivosDetectados.isEmpty()) {
            Toast.makeText(this, "No hay productos o dispositivos", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            tvProgress.text = "Enviando a todos..."
            val results = withContext(Dispatchers.IO) {
                val res = mutableListOf<Pair<String, String>>()
                for (dev in dispositivosDetectados) {
                    for (prod in listaProductos) {
                        val resp = deviceManager.sendCommandToDevice(
                            dev,
                            prod.comando.ifEmpty { prod.nombre }
                        )
                        res.add(dev.ip ?: "?" to resp)
                    }
                }
                res
            }
            val ok = results.count { !it.second.startsWith("Error") }
            tvProgress.text = "Envíos completados: $ok success"
            Toast.makeText(this@MainActivity, "Envíos finalizados", Toast.LENGTH_SHORT).show()
        }
    }
}

