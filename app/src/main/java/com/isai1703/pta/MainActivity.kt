package com.isai1703.pta

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnScanDevices: Button
    private lateinit var btnAddProduct: Button

    // Datos
    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<DeviceInfo>()
    private var dispositivoSeleccionado: DeviceInfo? = null

    // Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Permisos
    private val REQUEST_CODE_PERMISSIONS = 1001

    // Estado selector de imágenes
    private var pendingImageUri: Uri? = null
    private var currentDialogImageView: ImageView? = null

    // Selector moderno de imágenes
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                pendingImageUri = it
                currentDialogImageView?.setImageURI(it)
            }
        }

    // ---------------- BroadcastReceiver para escaneo Bluetooth ----------------
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.let {
                    // En Android 12+ leer bluetoothClass / name requiere BLUETOOTH_CONNECT
                    val hasBtConnect =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        else true

                    val majorClass = if (hasBtConnect) it.bluetoothClass?.majorDeviceClass else null
                    val typeName = when (majorClass) {
                        BluetoothClass.Device.Major.COMPUTER -> "Mini-PC"
                        BluetoothClass.Device.Major.PERIPHERAL -> "ESP32/STM32"
                        BluetoothClass.Device.Major.PHONE -> "Raspberry"
                        else -> "Desconocido"
                    }

                    val dName = if (hasBtConnect) it.name else null
                    val info = DeviceInfo(
                        ip = it.address,               // aquí usamos la MAC como "ip" para BT clásico
                        type = typeName,
                        name = dName ?: "Desconocido"
                    )

                    if (!dispositivosDetectados.any { d -> d.ip == info.ip }) {
                        dispositivosDetectados.add(info)
                        recyclerViewDevices.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    // ---------------- Ciclo de vida ----------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        recyclerView = findViewById(R.id.recyclerView)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnAddProduct = findViewById(R.id.btnAddProduct)

        // Verificar permisos
        checkAndRequestPermissions()

        // RecyclerView productos
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(
            productos = listaProductos,
            onSendCommandClick = { producto -> sendCommand(producto) },
            onEditClick = { producto -> openAddEditDialog(producto) }
        )

        // Productos ejemplo
        if (listaProductos.isEmpty()) {
            listaProductos += listOf(
                Producto(id = 1, nombre = "Producto 1", precio = "$10", imagenPath = null),
                Producto(id = 2, nombre = "Producto 2", precio = "$20", imagenPath = null)
            )
            recyclerView.adapter?.notifyDataSetChanged()
        }

        // RecyclerView dispositivos
        recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        recyclerViewDevices.adapter = DeviceAdapter(dispositivosDetectados) { selected ->
            dispositivoSeleccionado = selected
            Toast.makeText(
                this,
                "Dispositivo seleccionado: ${selected.ip}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Acciones
        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
    }

    override fun onStop() {
        super.onStop()
        // Cancelar discovery para conservar batería y recursos
        val adapter = bluetoothAdapter
        if (adapter != null && adapter.isDiscovering) {
            try { adapter.cancelDiscovery() } catch (_: SecurityException) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
    }

    // ---------------- Escaneo Bluetooth ----------------
    private fun scanDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        // Permisos necesarios para escanear
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val okScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            if (!okScan) {
                Toast.makeText(this, "Falta permiso BLUETOOTH_SCAN", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
                return
            }
        } else {
            val okLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!okLoc) {
                Toast.makeText(this, "Falta permiso de ubicación para escanear BT", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
                return
            }
        }

        dispositivosDetectados.clear()
        recyclerViewDevices.adapter?.notifyDataSetChanged()

        if (adapter.isDiscovering) {
            try { adapter.cancelDiscovery() } catch (_: SecurityException) {}
        }

        // Registrar receiver (si ya estaba registrado, lo atrapamos en try/catch)
        try {
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } catch (_: Exception) { /* ya registrado */ }

        try {
            adapter.startDiscovery()
            Toast.makeText(this, "Escaneando dispositivos Bluetooth...", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Permiso insuficiente para iniciar escaneo", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Permisos dinámicos ----------------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Bluetooth
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

        // Lectura imágenes
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Algunos permisos fueron denegados, ciertas funciones podrían no funcionar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------- Agregar / Editar producto ----------------
    private fun openAddEditDialog(producto: Producto?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_producto, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreProducto)
        val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecioProducto)
        val ivProducto = dialogView.findViewById<ImageView>(R.id.ivProductoDialog)
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)

        producto?.let {
            etNombre.setText(it.nombre)
            etPrecio.setText(it.precio)
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
                val imagenPath = pendingImageUri?.toString()

                if (producto == null) {
                    val nuevo = Producto(
                        id = (listaProductos.maxOfOrNull { it.id } ?: 0) + 1,
                        nombre = nombre.ifEmpty { "Sin nombre" },
                        precio = if (precio.isEmpty()) "$0" else precio,
                        imagenPath = imagenPath
                    )
                    listaProductos.add(nuevo)
                } else {
                    producto.nombre = nombre.ifEmpty { producto.nombre }
                    producto.precio = if (precio.isEmpty()) producto.precio else precio
                    producto.imagenPath = imagenPath ?: producto.imagenPath
                }

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

    // ---------------- Enviar comandos ----------------
    private fun sendCommand(producto: Producto) {
        val dispositivo = dispositivoSeleccionado
        if (dispositivo != null) {
            when {
                dispositivo.type.contains("ESP32", true) ||
                        dispositivo.type.contains("Mini-PC", true) ->
                    sendCommandWifi(dispositivo.ip, producto.nombre)

                dispositivo.type.contains("STM32", true) ||
                        dispositivo.type.contains("Raspberry", true) ->
                    sendCommandBluetooth(producto.nombre)

                else -> Toast.makeText(
                    this,
                    "Dispositivo no soportado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "No hay dispositivo seleccionado",
                Toast.LENGTH_SHORT
            ).show()
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
                    Toast.makeText(
                        this,
                        "Respuesta WiFi: $responseCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Error enviando comando WiFi: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Envío real por Bluetooth RFCOMM (SPP) usando el primer dispositivo emparejado,
     * o si hay un dispositivo seleccionado en la lista, intenta emparejado con esa MAC.
     * Requiere BLUETOOTH_CONNECT en Android 12+.
     */
    private fun sendCommandBluetooth(comando: String) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Falta permiso BLUETOOTH_CONNECT", Toast.LENGTH_SHORT).show()
            checkAndRequestPermissions()
            return
        }

        // Cancelar discovery para no interferir con la conexión
        if (adapter.isDiscovering) {
            try { adapter.cancelDiscovery() } catch (_: SecurityException) {}
        }

        // Elegir el dispositivo: si seleccionaste uno de la lista (MAC), úsalo; si no, el primero emparejado.
        val macSeleccionada = dispositivoSeleccionado?.ip
        val bonded = try { adapter.bondedDevices } catch (_: SecurityException) { emptySet<BluetoothDevice>() }
        val target: BluetoothDevice? = when {
            !macSeleccionada.isNullOrEmpty() -> bonded.firstOrNull { it.address == macSeleccionada }
            else -> bonded.firstOrNull()
        }

        if (target == null) {
            Toast.makeText(this, "No hay dispositivos Bluetooth emparejados", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                socket = target.createRfcommSocketToServiceRecord(sppUuid)
                socket.connect()

                val out = socket.outputStream
                out.write(comando.toByteArray(Charsets.UTF_8))
                out.flush()

                runOnUiThread {
                    Toast.makeText(this, "Comando BT enviado", Toast.LENGTH_SHORT).show()
                }
            } catch (se: SecurityException) {
                runOnUiThread {
                    Toast.makeText(this, "Permiso insuficiente para conectar BT", Toast.LENGTH_SHORT).show()
                }
            } catch (io: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error BT: ${io.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error BT: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }
}
