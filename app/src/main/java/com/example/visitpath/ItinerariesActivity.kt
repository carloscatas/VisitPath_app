package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.GeoPoint

class ItinerariesActivity : AppCompatActivity() {

    private var userLocation: GeoPoint = GeoPoint(0.0, 0.0)
    private var transportType: String = "Caminando"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.itineraries_activity)

        // Recuperar ubicación del intent
        userLocation = GeoPoint(
            intent.getDoubleExtra("userLatitude", 0.0),
            intent.getDoubleExtra("userLongitude", 0.0)
        )

        // Recuperar el modo de transporte desde el intent
        transportType = intent.getStringExtra("transportType") ?: "Caminando"

        val itineraries = intent.getSerializableExtra("itineraries") as? List<List<Monument>>
            ?: emptyList()

        val itinerariesLayout: LinearLayout = findViewById(R.id.itinerariesLayout)

        for ((index, itinerary) in itineraries.withIndex()) {
            val button = Button(this) // Crear un nuevo botón
            button.text = "Día ${index + 1}"

            // Aplicar estilo personalizado con bordes redondeados
            button.setBackgroundResource(R.drawable.rounded_button)
            button.setTextColor(ContextCompat.getColor(this, R.color.white)) // Texto blanco
            button.textSize = 16f // Tamaño del texto
            button.setAllCaps(false) // Desactivar texto en mayúsculas

            // Configurar márgenes del botón
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 16, 16, 16) // Márgenes entre botones
            button.layoutParams = params

            // Establecer acción al hacer clic
            button.setOnClickListener {
                // Verificar si hay puntos para el día seleccionado
                if (itinerary.isNotEmpty()) {
                    val intent = Intent(this@ItinerariesActivity, RoutePreviewActivity::class.java)
                    intent.putParcelableArrayListExtra("route", ArrayList(itinerary))
                    intent.putExtra("userLatitude", userLocation.latitude)
                    intent.putExtra("userLongitude", userLocation.longitude)
                    intent.putExtra("transportMode", transportType)
                    startActivity(intent)
                } else {
                    // Mostrar mensaje de error si no hay puntos para el día
                    Toast.makeText(
                        this@ItinerariesActivity,
                        "No hay puntos para el día seleccionado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            itinerariesLayout.addView(button)
        }
    }
}
