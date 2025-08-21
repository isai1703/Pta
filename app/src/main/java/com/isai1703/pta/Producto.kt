package com.isai1703.pta

/**
 * Modelo de producto utilizado en el catálogo de la aplicación.
 * Compatible con imágenes seleccionadas desde la galería del teléfono.
 * Se mantiene compatibilidad con envío de comandos (WiFi/Bluetooth) a ESP32, Raspberry Pi, etc.
 */
data class Producto(
    val id: Int,                // Identificador único del producto
    var nombre: String,         // Nombre del producto
    var precio: String,         // Precio en formato String (puede incluir moneda)
    var imagenPath: String? = null // Ruta o URI de la imagen seleccionada
)
