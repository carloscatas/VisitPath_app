package com.example.visitpath

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint

class OptimizationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private var remainingMonuments: MutableList<Monument> = mutableListOf()
    private var favoriteMonuments: MutableList<Monument> = mutableListOf()
    private var filteredMonuments: MutableList<Monument> = mutableListOf()
    private var userLocation: GeoPoint? = null
    private var transportType: String = "Caminando"
    private lateinit var btnAccept: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_optimization)

        // Obtener la latitud y longitud desde el Intent
        val userLatitude = intent.getDoubleExtra("userLatitude", Double.NaN)
        val userLongitude = intent.getDoubleExtra("userLongitude", Double.NaN)

        userLocation = if (!userLatitude.isNaN() && !userLongitude.isNaN()) {
            GeoPoint(userLatitude, userLongitude)
        } else {
            null // Si no se obtuvo correctamente, `userLocation` será `null`
        }

        if (userLocation == null) {
            Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        // Obtener los monumentos restantes desde el Intent
        // Obtener los monumentos restantes desde el Intent
        remainingMonuments = intent.getParcelableArrayListExtra("remainingMonuments") ?: mutableListOf()
        favoriteMonuments = intent.getParcelableArrayListExtra("favoriteMonuments") ?: mutableListOf()
        filteredMonuments = intent.getParcelableArrayListExtra("filteredMonuments") ?: mutableListOf()

        remainingMonuments.forEach {
            Log.d(
                "OptimizationActivity",
                "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}"
            )
        }

        // Verificar si hay monumentos disponibles
        if (remainingMonuments.isEmpty()) {
            Toast.makeText(this, "No hay monumentos adicionales para mostrar.", Toast.LENGTH_SHORT)
                .show()
            finish() // Cerrar la actividad si no hay monumentos para mostrar
            return
        }

        // Obtener el tipo de transporte y los monumentos restantes
        transportType = intent.getStringExtra("transportType") ?: "Caminando"

        // Filtrar monumentos según la distancia
        val maxDistance = when (transportType) {
            "Caminando" -> 5.0 // Máximo 5 km
            "Público" -> 15.0 // Máximo 15 km
            "Privado" -> 50.0 // Máximo 50 km
            else -> 5.0
        }

        val nearbyMonuments = remainingMonuments.filter { monument ->
            calculateDistance(
                userLocation!!,
                GeoPoint(monument.latitud, monument.longitud)
            ) <= maxDistance
        }.toMutableList()

        // Log para verificar los monumentos cercanos después del filtro
        Log.d(
            "OptimizationActivity",
            "Monumentos cercanos después del filtro: ${nearbyMonuments.size}"
        )

        if (nearbyMonuments.isEmpty()) {
            Toast.makeText(
                this,
                "No hay monumentos dentro del rango seleccionado.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Configurar RecyclerView para mostrar los monumentos cercanos
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Usar la lista filtrada `nearbyMonuments` en lugar de `remainingMonuments`
        monumentAdapter = MonumentAdapter(nearbyMonuments, { selectedMonument ->
            // Eliminar el monumento seleccionado de la lista
            nearbyMonuments.remove(selectedMonument)
            monumentAdapter.notifyDataSetChanged()
            Toast.makeText(
                this,
                "${selectedMonument.nombre} eliminado de la lista de optimización.",
                Toast.LENGTH_SHORT
            ).show()
        }, showDeleteIcon = true)

        recyclerView.adapter = monumentAdapter

        // Configurar el botón "Aceptar"
        btnAccept = findViewById(R.id.btnAccept)
        btnAccept.setOnClickListener {
            if (userLocation == null) {
                Toast.makeText(
                    this,
                    "No se pudo obtener la ubicación del usuario.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Obtener la lista de monumentos restantes (optimizada por el usuario)
            val finalRemainingMonuments = monumentAdapter.getCurrentMonumentList()

            // Llamar a RoutePlanner para generar la ruta definitiva con favoritos + filtrados + restantes
            val routePlanner = RoutePlanner()

            val finalRoute = routePlanner.generateRoute(
                allMonuments = finalRemainingMonuments, // Utilizamos los restantes de la optimización como base
                favoriteMonuments = favoriteMonuments, // Los favoritos seleccionados
                selectedTime = 10, // Asumimos un máximo de 10 horas para la ruta
                visitType = "Tour completo", // Puedes cambiar a "Tour rápido" si es necesario
                transportType = transportType,
                userLocation = userLocation!!,
                filteredMonuments = filteredMonuments // Los filtrados que quedan por visitar
            )

            // Mostrar la ruta en Google Maps
            if (finalRoute.isNotEmpty()) {
                routePlanner.openRouteInGoogleMaps(
                    context = this,
                    userLocation = userLocation!!,
                    route = finalRoute.first(),
                    transportMode = transportType
                )
            } else {
                Toast.makeText(this, "No se pudo generar una ruta viable.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

        // Función para calcular la distancia entre dos puntos
        private fun calculateDistance(userLocation: GeoPoint, monumentLocation: GeoPoint): Double {
            val earthRadius = 6371
            val dLat = Math.toRadians(monumentLocation.latitude - userLocation.latitude)
            val dLng = Math.toRadians(monumentLocation.longitude - userLocation.longitude)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(userLocation.latitude)) *
                    Math.cos(Math.toRadians(monumentLocation.latitude)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return earthRadius * c
        }
    }

