package com.example.visitpath

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint

class OptimizationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private var remainingMonuments: MutableList<Monument> = mutableListOf()
    private var userLocation: GeoPoint? = null
    private var transportType: String = "Caminando"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_optimization)

        // Obtener la latitud y longitud desde el Intent
        val userLatitude = intent.getDoubleExtra("userLatitude", Double.NaN)
        val userLongitude = intent.getDoubleExtra("userLongitude", Double.NaN)

        val userLocation: GeoPoint? = if (!userLatitude.isNaN() && !userLongitude.isNaN()) {
            GeoPoint(userLatitude, userLongitude)
        } else {
            null // Si no se obtuvo correctamente, `userLocation` será `null`
        }

        if (userLocation == null) {
            Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener los monumentos restantes desde el Intent
        val remainingMonuments = intent.getParcelableArrayListExtra<Monument>("remainingMonuments") ?: mutableListOf()
        Log.d("OptimizationActivity", "Monumentos restantes recibidos: ${remainingMonuments.size}")

        remainingMonuments.forEach {
            Log.d("OptimizationActivity", "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}")
        }

        // Verificar si hay monumentos disponibles
        if (remainingMonuments.isEmpty()) {
            Toast.makeText(this, "No hay monumentos adicionales para mostrar.", Toast.LENGTH_SHORT).show()
            finish() // Cerrar la actividad si no hay monumentos para mostrar
            return
        }


        // Obtener el tipo de transporte y los monumentos restantes
        val transportType = intent.getStringExtra("transportType") ?: "Caminando"

        // Filtrar monumentos según la distancia
        val maxDistance = when (transportType) {
            "Caminando" -> 5.0 // Máximo 5 km
            "Público" -> 15.0 // Máximo 15 km
            "Privado" -> 50.0 // Máximo 50 km
            else -> 5.0
        }

        val nearbyMonuments = remainingMonuments.filter { monument ->
            calculateDistance(userLocation, GeoPoint(monument.latitud, monument.longitud)) <= maxDistance
        }.toMutableList()

        // Log para verificar los monumentos cercanos después del filtro
        Log.d("OptimizationActivity", "Monumentos cercanos después del filtro: ${nearbyMonuments.size}")

        // Configurar RecyclerView para mostrar los monumentos cercanos
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val monumentAdapter = MonumentAdapter(nearbyMonuments) { selectedMonument ->
            // Acción al seleccionar un monumento, si es necesario
        }
        recyclerView.adapter = monumentAdapter
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
