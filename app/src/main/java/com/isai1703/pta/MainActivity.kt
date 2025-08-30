package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.adapters.DeviceAdapter
import com.isai1703.pta.adapters.ProductoAdapter
import com.isai1703.pta.utils.NetDevice
import com.isai1703.pta.utils.NetworkScanner
import com.isai1703.pta.utils.ProductStorage
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    // ---------- UI ----------
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnScanDevices: Button
    private lateinit var btnAddProduct: Button
    private lateinit var btnSendAll: Button

    // ---------- Datos ----------
    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<DeviceInfo>()
    private var dispositivoSeleccionado: DeviceInfo? = null

    // ---------- Bluetooth ----------
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    // ---------- Permisos ----------
    private val REQUEST_CODE_PERMISSIONS = 1001

    // ---------- Selector de imágenes ----------
    private var pendingImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            pendingImageUri = it
            currentDialogImageView?.setImageURI(it)
        }
    }

    // ---------- BroadcastReceiver Bluetooth ----------
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let {
                    val hasBtConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                        ip = it.address, // MAC como "ip" para BT
                        type = typeName,
                        name = dName ?: "Desconocido"
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

    // ---------- Ciclo de vida ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        btnSendAll = findViewById(R.id.btnSendAll)

        checkAndRequestPermissions()

        // ---------- Cargar productos persistidos ----------
        listaProductos.clear()
        listaProductos.addAll(ProductStorage.loadProducts(this))
        if (listaProductos.isEmpty()) {
            listaProductos += listOf(
                Producto(1, "Producto 1", "$10", null, "RELEASE_1"),
                Producto(2, "Producto 2", "$20", null, "RELEASE_2")
            )
            ProductStorage.saveProducts(this, listaProductos)
        }

        // ---------- RecyclerView productos ----------
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(
            productos = listaProductos,
            onSendCommandClick = { producto -> sendCommand(producto) },
            onEditClick = { producto -> openAddEditDialog(producto) }
        )

        // ---------- RecyclerView dispositivos ----------
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = DeviceAdapter(dispositivosDetectados) { selected ->
            dispositivoSeleccionado = selected
            Toast.makeText(this, "Dispositivo seleccionado: ${selected.ip}", Toast.LENGTH_SHORT).show()
        }

        // ---------- Botones ----------
        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
        btnSendAll.setOnClickListener { sendToAllDevices() }
    }

    override fun onStop() {
        super.onStop()
        bluetoothAdapter?.takeIf { it.isDiscovering }?.apply {
            try { cancelDiscovery() } catch (_: SecurityException) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) { }
    }

    // ---------- Escaneo ----------
    private fun scanDevices() {
        dispositivosDetectados.clear()
        recyclerViewDevices.adapter?.notifyDataSetChanged()

        progressBar.isIndeterminate = true
        tvProgress.text = "Escaneando Bluetooth y red local..."

        scanBluetooth()

        CoroutineScope(Dispatchers.Main).launch {
            // Escaneo WiFi subred local
            val wifiFoundLocal = withContext(Dispatchers.IO) { NetworkScanner.scanSubnetDeep() }
            mergeWifiResults(wifiFoundLocal)

            // Intento de detección directa de la máquina en subred alternativa
            tvProgress.text = "Buscando máquina en otras subredes..."
            val detected = withContext(Dispatchers.IO) { NetworkScanner.scanForMachine() }
            detected?.let {
                mergeWifiResults(listOf(it.asNetDevice()))
                dispositivoSeleccionado = dispositivosDetectados.firstOrNull { d -> d.ip == it.ip } ?: dispositivoSeleccionado
                saveDetectedIp(it.ip)
                tvProgress.text = "Máquina detectada en ${it.ip}. Configuración actualizada."
            } ?: run {
                tvProgress.text = "Escaneo completado: ${dispositivosDetectados.size} dispositivos. Máquina no encontrada."
            }

            progressBar.isIndeterminate = false
        }
    }

    private fun scanBluetooth() {
        val adapter = bluetoothAdapter ?: run {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        if (!ok) { checkAndRequestPermissions(); return }

        if (adapter.isDiscovering) try { adapter.cancelDiscovery() } catch (_: SecurityException) {}

        try { registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND)) } catch (_: Exception) {}
        try {
            adapter.startDiscovery()
            Toast.makeText(this, "Escaneando Bluetooth...", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Permiso insuficiente para escaneo BT", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mergeWifiResults(list: List<NetDevice>) {
        var added = 0
        list.forEach { net ->
            val normType = when {
                net.type.contains("ESP32", true) -> "ESP32"
                net.type.contains("Raspberry", true) -> "Raspberry"
                net.type.contains("Mini", true) || net.type.contains("PC", true) -> "Mini-PC"
                net.type.equals("Gateway", true) -> "Mini-PC"
                else -> net.type
            }
            val info = DeviceInfo(ip = net.ip, type = normType, name = net.name)
            if (!dispositivosDetectados.any { d -> d.ip == info.ip }) {
                dispositivosDetectados.add(info)
                added++
                if (dispositivoSeleccionado == null) dispositivoSeleccionado = info
            }
        }
        if (added > 0) recyclerViewDevices.adapter?.notifyDataSetChanged()
    }

    // ---------- Permisos ----------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED })
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, "Algunos permisos fueron denegados", Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Agregar / Editar producto ----------
    private fun openAddEditDialog(producto: Producto?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_producto, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreProducto)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecioProducto)
        val etComando = dialogView.findViewById<EditText>(R.id.etComandoProducto)
        val ivProducto = dialogView.findViewById<ImageView>(R.id.ivProductoDialog)
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)

        producto?.let {
            etNombre.setText(it.nombre)
            etPrecio.setText(it.precio)
            etComando.setText(it.comando)
            it.imagenPath?.let { path -> ivProducto.setImageURI(Uri.parse(path)) }
        }

        currentDialogImageView = ivProducto
        pendingImageUri = producto?.imagenPath?.let { Uri.parse(it) }

        btnSeleccionarImagen.setOnClickListener { pickImageLauncher.launch("image/*") }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (producto == null) "Agregar Producto" else "Editar Producto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val precio = etPrecio.text.toString().trim()
                val comando = etComando.text.toString().trim()
                val imagenPath = pendingImageUri?.toString()

                if (nombre.isNotEmpty() && precio.isNotEmpty()) {
                    if (producto == null) {
                        val nuevo = Producto(Random().nextInt(100000), nombre, precio, imagenPath, comando)
                        listaProductos.add(nuevo)
                    } else {
                        producto.nombre = nombre
                        producto.precio = precio
                        producto.comando = comando
                        producto.imagenPath = imagenPath
                    }
                    ProductStorage.saveProducts(this, listaProductos)
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    // ---------- Guardar IP detectada ----------
    private fun saveDetectedIp(ip: String) {
        try {
            val file = File(filesDir, "config.txt")
            file.writeText(ip)
        } catch (_: IOException) { }
    }

    // ---------- Enviar comandos ----------
    private fun sendCommand(producto: Producto) {
        dispositivoSeleccionado?.let { device ->
            when (device.type) {
                "ESP32", "STM32", "Mini-PC", "Raspberry" -> sendCommandWifi(device.ip, producto.comando)
                else -> Toast.makeText(this, "Dispositivo no soportado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendToAllDevices() {
        listaProductos.forEach { sendCommand(it) }
    }

    private fun sendCommandWifi(ip: String, comando: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://$ip/$comando")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
            } catch (_: Exception) { }
        }
    }
}
