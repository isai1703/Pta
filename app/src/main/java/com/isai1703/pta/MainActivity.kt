package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.IntentFilter
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
import com.isai1703.pta.model.DeviceInfo
import com.isai1703.pta.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

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

    // Permissions
    private val REQUEST_CODE_PERMISSIONS = 1001

    // Image picker for dialog
    private var currentDialogImageView: ImageView? = null
    private var pendingImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { pendingImageUri = it; currentDialogImageView?.setImageURI(it) }
    }

    // Bluetooth discovery receiver
    private val bluetoothReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let {
                    val hasBtConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    } else true

                    val majorClass = if (hasBtConnect) it.bluetoothClass?.majorDeviceClass else null
                    val typeName = when (majorClass) {
                        BluetoothClass.Device.Major.COMPUTER -> "Mini-PC"
                        BluetoothClass.Device.Major.PERIPHERAL -> "ESP32/STM32"
                        BluetoothClass.Device.Major.PHONE -> "Raspberry"
                        else -> "Desconocido"
                    }
                    val dName = if (hasBtConnect) it.name else null
                    val info = DeviceInfo(ip = it.address, type = typeName, name = dName ?: "Desconocido")
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
            // Seed minimal example; user will add real products (52) from app
            listaProductos += listOf(
                Producto(1, "Producto 1", "$10", null, "dispense?product=1"),
                Producto(2, "Producto 2", "$20", null, "dispense?product=2")
            )
            ProductStorage.saveProducts(this, listaProductos)
        }

        // Productos RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(listaProductos,
            onSendCommandClick = { producto -> sendCommandFromProducto(producto) },
            onEditClick = { producto -> openAddEditDialog(producto) }
        )

        // Dispositivos RecyclerView
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = DeviceAdapter(dispositivosDetectados) { selected ->
            dispositivoSeleccionado = selected
            Toast.makeText(this, "Seleccionado: ${selected.ip}", Toast.LENGTH_SHORT).show()
        }

        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
        btnSendAll.setOnClickListener { sendToAllDevices() }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
    }

    // Permissions
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

    // Scan devices (WiFi + Bluetooth) with progress UI
    private fun scanDevices() {
        dispositivosDetectados.clear()
        recyclerViewDevices.adapter?.notifyDataSetChanged()

        progressBar.progress = 0
        progressBar.max = 100
        tvProgress.text = "Inicializando escaneo..."

        // Start BT scan
        startBluetoothScan()

        // WiFi scan with progress and detection
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
                runOnUiThread { tvProgress.text = "Máquina encontrada: ${found.ip}"; progressBar.progress = 100 }
                saveDetectedIp(found.ip)
            } else {
                runOnUiThread { tvProgress.text = "Escaneo finalizado. ${dispositivosDetectados.size} dispositivos detectados."; progressBar.progress = 100 }
            }
        }
    }

    private fun startBluetoothScan() {
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

        if (adapter.isDiscovering) try { adapter.cancelDiscovery() } catch (_: Exception) {}
        try { registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND)) } catch (_: Exception) {}
        try { adapter.startDiscovery() } catch (_: Exception) {}
    }

    // Add / Edit product dialog
    private fun openAddEditDialog(producto: Producto?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_producto, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreProducto)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecioProducto)
        val etComando = dialogView.findViewById<EditText>(R.id.etComandoProducto)
        val ivProductoDialog = dialogView.findViewById<ImageView>(R.id.ivProductoDialog)
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)

        producto?.let {
            etNombre.setText(it.nombre)
            etPrecio.setText(it.precio)
            etComando.setText(it.comando)
            it.imagenPath?.let { path -> ivProductoDialog.setImageURI(Uri.parse(path)) }
        }

        currentDialogImageView = ivProductoDialog
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

                if (producto == null) {
                    val nuevo = Producto(id = (listaProductos.maxOfOrNull { it.id } ?: 0) + 1,
                        nombre = nombre.ifEmpty { "Sin nombre" },
                        precio = if (precio.isEmpty()) "$0" else precio,
                        imagenPath = imagenPath,
                        comando = comando)
                    listaProductos.add(nuevo)
                } else {
                    producto.nombre = nombre.ifEmpty { producto.nombre }
                    producto.precio = if (precio.isEmpty()) producto.precio else precio
                    producto.comando = comando
                    producto.imagenPath = imagenPath ?: producto.imagenPath
                }

                ProductStorage.saveProducts(this, listaProductos)
                recyclerView.adapter?.notifyDataSetChanged()
                currentDialogImageView = null
                pendingImageUri = null
            }
            .setNegativeButton("Cancelar") { _, _ ->
                currentDialogImageView = null
                pendingImageUri = null
            }
            .create()
        dialog.show()
    }

    // Save detected IP into internal config.txt (filesDir)
    private fun saveDetectedIp(ip: String) {
        try {
            val file = File(filesDir, "config.txt")
            file.writeText(ip.trim())
            Toast.makeText(this, "IP guardada: $ip", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    // Send commands
    private fun sendCommandFromProducto(producto: Producto) {
        val dispositivo = dispositivoSeleccionado
        if (dispositivo == null) {
            Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
            return
        }

        // If device type suggests BT-only, skip HTTP attempt (but we keep fallback)
        if (dispositivo.type.contains("STM32", true)) {
            // Bluetooth sending - placeholder (requires RFCOMM implementation)
            Toast.makeText(this, "STM32: enviar por Bluetooth (revisar pairing/UUID)", Toast.LENGTH_SHORT).show()
            return
        }

        // HTTP send
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sendCommandWifiBlocking(dispositivo.ip, producto.comando.ifEmpty { producto.nombre })
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Comando enviado a ${dispositivo.ip}", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Error enviando: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun sendToAllDevices() {
        if (listaProductos.isEmpty() || dispositivosDetectados.isEmpty()) {
            Toast.makeText(this, "No hay productos o dispositivos", Toast.LENGTH_SHORT).show(); return
        }
        CoroutineScope(Dispatchers.Main).launch {
            tvProgress.text = "Enviando a todos..."
            val results = withContext(Dispatchers.IO) {
                val res = mutableListOf<Pair<String, Boolean>>()
                for (dev in dispositivosDetectados) {
                    for (prod in listaProductos) {
                        try {
                            sendCommandWifiBlocking(dev.ip, prod.comando.ifEmpty { prod.nombre })
                            res.add(dev.ip to true)
                        } catch (_: Exception) { res.add(dev.ip to false) }
                    }
                }
                res
            }
            val ok = results.count { it.second }
            tvProgress.text = "Envíos completados: $ok success"
            Toast.makeText(this@MainActivity, "Envíos finalizados", Toast.LENGTH_SHORT).show()
        }
    }

    // Blocking HTTP GET (IO thread)
    @Throws(Exception::class)
    private fun sendCommandWifiBlocking(ip: String, command: String) {
        val url = URL("http://$ip/$command")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 2500
        conn.readTimeout = 2500
        conn.requestMethod = "GET"
        try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
