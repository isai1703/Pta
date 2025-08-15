package com.isai1703.pta

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RecyclerView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val productos = mutableListOf<Producto>()
    private lateinit var adapter: ProductoAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val index = productos.size - 1
            productos[index].imagenPath = it.toString()
            adapter.notifyItemChanged(index)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycler = findViewById(R.id.recyclerProductos)
        progressBar = findViewById(R.id.progressBar)

        adapter = ProductoAdapter(this, productos) { producto ->
            Toast.makeText(this, "Enviando comando a ${producto.nombre}", Toast.LENGTH_SHORT).show()
            NetworkUtils.sendCommand(this, producto.nombre)
        }

        recycler.adapter = adapter

        val addButton: Button = findViewById(R.id.addProductButton)
        addButton.setOnClickListener {
            productos.add(Producto(productos.size + 1, "Producto ${productos.size + 1}"))
            adapter.notifyItemInserted(productos.size - 1)
            pickImage.launch("image/*")
        }

        val scanButton: Button = findViewById(R.id.scanDevicesButton)
        scanButton.setOnClickListener {
            progressBar.progress = 0
            Thread {
                val encontrados = NetworkUtils.scanDevices(this) { prog ->
                    runOnUiThread { progressBar.progress = prog }
                }
                runOnUiThread {
                    Toast.makeText(this, "Encontrados: ${encontrados.size} dispositivos", Toast.LENGTH_LONG).show()
                }
            }.start()
        }
    }
}
