package com.isai1703.pta

import org.json.JSONObject

data class Producto(
    var id: Int,
    var nombre: String,
    var precio: String,
    var imagenPath: String?,
    var comando: String = ""   // se usa para comandos como RELEASE_5, etc.
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("nombre", nombre)
        o.put("precio", precio)
        o.put("imagenPath", imagenPath)
        o.put("comando", comando)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Producto {
            return Producto(
                id = o.optInt("id", 0),
                nombre = o.optString("nombre", "Sin nombre"),
                precio = o.optString("precio", "$0"),
                imagenPath = if (o.has("imagenPath") && !o.isNull("imagenPath")) o.getString("imagenPath") else null,
                comando = o.optString("comando", "")
            )
        }
    }
}
