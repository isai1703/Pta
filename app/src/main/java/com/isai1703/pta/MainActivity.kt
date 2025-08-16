package com.isai1703.pta

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView

    private val listaProductos = mutableListOf<Producto>()
    private val dispositivosDetectados = mutableListOf<Dispositivo>()

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ProductoAdapter(listaProductos) { producto ->
            sendCommand(producto)
        }

        // Llenar productos de ejemplo con icono de prueba
        listaProductos.add(Producto("Producto 1", "$10", R.drawable.icon_prueba))
        listaProductos.add(Producto("Producto 2", "$20", R.drawable.icon_prueba))
        recyclerView.adapter?.notifyDataSetChanged()

        // Iniciar escaneo de dispositivos
        scanDevices()
    }

    // Clase de dispositivo detectado
    data class Dispositivo(val ip: String, val tipo: String, val nombre: String)

    // Envío de comando WiFi o Bluetooth según dispositivo
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
                else -> {
                    Toast.makeText(this, "Dispositivo no soportado", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No hay dispositivos detectados", Toast.LENGTH_SHORT).show()
        }
    }

    // Enviar comando por WiFi/HTTP
    private fun sendCommandWifi(ip: String, comando: String) {
        Thread {
            try {
                val url = URL("http://$ip/command?cmd=$comando")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 500
                connection.readTimeout = 500
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

    // Enviar comando por Bluetooth
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

    // Escaneo de dispositivos con ThreadPool y progreso
    private fun scanDevices() {
        val handler = Handler(Looper.getMainLooper())
        val executor = Executors.newFixedThreadPool(20)
        val ips = generateIPsInLocalSubnet()
        val total = ips.size
        var current = 0

        progressBar.max = total
        progressBar.progress = 0

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

    // Generar IPs locales
    private fun generateIPsInLocalSubnet(): List<String> {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        val baseIP = dhcp.ipAddress
        val ips = mutableListOf<String>()
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

    // Detectar dispositivo por puerto y firma HTTP
    private fun detectDevice(ip: String): Dispositivo? {
        return try {
            // Intento de conexión HTTP para identificar tipo
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 200)
            socket.close()
            val url = URL("http://$ip")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 200
            connection.readTimeout = 200
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
