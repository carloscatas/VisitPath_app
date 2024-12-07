package com.example.visitpath

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.GeoPoint

class RoutePreviewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private var route: MutableList<Monument> = mutableListOf()
    private var userLocation: GeoPoint? = null
    private var transportMode: String = "walking"
    private lateinit var confirmButton: MaterialButton
    private lateinit var returnButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_preview)

        // Obtener los datos de la ruta desde el Intent
        route = intent.getParcelableArrayListExtra("route") ?: mutableListOf()
        userLocation = intent.getParcelableExtra("userLocation")
        transportMode = intent.getStringExtra("transportMode") ?: "walking"

        val userLatitude = intent.getDoubleExtra("userLatitude", Double.NaN)
        val userLongitude = intent.getDoubleExtra("userLongitude", Double.NaN)

        if (!userLatitude.isNaN() && !userLongitude.isNaN()) {
            userLocation = GeoPoint(userLatitude, userLongitude)
        } else {
            userLocation = null // Manejo de error
        }

        // Verificar que los datos son válidos
        if (userLocation == null) {
            Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (route.isEmpty()) {
            Toast.makeText(this, "La ruta no contiene puntos para mostrar.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configurar el adaptador para manejar la eliminación de monumentos
        monumentAdapter = MonumentAdapter(route, { selectedMonument ->
            // Eliminar el monumento seleccionado de la lista
            route.remove(selectedMonument)
            monumentAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Punto eliminado: ${selectedMonument.nombre}", Toast.LENGTH_SHORT).show()

            // Verificar si la lista está vacía después de eliminar
            if (route.isEmpty()) {
                Toast.makeText(this, "La ruta quedó vacía. Regresando a la pantalla anterior.", Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad si la lista está vacía
            }
        }, showDeleteIcon = true)

        recyclerView.adapter = monumentAdapter

        // Configurar el botón flotante
        confirmButton = findViewById(R.id.confirmRouteButton)
        confirmButton.setOnClickListener {
            if (route.isEmpty()) {
                Toast.makeText(this, "No hay puntos en la ruta para abrir en Google Maps.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Llamar directamente a RoutePlanner para abrir Google Maps
            openRouteInGoogleMaps()
        }

    }

    /**
     * Usa RoutePlanner para abrir la ruta optimizada en Google Maps.
     */
    private fun openRouteInGoogleMaps() {
        val routePlanner = RoutePlanner()
        routePlanner.openRouteInGoogleMaps(
            context = this,
            userLocation = userLocation!!,
            route = route,
            transportMode = transportMode
        )
    }
}
