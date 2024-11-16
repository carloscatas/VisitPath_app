package com.example.visitpath

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FavoritesAdapter(
    private var favoriteList: MutableList<Monument>,
    private val onRemoveFavorite: (Monument) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val starIcon: ImageView = view.findViewById(R.id.starIcon)
        val monumentImageView: ImageView = view.findViewById(R.id.monumentImage)
        val monumentName: TextView = view.findViewById(R.id.monumentName)
        val monumentDescription: TextView = view.findViewById(R.id.monumentDescription)
        val monumentCategory: TextView = view.findViewById(R.id.monumentCategory)
        val monumentDuration: TextView = view.findViewById(R.id.monumentDuration)
        val monumentCost: TextView = view.findViewById(R.id.monumentCost)
        val monumentAccessibility: TextView = view.findViewById(R.id.monumentAccessibility)
        val monumentAudio: TextView = view.findViewById(R.id.monumentAudio)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_monument, parent, false)
        return FavoritesViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val monument = favoriteList[position]

        // Configura los datos del monumento
        Glide.with(holder.itemView.context)
            .load(monument.imagenURL)
            .into(holder.monumentImageView)

        // Configura los datos del monumento
        holder.monumentName.text = monument.nombre
        holder.monumentDescription.text = monument.descripcion
        holder.monumentCategory.text = "Categoría: ${monument.categoria}"
        holder.monumentDuration.text = "Duración de la visita: ${monument.duracionVisita} horas"
        holder.monumentCost.text = "Entrada: ${if (monument.costoEntrada) "Gratis" else "De pago"}"
        holder.monumentAccessibility.text = "Accesibilidad PMR: ${if (monument.movilidadReducida) "Sí" else "No"}"
        holder.monumentAudio.text = "Audioguía: ${if (monument.audioURL.isNotEmpty()) "Sí" else "No"}"

        // Establece la estrella como seleccionada
        holder.starIcon.setImageResource(R.drawable.ic_full_star)
        holder.starIcon.setColorFilter(android.graphics.Color.YELLOW)

        // Configura el clic en la estrella para eliminar de favoritos
        holder.starIcon.setOnClickListener {
            onRemoveFavorite(monument)
        }
    }

    override fun getItemCount(): Int = favoriteList.size

    // Método para actualizar la lista de favoritos
    fun updateFavorites(newFavorites: MutableList<Monument>) {
        favoriteList = newFavorites
        notifyDataSetChanged()
    }
}
