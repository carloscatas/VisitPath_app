package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.GeoPoint

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private var favoriteMonuments: MutableList<Monument> = mutableListOf()
    private var filteredMonuments: MutableList<Monument> = mutableListOf() // Almacena la lista completa de monumentos filtrados
    private var userLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Recibir la ubicación del usuario desde MonumentListActivity
        val userLatitude = intent.getDoubleExtra("userLatitude", Double.NaN)
        val userLongitude = intent.getDoubleExtra("userLongitude", Double.NaN)

        userLocation = if (!userLatitude.isNaN() && !userLongitude.isNaN()) {
            GeoPoint(userLatitude, userLongitude)
        } else {
            null // Ubicación no proporcionada
        }

        // Recibir los favoritos desde la actividad principal
        favoriteMonuments = intent.getParcelableArrayListExtra<Monument>("favorites") ?: mutableListOf()

        // Recibir la lista completa de monumentos desde la actividad principal
        filteredMonuments = intent.getParcelableArrayListExtra<Monument>("filteredMonuments") ?: mutableListOf()

        // Inicializa el RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configura el adaptador con la lista de favoritos
        favoritesAdapter = FavoritesAdapter(favoriteMonuments) { monumentToRemove ->
            // Eliminar de favoritos al pulsar la estrella
            favoriteMonuments.remove(monumentToRemove)
            favoritesAdapter.updateFavorites(favoriteMonuments)
        }
        recyclerView.adapter = favoritesAdapter

        // En el botón de retorno en FavoritesActivity
        val returnButton: ImageButton = findViewById(R.id.returnButton)
        returnButton.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putParcelableArrayListExtra("updatedFavorites", ArrayList(favoriteMonuments))
            setResult(RESULT_OK, resultIntent)
            finish() // Finalizar actividad y regresar
        }

        // Configurar el botón de generación de rutas
        val fabCreateRoute: FloatingActionButton = findViewById(R.id.fab_create_route)
        fabCreateRoute.setOnClickListener {
            if (favoriteMonuments.isEmpty()) {
                Toast.makeText(this, "No hay favoritos seleccionados para generar una ruta.", Toast.LENGTH_SHORT).show()
            } else if (userLocation == null) {
                Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT).show()
            } else {
                // Llamar a la actividad de configuración de rutas
                val intent = Intent(this, RouteConfigActivity::class.java)
                intent.putExtra("userLatitude", userLocation!!.latitude)
                intent.putExtra("userLongitude", userLocation!!.longitude)
                startActivityForResult(intent, RoutePlanner.ROUTE_CONFIG_REQUEST_CODE)
            }
        }
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("updatedFavorites", ArrayList(favoriteMonuments))
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RoutePlanner.ROUTE_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedTime = data?.getIntExtra("selectedTime", 1) ?: 1
            val visitType = data?.getStringExtra("visitType") ?: "Tour rápido"
            val transportType = data?.getStringExtra("transportType") ?: "Caminando"

            if (userLocation == null) {
                Toast.makeText(this, "No se pudo obtener la ubicación del usuario.", Toast.LENGTH_SHORT).show()
                return
            }

            val routePlanner = RoutePlanner()
            val (viableRoute, remainingMonuments, currentTime) = routePlanner.handleRouteGeneration(
                allMonuments = filteredMonuments,
                favoriteMonuments = favoriteMonuments,
                userLocation = userLocation!!,
                selectedTime = selectedTime,
                visitType = visitType,
                transportType = transportType
            )

            if (viableRoute.isEmpty()) {
                Toast.makeText(this, "No es posible generar una ruta con el tiempo seleccionado.", Toast.LENGTH_LONG).show()
                return
            }

            // Verificar tiempo extra
            if ((currentTime + 1) <= selectedTime.toDouble() && remainingMonuments.isNotEmpty()) {
                routePlanner.checkForExtraTimeAndShowDialog(
                    context = this,
                    currentTime = currentTime,
                    availableTime = selectedTime.toDouble(),
                    route = viableRoute,
                    remainingMonuments = remainingMonuments,
                    visitType = visitType,
                    transportType = transportType,
                    currentLocation = GeoPoint(viableRoute.last().latitud, viableRoute.last().longitud)
                )
            } else {
                routePlanner.openRouteInGoogleMaps(this, userLocation!!, viableRoute, transportType)
            }
        }
    }


}
