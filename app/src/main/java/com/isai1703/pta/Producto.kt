package com.isai1703.pta

data class Producto(
    val id: Int,
    val nombre: String,
    val imagen: Int,
    var disponible: Boolean = true
)
