package com.example.visitpath

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.GeoPoint

class ItinerariesActivity : AppCompatActivity() {

    private var userLocation: GeoPoint = GeoPoint(0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.itineraries_activity)

        // Recuperar ubicación del intent
        userLocation = GeoPoint(
            intent.getDoubleExtra("userLatitude", 0.0),
            intent.getDoubleExtra("userLongitude", 0.0)
        )

        val itineraries = intent.getSerializableExtra("itineraries") as? List<List<Monument>>
            ?: emptyList()

        val itinerariesLayout: LinearLayout = findViewById(R.id.itinerariesLayout)

        for ((index, itinerary) in itineraries.withIndex()) {
            val button = Button(this) // Crear un nuevo botón
            button.text = "Día ${index + 1}"
            button.setOnClickListener {
                // Verificar si hay puntos para el día seleccionado
                if (itinerary.isNotEmpty()) {
                    // Abrir la ruta en Google Maps
                    RoutePlanner().openRouteInGoogleMaps(
                        this@ItinerariesActivity,
                        userLocation,
                        itinerary,
                        "Caminando"
                    )
                } else {
                    // Mostrar mensaje de error si no hay puntos para el día
                    Toast.makeText(
                        this@ItinerariesActivity,
                        "No hay puntos para el día seleccionado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            itinerariesLayout.addView(button) // Añadir el botón al layout
        }
    }
}
