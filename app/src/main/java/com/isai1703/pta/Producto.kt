package com.isai1703.pta

data class Producto(
    val id: Int,
    val nombre: String,
    val imagenRes: Int,
    var disponible: Boolean = true
)
