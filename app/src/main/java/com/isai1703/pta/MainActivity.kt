// fragmento relevante de MainActivity.kt

// En onCreate(), actualizar el adapter:
recyclerView.adapter = ProductoAdapter(listaProductos,
    onSendCommandClick = { producto -> sendCommand(producto) },
    onEditClick = { producto -> openAddEditDialog(producto) }
)

// Nuevo botón para agregar producto
val btnAgregarProducto: Button = findViewById(R.id.btnAgregarProducto)
btnAgregarProducto.setOnClickListener {
    openAddEditDialog(null)
}

// Función para abrir diálogo
private fun openAddEditDialog(producto: Producto?) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_producto, null)
    val etNombre = dialogView.findViewById<EditText>(R.id.etNombreProducto)
    val etPrecio = dialogView.findViewById<EditText>(R.id.etPrecioProducto)
    val ivProducto = dialogView.findViewById<ImageView>(R.id.ivProductoDialog)
    val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnSeleccionarImagen)

    producto?.let {
        etNombre.setText(it.nombre)
        etPrecio.setText(it.precio)
        it.imagenPath?.let { path ->
            ivProducto.setImageURI(android.net.Uri.parse(path))
        }
    }

    var selectedImagePath: String? = producto?.imagenPath

    btnSeleccionarImagen.setOnClickListener {
        // Abrir galería para seleccionar imagen
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
    }

    val dialog = AlertDialog.Builder(this)
        .setTitle(if (producto == null) "Agregar Producto" else "Editar Producto")
        .setView(dialogView)
        .setPositiveButton("Guardar") { _, _ ->
            val nombre = etNombre.text.toString()
            val precio = etPrecio.text.toString()
            if (producto == null) {
                // Nuevo producto
                val nuevo = Producto(listaProductos.size + 1, nombre, selectedImagePath)
                nuevo.precio = precio
                listaProductos.add(nuevo)
            } else {
                // Editar existente
                producto.nombre = nombre
                producto.precio = precio
                producto.imagenPath = selectedImagePath
            }
            recyclerView.adapter?.notifyDataSetChanged()
        }
        .setNegativeButton("Cancelar", null)
        .create()
    dialog.show()
}

// Manejar resultado galería
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
        data?.data?.let { uri ->
            selectedImagePath = uri.toString()
            ivProductoDialog.setImageURI(uri)
        }
    }
}
