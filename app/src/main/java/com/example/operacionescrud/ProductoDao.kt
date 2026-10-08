package com.example.operacionescrud

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(producto: Producto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(productos: List<Producto>)

    @Query("SELECT * FROM productos")
    suspend fun obtenerTodos(): List<Producto>

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' OR proveedor LIKE '%' || :query || '%' OR descripcion LIKE '%' || :query || '%'")
    suspend fun buscar(query: String): List<Producto>

    @Update
    suspend fun actualizar(producto: Producto)

    @Delete
    suspend fun eliminar(producto: Producto)
}