package com.isai1703.pta

data class Producto(
    var id: Int,
    var nombre: String,
    var precio: String,
    var imagenPath: String? = null, // uri string: content://... o file://... o null
    var comando: String = ""
)
