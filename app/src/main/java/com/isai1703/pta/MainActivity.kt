package com.isai1703.pta

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnConnect: Button

    // Data
    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<DeviceInfo>()
    private var dispositivoSeleccionado: DeviceInfo? = null

    // Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    // Device Manager
    private lateinit var deviceManager: DeviceManager

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
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
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
                        name = dName ?: typeName,
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

        deviceManager = DeviceManager()

        // Bind UI
        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        btnSendAll = findViewById(R.id.btnSendAll)
        btnConnect = findViewById(R.id.btnConnect)

        checkAndRequestPermissions()
        loadSavedConfig()
        loadProducts()

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
            saveDetectedIp(selected.ip ?: "")
            Toast.makeText(this, "Seleccionado: ${selected.name} (${selected.ip})", Toast.LENGTH_SHORT).show()
        }

        // Listeners
        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
        btnSendAll.setOnClickListener { sendToAllDevices() }
        btnConnect.setOnClickListener { connectToSelected() }
    }

        // Agregar IP manualmente
        findViewById<Button>(R.id.btnManualIP)?.setOnClickListener {
            showManualIPDialog()
        private fun showManualIPDialog() {
            val input = EditText(this)
            input.hint = "192.168.100.24"
    
            AlertDialog.Builder(this)
                .setTitle("Agregar IP manualmente")
                .setView(input)
                .setPositiveButton("Agregar") { _, _ ->
                    val ip = input.text.toString().trim()
                    if (ip.isNotEmpty()) {
                        val device = DeviceInfo(
                            ip = ip,
                            name = "Máquina Nochebuena (manual)",
                            type = DeviceType.GENERIC_HTTP
                        )
                        dispositivosDetectados.add(device)
                        dispositivoSeleccionado = device
                        recyclerViewDevices.adapter?.notifyDataSetChanged()
                        saveDetectedIp(ip)
                        Toast.makeText(this, "IP agregada: $ip", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show() 
         }
}
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {
        }
        deviceManager.disconnect()
    }

    // -------- CONFIGURACIÓN
    private fun loadSavedConfig() {
        try {
            val file = File(filesDir, "config.txt")
            if (file.exists()) {
                val lines = file.readLines()
                if (lines.isNotEmpty()) {
                    val savedIp = lines[0].trim()
                    // Intenta conectar automáticamente a la IP guardada
                    val savedDevice = DeviceInfo(
                        ip = savedIp,
                        name = "Máquina Nochebuena (guardada)",
                        type = DeviceType.GENERIC_HTTP
                    )
                    dispositivosDetectados.add(savedDevice)
                    dispositivoSeleccionado = savedDevice
                    recyclerViewDevices.adapter?.notifyDataSetChanged()
                    tvProgress.text = "IP guardada cargada: $savedIp"
                }
            }
        } catch (e: Exception) {
            tvProgress.text = "No hay configuración guardada"
        }
    }

    private fun loadProducts() {
        listaProductos.clear()
        listaProductos.addAll(ProductStorage.loadProducts(this))
        
        // Si no hay productos, crea los 60 por defecto
        if (listaProductos.isEmpty()) {
            generateDefaultProducts()
            ProductStorage.saveProducts(this, listaProductos)
        }
    }

    private fun generateDefaultProducts() {
        // Genera 60 productos con comandos para motores
        for (i in 1..60) {
            listaProductos += Producto(
                id = i,
                nombre = "Producto $i",
                precio = "$${i * 10}",
                imagenUri = null,
                comando = "dispense?motor=$i"
            )
        }
    }

    // -------- PERMISOS
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
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
            tvProgress.text = "Escaneando todas las redes disponibles..."
            progressBar.progress = 0
            
            val subnets = listOf(
                "192.168.100",   // RED DE LA MÁQUINA
                "192.168.0",
                "192.168.50",
                "192.168.1",
                "192.168.2"
            )
            
            val found = withContext(Dispatchers.IO) {
                NetworkScanner.scanMultipleSubnetsWithProgress(
                    subnets,
                    chunkSize = 32
                ) { scanned, total, foundDevice ->
                    runOnUiThread {
                        val percent = if (total > 0) (scanned * 100 / total) else 0
                        progressBar.progress = percent
                        tvProgress.text = "Escaneadas $scanned / $total IPs"
                        foundDevice?.let {
                            if (!dispositivosDetectados.any { d -> d.ip == it.ip }) {
                                val deviceInfo = it.asDeviceInfo()
                                dispositivosDetectados.add(deviceInfo)
                                recyclerViewDevices.adapter?.notifyDataSetChanged()
                                
                                // Auto-selecciona la máquina expendedora encontrada
                                if (it.name.contains("Nochebuena", ignoreCase = true) || 
                                    it.name.contains("Vending", ignoreCase = true)) {
                                    dispositivoSeleccionado = deviceInfo
                                    saveDetectedIp(it.ip)
                                }
                            }
                        }
                    }
                }
            }
            
            if (found != null) {
                runOnUiThread {
                    tvProgress.text = "Máquina encontrada: ${found.name} en ${found.ip}:${found.port}"
                    progressBar.progress = 100
                    Toast.makeText(
                        this@MainActivity, 
                        "Máquina expendedora detectada automáticamente", 
                        Toast.LENGTH_LONG
                    ).show()
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

    // -------- CONEXIÓN
    private fun connectToSelected() {
        val dispositivo = dispositivoSeleccionado ?: run {
            Toast.makeText(this, "Selecciona un dispositivo primero", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val ok = withContext(Dispatchers.IO) { deviceManager.connectToDevice(dispositivo) }
            Toast.makeText(
                this@MainActivity,
                if (ok) "Conectado a ${dispositivo.name}" else "Error de conexión",
                Toast.LENGTH_SHORT
            ).show()
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
                deviceManager.connectToDevice(dispositivo)
                deviceManager.sendCommand(producto.comando.ifEmpty { producto.nombre })
            }
            Toast.makeText(this@MainActivity, "Respuesta: $result", Toast.LENGTH_SHORT).show()
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
                    deviceManager.connectToDevice(dev)
                    for (prod in listaProductos) {
                        val resp = deviceManager.sendCommand(prod.comando.ifEmpty { prod.nombre })
                        res.add((dev.ip ?: "?") to resp)
                    }
                }
                res
            }
            val ok = results.count { !it.second.startsWith("Error") }
            tvProgress.text = "Envíos completados: $ok success"
            Toast.makeText(this@MainActivity, "Envíos finalizados", Toast.LENGTH_SHORT).show()
        }
    }

    // -------- DIALOGO AGREGAR/EDITAR PRODUCTO
    private fun openAddEditDialog(producto: Producto?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_product, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecio)
        val etComando = dialogView.findViewById<EditText>(R.id.etComando)
        val ivImagen = dialogView.findViewById<ImageView>(R.id.ivImagen)

        producto?.let {
            etNombre.setText(it.nombre)
            etPrecio.setText(it.precio)
            etComando.setText(it.comando)
            it.imagenUri?.let { uri -> ivImagen.setImageURI(Uri.parse(uri)) }
        }

        ivImagen.setOnClickListener {
            currentDialogImageView = ivImagen
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(if (producto == null) "Agregar producto" else "Editar producto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString()
                val precio = etPrecio.text.toString()
                val comando = etComando.text.toString()
                val uri = pendingImageUri?.toString()

                if (producto == null) {
                    val nuevo = Producto(
                        id = listaProductos.size + 1,
                        nombre = nombre,
                        precio = precio,
                        imagenUri = uri,
                        comando = comando
                    )
                    listaProductos.add(nuevo)
                } else {
                    producto.nombre = nombre
                    producto.precio = precio
                    producto.comando = comando
                    producto.imagenUri = uri
                }
                ProductStorage.saveProducts(this, listaProductos)
                recyclerView.adapter?.notifyDataSetChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
