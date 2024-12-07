package com.example.visitpath

import android.content.Intent
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
            null    // Si no se obtuvo correctamente, `userLocation` será `null`
        }

        if (userLocation == null) {
            Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        // Obtener los monumentos restantes desde el Intent
        remainingMonuments = intent.getParcelableArrayListExtra("remainingMonuments") ?: mutableListOf()
        favoriteMonuments = intent.getParcelableArrayListExtra("favoriteMonuments") ?: mutableListOf()
        filteredMonuments = intent.getParcelableArrayListExtra("filteredMonuments") ?: mutableListOf()

        // Obtener `remainingTime` desde el Intent
        val remainingTime = intent.getDoubleExtra("remainingTime", -1.0)
        if (remainingTime == -1.0) {
            Toast.makeText(this, "No se pudo obtener el tiempo restante.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // Añadir Logs para verificar la recepción de los datos
        Log.d("OptimizationActivity", "Tiempo restante disponible recibido: $remainingTime horas")

        // Añadir Logs para verificar la recepción de los datos
        Log.d("OptimizationActivity", "Monumentos favoritos recibidos: ${favoriteMonuments.size}")
        favoriteMonuments.forEach {
            Log.d("OptimizationActivity", "- ${it.nombre}")
        }

        Log.d("OptimizationActivity", "Monumentos filtrados recibidos: ${filteredMonuments.size}")
        filteredMonuments.forEach {
            Log.d("OptimizationActivity", "- ${it.nombre}")
        }

        Log.d("OptimizationActivity", "Monumentos restantes recibidos: ${remainingMonuments.size}")
        remainingMonuments.forEach {
            Log.d("OptimizationActivity", "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}")
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

        // Config botón "Aceptar"
        btnAccept = findViewById(R.id.btnAccept)
        // OptimizationActivity.kt
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

            // Generar la ruta inicial con favoritos y filtrados ya seleccionados
            val initialRoute  = routePlanner.generateFinalRoute(
                favoriteMonuments = favoriteMonuments,
                filteredMonuments = filteredMonuments,
                remainingMonuments = emptyList(),
                userLocation = userLocation!!,
                transportType = transportType
            ).toMutableList() // Convertir a MutableList para añadir monumentos adicionales si es necesario

            // Verificar si la ruta inicial tiene monumentos
            if (initialRoute.isEmpty()) {
                Log.d("OptimizationActivity", "La ruta inicial generada está vacía (favoritos + filtrados).")
            } else {
                Log.d("OptimizationActivity", "Ruta inicial generada (favoritos + filtrados), total de puntos: ${initialRoute.size}")
                initialRoute.forEach { monument ->
                    Log.d("OptimizationActivity", "- Monumento: ${monument.nombre}, Duración: ${monument.duracionVisita}")
                }
            }

            // Actualizar `remainingMonuments` excluyendo los monumentos ya añadidos a la ruta inicial
            val updatedRemainingMonuments = finalRemainingMonuments.filterNot { monument ->
                initialRoute.any { it.nombre == monument.nombre }
            }.toMutableList()

            // Calcular el tiempo restante
            val remainingTime = intent.getDoubleExtra("remainingTime", 1.0)
            val selectedTime = intent.getIntExtra("selectedTime", 1)

            // Inicialmente, el tiempo ya usado es lo que implica la ruta generada hasta ahora (favoritos + filtrados)
            var currentTime = 0.0
            var currentLocation = userLocation!!

            // Log para verificar el tiempo restante y acumulado inicialmente
            Log.d("OptimizationActivity", "Tiempo inicial acumulado tras favoritos y filtrados: $currentTime h")
            Log.d("OptimizationActivity", "Tiempo restante disponible: $remainingTime h")

            // Añadir los monumentos restantes si hay tiempo disponible
            if (remainingTime>0) {
                Log.d("OptimizationActivity", "Intentando añadir monumentos restantes...")

                val orderedRemainingMonuments = routePlanner.optimizeRouteOrder(currentLocation, finalRemainingMonuments)
                var index = 0

                while (index < orderedRemainingMonuments.size && currentTime < selectedTime && initialRoute.size < 10) {
                    val remainingMonument = orderedRemainingMonuments[index]

                    val travelTime = routePlanner.calculateTravelTime(
                        routePlanner.getDistanceBetweenPoints(currentLocation, GeoPoint(remainingMonument.latitud, remainingMonument.longitud)),
                        transportType
                    )
                    val visitTime = if (intent.getStringExtra("visitType") == "Tour rápido") 0.5 else remainingMonument.duracionVisita
                    val totalTimeNeeded = travelTime + visitTime

                    Log.d("OptimizationActivity", "Evaluando añadir: ${remainingMonument.nombre}, Tiempo de viaje: $travelTime h, Tiempo de visita: $visitTime h, Tiempo total requerido para este punto: $totalTimeNeeded h")

                    // Verificar si al añadir este monumento, excederemos el tiempo restante disponible
                    if (currentTime + totalTimeNeeded <= remainingTime ) {
                        initialRoute.add(remainingMonument)
                        currentTime += totalTimeNeeded
                        currentLocation = GeoPoint(remainingMonument.latitud, remainingMonument.longitud)

                        Log.d("OptimizationActivity", "Añadido a la ruta: ${remainingMonument.nombre}, Tiempo acumulado ahora: $currentTime h")
                    } else {
                        Log.d("OptimizationActivity", "No se añadió ${remainingMonument.nombre} porque excede el tiempo disponible.")
                    }

                    index++
                }
            }
            val optimizedRoute = routePlanner.optimizeRouteOrder(userLocation!!, initialRoute)


            // Abrir la ruta final
            if (optimizedRoute.isNotEmpty()) {
                Log.d("OptimizationActivity", "Ruta final generada, lista para ser mostrada en Google Maps:")
                initialRoute.forEach { monument ->
                    Log.d("OptimizationActivity", "- ${monument.nombre}, Latitud: ${monument.latitud}, Longitud: ${monument.longitud}")
                }

                val intent = Intent(this, RoutePreviewActivity::class.java)
                intent.putParcelableArrayListExtra("route", ArrayList(optimizedRoute))
                intent.putExtra("userLatitude", userLocation!!.latitude)
                intent.putExtra("userLongitude", userLocation!!.longitude)
                intent.putExtra("transportMode", transportType)
                startActivity(intent)


            } else {
                Toast.makeText(this, "No se pudo generar una ruta viable.", Toast.LENGTH_SHORT).show()
                Log.d("OptimizationActivity", "No se pudo generar una ruta viable.")
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

