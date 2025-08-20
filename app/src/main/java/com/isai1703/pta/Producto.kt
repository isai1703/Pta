package com.isai1703.pta.model

data class Producto(
    val id: Int,
    var nombre: String,
    var precio: String,
    var imagenPath: String? = null // URI de imagen desde galería
)
