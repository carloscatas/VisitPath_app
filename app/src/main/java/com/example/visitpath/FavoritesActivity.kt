package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoritesAdapter
    private var favoriteMonuments: MutableList<Monument> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Recibir los favoritos desde la actividad principal
        favoriteMonuments = intent.getParcelableArrayListExtra<Monument>("favorites") ?: mutableListOf()

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

    }
    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putParcelableArrayListExtra("updatedFavorites", ArrayList(favoriteMonuments))
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }

}
