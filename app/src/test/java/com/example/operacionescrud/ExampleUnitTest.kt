package com.example.operacionescrud

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun firestoreRestJson_mapsToProducto() {
        val json = """
            {
              "documents": [
                {
                  "name": "projects/operaciones-crud-8ba3c/databases/(default)/documents/productos/abc-123",
                  "fields": {
                    "nombre": { "stringValue": "Teclado Mecánico" },
                    "descripcion": { "stringValue": "Switch rojo" },
                    "cantidad": { "integerValue": "15" },
                    "precio": { "doubleValue": 45990.5 },
                    "proveedor": { "stringValue": "Keychron" }
                  }
                }
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, FirestoreListResponse::class.java)
        val productos = response.documents.orEmpty().map { it.toProducto() }

        assertEquals(1, productos.size)
        assertEquals(
            Producto(
                id = "abc-123",
                nombre = "Teclado Mecánico",
                descripcion = "Switch rojo",
                cantidad = 15,
                precio = 45990.5,
                proveedor = "Keychron",
            ),
            productos.first(),
        )
    }
}
