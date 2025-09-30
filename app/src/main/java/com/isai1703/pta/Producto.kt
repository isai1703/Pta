package com.isai1703.pta

data class Producto(
    var id: Int,
    var nombre: String,
    var precio: String,
    var imagenUri: String? = null,  // Cambiado de imagenPath
    var comando: String = ""
)
