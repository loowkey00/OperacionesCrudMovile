package com.example.operacionescrud

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var repository: InventarioRepository
    private var productoSeleccionado: Producto? = null
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Respeta la barra de estado, la de navegación y el teclado
        val header = findViewById<View>(R.id.header)
        val svMain = findViewById<ScrollView>(R.id.svMain)
        val headerPadTop = header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            header.setPadding(
                header.paddingLeft,
                headerPadTop + bars.top,
                header.paddingRight,
                header.paddingBottom,
            )
            svMain.setPadding(0, 0, 0, maxOf(bars.bottom, ime.bottom))
            insets
        }

        val dao = InventarioDatabase.getDatabase(this).productoDao()
        repository = InventarioRepository(dao)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val etCantidad = findViewById<EditText>(R.id.etCantidad)
        val etPrecio = findViewById<EditText>(R.id.etPrecio)
        val etProveedor = findViewById<EditText>(R.id.etProveedor)
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        val etPrecioMin = findViewById<EditText>(R.id.etPrecioMin)
        val etPrecioMax = findViewById<EditText>(R.id.etPrecioMax)
        val btnLimpiarFiltro = findViewById<Button>(R.id.btnLimpiarFiltro)
        val btnCrear = findViewById<Button>(R.id.btnCrear)
        val btnActualizar = findViewById<Button>(R.id.btnActualizar)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)
        val containerInventario = findViewById<LinearLayout>(R.id.containerInventario)
        val tvFormTitulo = findViewById<TextView>(R.id.tvFormTitulo)
        val tvContador = findViewById<TextView>(R.id.tvContador)

        fun marcarSeleccion() {
            val idSel = productoSeleccionado?.id
            for (i in 0 until containerInventario.childCount) {
                val v = containerInventario.getChildAt(i)
                v.isActivated = idSel != null && v.tag == idSel
            }
            tvFormTitulo.text = productoSeleccionado?.let {
                getString(R.string.titulo_editando, it.nombre)
            } ?: getString(R.string.titulo_nuevo)
        }

        fun mostrarProductos(productos: List<Producto>) {
            containerInventario.removeAllViews()
            tvContador.text = getString(R.string.contador_productos, productos.size)
            if (productos.isEmpty()) {
                val tvVacio = TextView(this@MainActivity).apply {
                    text = getString(R.string.text_sin_resultados)
                    textSize = 15f
                    setTextColor(getColor(R.color.text_secondary))
                    setPadding(16, 24, 16, 24)
                    gravity = Gravity.CENTER
                }
                containerInventario.addView(tvVacio)
            } else {
                val inflater = LayoutInflater.from(this@MainActivity)
                productos.forEach { p ->
                    val itemView = inflater.inflate(R.layout.item_producto, containerInventario, false)
                    itemView.tag = p.id
                    itemView.findViewById<TextView>(R.id.tvItemNombre).text = p.nombre
                    itemView.findViewById<TextView>(R.id.tvItemPrecio).text =
                        getString(R.string.item_precio, p.precio)
                    itemView.findViewById<TextView>(R.id.tvItemDescripcion).text =
                        p.descripcion.ifBlank { getString(R.string.item_sin_descripcion) }
                    itemView.findViewById<TextView>(R.id.tvItemCantidad).text =
                        getString(R.string.item_cantidad, p.cantidad)
                    itemView.findViewById<TextView>(R.id.tvItemProveedor).text =
                        if (p.proveedor.isBlank()) getString(R.string.item_sin_proveedor)
                        else getString(R.string.item_proveedor, p.proveedor)

                    itemView.setOnClickListener {
                        productoSeleccionado = p
                        etNombre.setText(p.nombre)
                        etDescripcion.setText(p.descripcion)
                        etCantidad.setText(p.cantidad.toString())
                        etPrecio.setText(p.precio.toString())
                        etProveedor.setText(p.proveedor)
                        marcarSeleccion()
                        svMain.smoothScrollTo(0, 0)
                        Toast.makeText(this@MainActivity, "Seleccionado: ${p.nombre}", Toast.LENGTH_SHORT).show()
                    }
                    containerInventario.addView(itemView)
                }
                marcarSeleccion()
            }
        }

        fun cargarInventario() {
            val query = etBuscar.text.toString().trim()
            val minPrecio = etPrecioMin.text.toString().trim().toDoubleOrNull()
            val maxPrecio = etPrecioMax.text.toString().trim().toDoubleOrNull()

            lifecycleScope.launch {
                when (val res = repository.filtrar(query, minPrecio, maxPrecio)) {
                    is Resource.Success -> mostrarProductos(res.data)
                    is Resource.Error -> Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun obtenerProductoFromInputs(): Producto? {
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val cantidadStr = etCantidad.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val proveedor = etProveedor.text.toString().trim()

            if (nombre.isEmpty() || cantidadStr.isEmpty() || precioStr.isEmpty()) {
                Toast.makeText(this, "Completa nombre, cantidad y precio", Toast.LENGTH_SHORT).show()
                return null
            }

            val cantidad = cantidadStr.toIntOrNull() ?: 0
            val precio = precioStr.toDoubleOrNull() ?: 0.0

            return Producto(
                id = productoSeleccionado?.id ?: UUID.randomUUID().toString(),
                nombre = nombre,
                descripcion = descripcion,
                cantidad = cantidad,
                precio = precio,
                proveedor = proveedor,
            )
        }

        fun limpiarCampos() {
            etNombre.text.clear()
            etDescripcion.text.clear()
            etCantidad.text.clear()
            etPrecio.text.clear()
            etProveedor.text.clear()
            productoSeleccionado = null
            marcarSeleccion()
        }

        val filtroWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarInventario()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etBuscar.addTextChangedListener(filtroWatcher)
        etPrecioMin.addTextChangedListener(filtroWatcher)
        etPrecioMax.addTextChangedListener(filtroWatcher)

        btnLimpiarFiltro.setOnClickListener {
            etBuscar.text.clear()
            etPrecioMin.text.clear()
            etPrecioMax.text.clear()
            cargarInventario()
        }

        btnCrear.setOnClickListener {
            val producto = obtenerProductoFromInputs() ?: return@setOnClickListener
            lifecycleScope.launch {
                when (val res = repository.crear(producto)) {
                    is Resource.Success -> {
                        limpiarCampos()
                        cargarInventario()
                        Toast.makeText(this@MainActivity, "Producto creado", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnActualizar.setOnClickListener {
            val producto = obtenerProductoFromInputs() ?: return@setOnClickListener
            lifecycleScope.launch {
                when (val res = repository.actualizar(producto)) {
                    is Resource.Success -> {
                        limpiarCampos()
                        cargarInventario()
                        Toast.makeText(this@MainActivity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnEliminar.setOnClickListener {
            val producto = productoSeleccionado ?: obtenerProductoFromInputs() ?: return@setOnClickListener
            lifecycleScope.launch {
                when (val res = repository.eliminar(producto)) {
                    is Resource.Success -> {
                        limpiarCampos()
                        cargarInventario()
                        Toast.makeText(this@MainActivity, "Producto eliminado", Toast.LENGTH_SHORT).show()
                    }
                    is Resource.Error -> Toast.makeText(this@MainActivity, res.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        cargarInventario()

        // Sincronización en tiempo real desde Cloud Firestore para múltiples usuarios
        firestoreListener = repository.observarCambiosNube {
            cargarInventario()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }
}
