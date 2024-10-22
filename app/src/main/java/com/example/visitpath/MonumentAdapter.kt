package com.example.visitpath

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class MonumentAdapter(private var monumentList: MutableList<Monument>) :
    RecyclerView.Adapter<MonumentAdapter.MonumentViewHolder>() {

    class MonumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.monumentName)
        val categoriaTextView: TextView = view.findViewById(R.id.monumentCategory)
        val duracionTextView: TextView = view.findViewById(R.id.monumentDuration)
        val costoEntradaTextView: TextView = view.findViewById(R.id.monumentCost)
        val movilidadTextView: TextView = view.findViewById(R.id.monumentAccessibility)
        val monumentImageView: ImageView = view.findViewById(R.id.monumentImage) // Aquí vinculamos la imagen
        val audioguiaTextView: TextView = view.findViewById(R.id.monumentAudio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monument, parent, false)
        return MonumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonumentViewHolder, position: Int) {
        val monument = monumentList[position]

        Glide.with(holder.itemView.context)
            .load(monument.imagenURL)  // Aquí usarías el campo imagenURL del monumento
            .into(holder.monumentImageView)

        // Nombre del monumento (negrita por defecto en el TextView)
        holder.nombreTextView.text = monument.nombre

        // Crear el texto para Categoría
        holder.categoriaTextView.text = formatText("Categoría: ", monument.categoria)

        // Crear el texto para Duración de la visita
        holder.duracionTextView.text = formatText("Duración de la visita: ", "${monument.duracionVisita} horas")

        // Crear el texto para Costo de entrada
        holder.costoEntradaTextView.text = if (monument.costoEntrada) {
            formatText("Entrada: ", "Gratis")
        } else {
            formatText("Entrada: ", "De pago")
        }

        // Crear el texto para Accesibilidad
        holder.movilidadTextView.text = if (monument.movilidadReducida) {
            formatText("Accesibilidad PMR: ", "Sí")
        } else {
            formatText("Accesibilidad PMR: ", "No")
        }
        // Audioguía disponible
        holder.audioguiaTextView.text = "Audioguía: " + if (monument.audioURL.isNotEmpty()) "Sí" else "No"

    }



    // Función para aplicar negrita solo al nombre del campo y no al valor
    private fun formatText(label: String, value: String): SpannableString {
        val spannableString = SpannableString(label + value)
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD), // Negrita
            0, label.length, // Desde el inicio hasta el final del "label"
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }


    override fun getItemCount(): Int = monumentList.size

    fun updateData(newMonuments: MutableList<Monument>) {
        monumentList.clear()
        monumentList.addAll(newMonuments)
        notifyDataSetChanged()
    }
}
