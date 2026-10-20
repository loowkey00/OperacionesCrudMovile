package com.example.operacionescrud

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}

class InventarioRepository(
    private val dao: ProductoDao,
    private val apiService: FirestoreApiService = FirestoreApiService.instance,
) {
    private val dbFirestore = FirebaseFirestore.getInstance()
    private val coleccion = dbFirestore.collection("productos")

    private suspend inline fun <T> ejecutarSeguro(crossinline bloque: suspend () -> T): Resource<T> =
        withContext(Dispatchers.IO) {
            runCatching { bloque() }.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.localizedMessage ?: "Error de base de datos", it) },
            )
        }

    // 1. Operaciones de Base de Datos Local (Room) encapsuladas con runCatching + Resource<T>
    suspend fun insertar(producto: Producto): Resource<Unit> = ejecutarSeguro {
        dao.insertar(producto)
    }

    suspend fun insertarTodos(productos: List<Producto>): Resource<Unit> = ejecutarSeguro {
        dao.insertarTodos(productos)
    }

    suspend fun obtenerTodos(): Resource<List<Producto>> = ejecutarSeguro {
        dao.obtenerTodos()
    }

    suspend fun buscar(query: String): Resource<List<Producto>> = ejecutarSeguro {
        if (query.isBlank()) dao.obtenerTodos() else dao.buscar(query)
    }

    suspend fun filtrar(query: String, minPrecio: Double?, maxPrecio: Double?): Resource<List<Producto>> =
        ejecutarSeguro {
            dao.filtrarProductos(query.trim(), minPrecio, maxPrecio)
        }

    // 2. Operaciones CRUD Remotas (Firestore SDK) + Local (Room)
    suspend fun crear(producto: Producto): Resource<Unit> = ejecutarSeguro {
        dao.insertar(producto)
        coleccion.document(producto.id).set(producto).await()
    }

    suspend fun actualizar(producto: Producto): Resource<Unit> = ejecutarSeguro {
        dao.actualizar(producto)
        coleccion.document(producto.id).set(producto).await()
    }

    suspend fun eliminar(producto: Producto): Resource<Unit> = ejecutarSeguro {
        dao.eliminar(producto)
        coleccion.document(producto.id).delete().await()
    }

    fun observarCambiosNube(onSincronizado: () -> Unit = {}): ListenerRegistration {
        return coleccion.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val productosRemotos = snapshot.toObjects(Producto::class.java)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { dao.insertarTodos(productosRemotos) }
                withContext(Dispatchers.Main) {
                    onSincronizado()
                }
            }
        }
    }

    // 3. Interoperabilidad REST con Retrofit (sin Firebase SDK)
    suspend fun obtenerProductosRest(): List<Producto> = withContext(Dispatchers.IO) {
        apiService.obtenerProductos().documents.orEmpty().map { it.toProducto() }
    }
}
