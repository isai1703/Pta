package com.isai1703.pta

data class Producto(
    var id: Int,
    var nombre: String,
    var precio: String,
    var imagenPath: String? = null,
    var comando: String = ""
)
