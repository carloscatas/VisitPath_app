package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private var favoriteMonuments: MutableList<Monument> = mutableListOf()
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

        // Verificar si se recibieron favoritos correctamente
        if (favoriteMonuments.isEmpty()) {
            Toast.makeText(this, "No hay monumentos favoritos seleccionados.", Toast.LENGTH_SHORT).show()
        }

        // Inicializa el RecyclerView para mostrar la lista de favoritos
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configura el adaptador con la lista de favoritos
        favoritesAdapter = FavoritesAdapter(favoriteMonuments) { monumentToRemove ->
            // Eliminar de favoritos al pulsar la estrella
            favoriteMonuments.remove(monumentToRemove)
            favoritesAdapter.updateFavorites(favoriteMonuments)
        }
        recyclerView.adapter = favoritesAdapter

        // Configurar el botón de retorno para regresar a la actividad anterior
        val returnButton: ImageButton = findViewById(R.id.returnButton)
        returnButton.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putParcelableArrayListExtra("updatedFavorites", ArrayList(favoriteMonuments))
            setResult(RESULT_OK, resultIntent)
            finish() // Finalizar actividad y regresar
        }
    }

    override fun onBackPressed() {
        // Al presionar el botón de retroceso, devolver la lista actualizada de favoritos
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("updatedFavorites", ArrayList(favoriteMonuments))
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }
}
