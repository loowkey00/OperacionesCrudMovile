package com.example.operacionescrud

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class InventarioRepository(private val dao: ProductoDao) {
    private val dbFirestore = FirebaseFirestore.getInstance()
    private val coleccion = dbFirestore.collection("productos")

    suspend fun crear(producto: Producto): Unit = withContext(Dispatchers.IO) {
        dao.insertar(producto)
        coleccion.document(producto.id).set(producto).await()
    }

    suspend fun leerTodos(): List<Producto> = withContext(Dispatchers.IO) {
        return@withContext dao.obtenerTodos()
    }

    suspend fun buscar(query: String): List<Producto> = withContext(Dispatchers.IO) {
        return@withContext if (query.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscar(query)
        }
    }

    suspend fun filtrar(query: String, minPrecio: Double?, maxPrecio: Double?): List<Producto> = withContext(Dispatchers.IO) {
        return@withContext dao.filtrarProductos(query.trim(), minPrecio, maxPrecio)
    }

    suspend fun actualizar(producto: Producto): Unit = withContext(Dispatchers.IO) {
        dao.actualizar(producto)
        coleccion.document(producto.id).set(producto).await()
    }

    suspend fun eliminar(producto: Producto): Unit = withContext(Dispatchers.IO) {
        dao.eliminar(producto)
        coleccion.document(producto.id).delete().await()
    }

    fun iniciarEscuchadorTiempoReal(onSincronizado: () -> Unit): ListenerRegistration {
        return coleccion.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val productosRemotos = snapshot.toObjects(Producto::class.java)
            CoroutineScope(Dispatchers.IO).launch {
                dao.insertarTodos(productosRemotos)
                withContext(Dispatchers.Main) {
                    onSincronizado()
                }
            }
        }
    }
}