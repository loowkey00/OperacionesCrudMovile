package com.example.operacionescrud

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var nombre: String = "",
    var descripcion: String = "",
    var cantidad: Int = 0,
    var precio: Double = 0.0,
    var proveedor: String = "",
)