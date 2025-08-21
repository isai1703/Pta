package com.isai1703.pta

data class Producto(
    val id: Int,
    var nombre: String,
    var precio: String,
    var imagenPath: String? = null // URI de la imagen seleccionada
)
