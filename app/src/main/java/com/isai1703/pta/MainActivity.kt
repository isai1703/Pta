package com.isai1703.pta

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
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
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
                    val typeName = when (it.bluetoothClass?.majorDeviceClass) {
                        BluetoothClass.Device.Major.COMPUTER -> "Mini-PC"
                        BluetoothClass.Device.Major.PERIPHERAL -> "ESP32/STM32"
                        else -> "Desconocido"
                    }
                    val info = DeviceInfo(it.name ?: "Desconocido", it.address, typeName)
                    if (!dispositivosDetectados.contains(info)) {
                        dispositivosDetectados.add(info)
                        recyclerViewDevices.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

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
            Toast.makeText(this, "Dispositivo seleccionado: ${selected.ip}", Toast.LENGTH_SHORT)
                .show()
        }

        // Acciones
        btnScanDevices.setOnClickListener { scanDevices() }
        btnAddProduct.setOnClickListener { openAddEditDialog(null) }
    }

    // ---------------- Permisos dinámicos ----------------
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Lectura imágenes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
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
                        dispositivo.type.contains("Mini-PC", true) -> sendCommandWifi(
                    dispositivo.ip,
                    producto.nombre
                )

                dispositivo.type.contains("STM32", true) ||
                        dispositivo.type.contains("Raspberry", true) -> sendCommandBluetooth(producto.nombre)

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
                    Toast.makeText(this, "Comando enviado por WiFi: $responseCode", Toast.LENGTH_SHORT)
                        .show()
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
        }
    }

    // ---------------- Escaneo de dispositivos ----------------
    private fun scanDevices() {
        dispositivosDetectados.clear()
        recyclerViewDevices.adapter?.notifyDataSetChanged()
        progressBar.progress = 0
        tvProgress.text = "Escaneando..."

        // Registrar receptor para escaneo Bluetooth
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
        bluetoothAdapter?.startDiscovery()

        // Ejemplo de escaneo WiFi/HTTP (simulado)
        dispositivosDetectados += listOf(
            DeviceInfo("ESP32 Demo", "192.168.1.100", "ESP32"),
            DeviceInfo("Raspberry Pi Demo", "192.168.1.101", "Raspberry")
        )

        recyclerViewDevices.adapter?.notifyDataSetChanged()
        tvProgress.text = "Escaneo completado: ${dispositivosDetectados.size} dispositivos"
        progressBar.progress = 100
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // No hacer nada si ya no estaba registrado
        }
    }
}
