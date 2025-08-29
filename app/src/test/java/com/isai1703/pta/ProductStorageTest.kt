package com.isai1703.pta

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.isai1703.pta.utils.ProductStorage
import org.junit.Assert.*
import org.junit.Test

class ProductStorageTest {

    @Test
    fun testSaveAndLoadProducts() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Lista de productos de ejemplo
        val productos = listOf(
            Producto(1, "Coca-Cola", "$15", "coca.png", "RELEASE_1"),
            Producto(2, "Pepsi", "$14", "pepsi.png", "RELEASE_2")
        )

        // Guardar
        ProductStorage.saveProducts(context, productos)

        // Cargar
        val loaded = ProductStorage.loadProducts(context)

        // Validaciones
        assertEquals("El tamaño debe coincidir", productos.size, loaded.size)
        assertEquals("El nombre debe coincidir", productos[0].nombre, loaded[0].nombre)
        assertEquals("El precio debe coincidir", productos[1].precio, loaded[1].precio)
        assertEquals("El comando debe coincidir", productos[1].comando, loaded[1].comando)
    }
}
