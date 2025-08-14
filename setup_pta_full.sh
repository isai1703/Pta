#!/bin/bash
set -e

# ------------------------
# Rutas del proyecto
# ------------------------
APP_ROOT=~/Pta-filtrado/app/src/main
KOTLIN=~/Pta-filtrado/app/src/main/java/com/isai1703/pta
RES=~/Pta-filtrado/app/src/main/res
LAYOUT=$RES/layout
DRAWABLE=$RES/drawable
ASSETS=~/Pta-filtrado/app/src/main/assets
MANIFEST=~/Pta-filtrado/app/src/main/AndroidManifest.xml
PROJECT_ROOT=~/Pta-filtrado

mkdir -p "$KOTLIN" "$LAYOUT" "$DRAWABLE" "$ASSETS"

# ------------------------
# Drawables requeridos (usar nombres que ya tienes)
# ------------------------
# Si faltan, los creamos para evitar fallo de recursos
if [ ! -f "$DRAWABLE/ic_disconnected.xml" ]; then
  cat > "$DRAWABLE/ic_disconnected.xml" <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#D32F2F" android:pathData="M2,12h20M12,2v20"/>
</vector>
EOF
fi

if [ ! -f "$DRAWABLE/ic_connected.xml" ]; then
  cat > "$DRAWABLE/ic_connected.xml" <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#388E3C" android:pathData="M5,13l4,4 10,-10" />
</vector>
EOF
fi

# Alias opcionales por si en layout se usan otros nombres
[ -f "$DRAWABLE/ic_desconectado.xml" ] || cp "$DRAWABLE/ic_disconnected.xml" "$DRAWABLE/ic_desconectado.xml" || true
[ -f "$DRAWABLE/ic_conectado.xml" ]   || cp "$DRAWABLE/ic_connected.xml"   "$DRAWABLE/ic_conectado.xml"   || true

# Ícono genérico de producto si faltara
if [ ! -f "$DRAWABLE/ic_producto.xml" ] && [ ! -f "$DRAWABLE/ic_producto.jpg" ]; then
  cat > "$DRAWABLE/ic_producto.xml" <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="60dp" android:height="60dp" android:viewportWidth="24" android:viewportHeight="24">
  <path android:fillColor="#1976D2" android:pathData="M4,4h16v16H4z"/>
</vector>
EOF
fi

# ------------------------
# assets/config.txt (IP preferida, opcional)
# ------------------------
if [ ! -f "$ASSETS/config.txt" ]; then
  echo "192.168.1.50" > "$ASSETS/config.txt"
fi

# ------------------------
# AndroidManifest.xml (permisos + activity)
# ------------------------
cat > "$MANIFEST" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.isai1703.pta">

    <!-- Permisos de red -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>

    <!-- Bluetooth (manejo compat y 12+) -->
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:required="false"/>
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:required="false"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:required="false"/>

    <application
        android:allowBackup="true"
        android:label="Pta-filtrado"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.NoActionBar">
        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

# ------------------------
# Layout principal con lista de productos + lista de dispositivos
# ------------------------
cat > "$LAYOUT/activity_main.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="8dp">

    <TextView
        android:id="@+id/tvTituloDispositivos"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Dispositivos detectados"
        android:textSize="18sp"
        android:padding="4dp" />

    <ImageView
        android:id="@+id/iconoEstadoGeneral"
        android:layout_width="28dp"
        android:layout_height="28dp"
        android:src="@drawable/ic_disconnected"
        android:layout_gravity="end"
        android:contentDescription="@string/untitled" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvDispositivos"
        android:layout_width="match_parent"
        android:layout_height="140dp"
        android:paddingBottom="8dp" />

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="#dddddd"
        android:layout_marginVertical="6dp"/>

    <TextView
        android:id="@+id/tvTituloProductos"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Productos"
        android:textSize="18sp"
        android:padding="4dp" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvProductos"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="6dp">

        <Button
            android:id="@+id/btnEscanear"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:text="Escanear"/>

        <Button
            android:id="@+id/btnEnviarTodos"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:text="Enviar a todos"
            android:layout_marginStart="8dp"/>
    </LinearLayout>

</LinearLayout>
EOF

# ------------------------
# Item para la lista de productos
# ------------------------
cat > "$LAYOUT/item_producto.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="horizontal"
    android:padding="8dp"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/ivImagen"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:src="@drawable/ic_producto"
        android:layout_marginEnd="8dp"/>

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content">

        <TextView
            android:id="@+id/tvNombre"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Producto"
            android:textSize="16sp"/>
        <TextView
            android:id="@+id/tvDisponible"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Disponible"
            android:textSize="12sp"/>
    </LinearLayout>

    <Button
        android:id="@+id/btnEnviar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Enviar"/>
</LinearLayout>
EOF

# ------------------------
# Item para la lista de dispositivos
# ------------------------
cat > "$LAYOUT/item_device.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="horizontal"
    android:padding="6dp"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/ivStatus"
        android:layout_width="22dp"
        android:layout_height="22dp"
        android:src="@drawable/ic_disconnected"
        android:layout_marginEnd="8dp"/>

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content">

        <TextView
            android:id="@+id/tvNombreDispositivo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Nombre"
            android:textSize="14sp"/>
        <TextView
            android:id="@+id/tvDireccionDispositivo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="192.168.1.x / BT"
            android:textSize="12sp"/>
        <TextView
            android:id="@+id/tvTipoDispositivo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Tipo"
            android:textSize="12sp"/>
    </LinearLayout>

    <Button
        android:id="@+id/btnConectar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Conectar"/>
