package com.example.operacionescrud

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface FirestoreApiService {
    @GET("productos")
    suspend fun obtenerProductos(): FirestoreListResponse

    companion object {
        private const val PROJECT_ID = "operaciones-crud-8ba3c"
        private const val BASE_URL =
            "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/"

        // shortcut: sin interceptor de Auth token, suficiente mientras las reglas de Firestore permitan lectura publica
        val instance: FirestoreApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FirestoreApiService::class.java)
        }
    }
}

data class FirestoreListResponse(
    @SerializedName("documents") val documents: List<FirestoreDocumentDto>? = null,
)

data class FirestoreDocumentDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("fields") val fields: ProductoFieldsDto? = null,
) {
    fun toProducto(): Producto {
        val f = fields
        return Producto(
            id = f?.id?.stringValue?.ifBlank { null } ?: name.substringAfterLast('/'),
            nombre = f?.nombre?.stringValue.orEmpty(),
            descripcion = f?.descripcion?.stringValue.orEmpty(),
            cantidad = f?.cantidad?.integerValue?.toIntOrNull()
                ?: f?.cantidad?.doubleValue?.toInt() ?: 0,
            precio = f?.precio?.doubleValue
                ?: f?.precio?.integerValue?.toDoubleOrNull() ?: 0.0,
            proveedor = f?.proveedor?.stringValue.orEmpty(),
        )
    }
}

data class ProductoFieldsDto(
    @SerializedName("id") val id: FirestoreValueDto? = null,
    @SerializedName("nombre") val nombre: FirestoreValueDto? = null,
    @SerializedName("descripcion") val descripcion: FirestoreValueDto? = null,
    @SerializedName("cantidad") val cantidad: FirestoreValueDto? = null,
    @SerializedName("precio") val precio: FirestoreValueDto? = null,
    @SerializedName("proveedor") val proveedor: FirestoreValueDto? = null,
)

data class FirestoreValueDto(
    @SerializedName("stringValue") val stringValue: String? = null,
    @SerializedName("integerValue") val integerValue: String? = null,
    @SerializedName("doubleValue") val doubleValue: Double? = null,
)
