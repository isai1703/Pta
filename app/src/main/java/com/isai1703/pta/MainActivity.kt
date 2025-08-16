package com.isai1703.pta

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.isai1703.pta.model.Producto
import com.isai1703.pta.model.ProductoAdapter
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnScanDevices: Button

    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<Dispositivo>()

    // Usar BluetoothManager en lugar de getDefaultAdapter()
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnScanDevices = findViewById(R.id.btnScanDevices)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(listaProductos) { producto ->
            sendCommand(producto)
        }

        // Productos de ejemplo
        listaProductos.add(Producto("Producto 1", "$10", R.drawable.icon_prueba))
        listaProductos.add(Producto("Producto 2", "$20", R.drawable.icon_prueba))
        recyclerView.adapter?.notifyDataSetChanged()

        btnScanDevices.setOnClickListener { scanDevices() }
    }

    data class Dispositivo(val ip: String, val tipo: String, val nombre: String)

    // -------- Enviar comandos --------
    private fun sendCommand(producto: Producto) {
        val dispositivo = dispositivosDetectados.firstOrNull()
        if (dispositivo != null) {
            when {
                dispositivo.tipo == "ESP32" || dispositivo.tipo.contains("Mini-PC", true) -> {
                    sendCommandWifi(dispositivo.ip, producto.nombre)
                }
                dispositivo.tipo.contains("STM32", true) || dispositivo.tipo.contains("Raspberry", true) -> {
                    sendCommandBluetooth(producto.nombre)
                }
                else -> Toast.makeText(this, "Dispositivo no soportado", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay dispositivos detectados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommandWifi(ip: String, comando: String) {
        Thread {
            try {
                val url = URL("http://$ip/command?cmd=$comando")
                val connection = url.openConnection() as HttpURLConnection
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
                    val outStream: OutputStream = socket.outputStream
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

    // -------- Escaneo de red --------
    private fun scanDevices() {
        val handler = Handler(Looper.getMainLooper())
        val executor = Executors.newFixedThreadPool(20)
        val ips = generateIPsInLocalSubnet()
        val total = ips.size
        var current = 0

        progressBar.max = total
        progressBar.progress = 0
        dispositivosDetectados.clear()

        for (ip in ips) {
            executor.execute {
                detectDevice(ip)?.let { dispositivo ->
                    dispositivosDetectados.add(dispositivo)
                }
                current++
                handler.post {
                    progressBar.progress = current
                    tvProgress.text = "Escaneando $current/$total"
                }
            }
        }
    }

    private fun generateIPsInLocalSubnet(): List<String> {
        val ips = mutableListOf<String>()
        val baseIP: Int = getDeviceIpAddress()

        for (i in 1..254) {
            val ip = (baseIP and 0xFFFFFF00.toInt()) or i
            val formattedIP = String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
            ips.add(formattedIP)
        }
        return ips
    }

    // Método moderno para obtener IP (reemplaza dhcpInfo)
    private fun getDeviceIpAddress(): Int {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: Network? = connectivityManager.activeNetwork
        val linkProperties: LinkProperties? = connectivityManager.getLinkProperties(activeNetwork)
        val inetAddress = linkProperties?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address }
        return inetAddress?.address?.let {
            (it[3].toInt() and 0xff shl 24) or
                    (it[2].toInt() and 0xff shl 16) or
                    (it[1].toInt() and 0xff shl 8) or
                    (it[0].toInt() and 0xff)
        } ?: 0
    }

    private fun detectDevice(ip: String): Dispositivo? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 2000)
            socket.close()

            val url = URL("http://$ip")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            val header = connection.getHeaderField("Server") ?: ""

            val tipo = when {
                header.contains("ESP32", true) -> "ESP32"
                header.contains("Raspberry", true) -> "Raspberry Pi"
                header.contains("STM32", true) -> "STM32"
                header.contains("TarjetaPersonalizada", true) -> "Tarjeta personalizada"
                else -> "Mini-PC"
            }

            Dispositivo(ip, tipo, "Dispositivo $ip")
        } catch (e: Exception) {
            null
        }
    }
}