</LinearLayout>
EOF

# ------------------------
# Kotlin: modelos y adapters
# ------------------------
cat > "$KOTLIN/Producto.kt" <<'EOF'
package com.isai1703.pta

data class Producto(
    val id: Int,
    val nombre: String,
    val imagenRes: Int,
    var disponible: Boolean = true
)
EOF

cat > "$KOTLIN/Device.kt" <<'EOF'
package com.isai1703.pta

enum class TipoDispositivo { ESP32, RASPBERRY, STM32, MINIPC, DESCONOCIDO }

data class Device(
    val nombre: String,
    val address: String,        // IP o MAC
    val tipo: TipoDispositivo,
    var conectado: Boolean = false
)
EOF

cat > "$KOTLIN/ProductoAdapter.kt" <<'EOF'
package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private val items: List<Producto>,
    private val onClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.VH>() {

    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvDisponible: TextView = v.findViewById(R.id.tvDisponible)
        val ivImagen: ImageView = v.findViewById(R.id.ivImagen)
        val btnEnviar: Button = v.findViewById(R.id.btnEnviar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.tvNombre.text = p.nombre
        h.tvDisponible.text = if (p.disponible) "Disponible" else "Agotado"
        h.ivImagen.setImageResource(p.imagenRes)
        h.btnEnviar.isEnabled = p.disponible
        h.btnEnviar.setOnClickListener { onClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
EOF

cat > "$KOTLIN/DeviceAdapter.kt" <<'EOF'
package com.isai1703.pta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val items: MutableList<Device>,
    private val onConnectClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
        val tvNombre: TextView = v.findViewById(R.id.tvNombreDispositivo)
        val tvDireccion: TextView = v.findViewById(R.id.tvDireccionDispositivo)
        val tvTipo: TextView = v.findViewById(R.id.tvTipoDispositivo)
        val btnConectar: Button = v.findViewById(R.id.btnConectar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val d = items[pos]
        h.tvNombre.text = d.nombre
        h.tvDireccion.text = d.address
        h.tvTipo.text = d.tipo.name
        h.ivStatus.setImageResource(if (d.conectado) R.drawable.ic_connected else R.drawable.ic_disconnected)
        h.btnConectar.setOnClickListener { onConnectClick(d) }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(nuevos: List<Device>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    fun markConnected(address: String, connected: Boolean) {
        val i = items.indexOfFirst { it.address == address }
        if (i >= 0) {
            items[i] = items[i].copy(conectado = connected)
            notifyItemChanged(i)
        }
    }
}
EOF

cat > "$KOTLIN/PermissionsHelper.kt" <<'EOF'
package com.isai1703.pta

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsHelper {
    private const val REQ = 101

    fun ensure(activity: Activity) {
        val needs = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needs += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                needs += Manifest.permission.BLUETOOTH_SCAN
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needs += Manifest.permission.BLUETOOTH_CONNECT
            }
        }
        if (needs.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, needs.toTypedArray(), REQ)
        }
    }
}
EOF

# ------------------------
# Kotlin: Utilidades de red
# ------------------------
cat > "$KOTLIN/NetworkUtils.kt" <<'EOF'
package com.isai1703.pta

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.math.pow

object NetworkUtils {

    fun localIPv4(context: Context): String? {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wm.connectionInfo.ipAddress
            if (ipInt == 0) return null
            val ip = String.format("%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
            return ip
        } catch (_: Exception) { }
        // Fallback: enumerar interfaces
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is InetAddress) {
                        val host = a.hostAddress ?: continue
                        if (host.count { it == '.' } == 3 && !host.contains(':')) return host
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    fun subnetCandidates(ip: String): List<String> {
        val parts = ip.split(".")
        if (parts.size != 4) return (1..254).map { "192.168.1.$it" }
        val base = parts.take(3).joinToString(".")
        return (1..254).map { "$base.$it" }
    }

    fun isTcpOpen(host: String, port: Int, timeoutMs: Int = 200): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
EOF

# ------------------------
# Kotlin: MainActivity con escaneo multi-dispositivo + lista + conexión
# ------------------------
cat > "$KOTLIN/MainActivity.kt" <<'EOF'
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
EOF

# ------------------------
# Mensajes finales y (opcional) Git push
# ------------------------
echo "✅ Archivos creados/actualizados."
echo "Rutas clave:"
echo " - $MANIFEST"
echo " - $LAYOUT/activity_main.xml"
echo " - $LAYOUT/item_producto.xml"
echo " - $LAYOUT/item_device.xml"
echo " - $KOTLIN/MainActivity.kt"
echo " - $KOTLIN/Producto.kt, Device.kt, ProductoAdapter.kt, DeviceAdapter.kt, PermissionsHelper.kt, NetworkUtils.kt"
echo " - $ASSETS/config.txt"
echo
echo "👉 Para compilar:"
echo "cd $PROJECT_ROOT && ./gradlew clean assembleDebug"
echo
echo "👉 (Opcional) Hacer commit y push a GitHub (requiere 'origin' configurado y permisos):"
echo "cd $PROJECT_ROOT && git add . && git commit -m \"feat: escaneo multidispositivo + lista y conexión automática sin perder funcionalidades\" && git push origin main"
