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
import android.content.Intent


class MonumentAdapter(private var monumentList: MutableList<Monument>) :
    RecyclerView.Adapter<MonumentAdapter.MonumentViewHolder>() {

    class MonumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.monumentName)
        val descripcionTextView: TextView = view.findViewById(R.id.monumentDescription)
        val categoriaTextView: TextView = view.findViewById(R.id.monumentCategory)
        val duracionTextView: TextView = view.findViewById(R.id.monumentDuration)
        val costoEntradaTextView: TextView = view.findViewById(R.id.monumentCost)
        val movilidadTextView: TextView = view.findViewById(R.id.monumentAccessibility)
        val audioguiaTextView: TextView = view.findViewById(R.id.monumentAudio)
        val monumentImageView: ImageView = view.findViewById(R.id.monumentImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monument, parent, false)
        return MonumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonumentViewHolder, position: Int) {
        val monument = monumentList[position]

        Glide.with(holder.itemView.context)
            .load(monument.imagenURL)
            .into(holder.monumentImageView)

        // Asignar textos
        holder.nombreTextView.text = monument.nombre
        holder.descripcionTextView.text = monument.descripcion
        holder.categoriaTextView.text = formatText("Categoría: ", monument.categoria)
        holder.duracionTextView.text = formatText("Duración de la visita: ", "${monument.duracionVisita} horas")

        holder.costoEntradaTextView.text = if (monument.costoEntrada) {
            formatText("Entrada: ", "Gratis")
        } else {
            formatText("Entrada: ", "De pago")
        }
        holder.movilidadTextView.text = if (monument.movilidadReducida) {
            formatText("Accesibilidad PMR: ", "Sí")
        } else {
            formatText("Accesibilidad PMR: ", "No")
        }
        holder.audioguiaTextView.text = "Audioguía: " + if (monument.audioURL.isNotEmpty()) "Sí" else "No"

        // Configurar el clic en el elemento para abrir MonumentDetailActivity
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MonumentDetailActivity::class.java)
            intent.putExtra("monument", monument) // Pasar el objeto Parcelable
            context.startActivity(intent)
        }
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

