package com.isai1703.pta.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isai1703.pta.Producto
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Persistencia local de productos en JSON.
 */
object ProductStorage {
    private const val FILE_NAME = "products.json"

    fun saveProducts(context: Context, products: List<Producto>) {
        try {
            val json: String = Gson().toJson(products)
            val file = File(context.filesDir, FILE_NAME)
            FileWriter(file, false).use { writer ->
                writer.write(json)
                writer.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadProducts(context: Context): List<Producto> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()

            FileReader(file).use { reader ->
                val type = object : TypeToken<List<Producto>>() {}.type
                Gson().fromJson<List<Producto>>(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
