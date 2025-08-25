package com.isai1703.pta.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isai1703.pta.models.Producto
import java.io.File

object ProductStorage {
    private const val FILE_NAME = "products.json"

    fun saveProducts(context: Context, products: List<Producto>) {
        try {
            val gson = Gson()
            val json = gson.toJson(products)
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadProducts(context: Context): MutableList<Producto> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return mutableListOf()
            val json = file.readText()
            val gson = Gson()
            val type = object : TypeToken<MutableList<Producto>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
}
