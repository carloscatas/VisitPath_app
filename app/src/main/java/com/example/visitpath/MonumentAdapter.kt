package com.example.visitpath

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MonumentAdapter(private var monumentList: MutableList<Monument>) :
    RecyclerView.Adapter<MonumentAdapter.MonumentViewHolder>() {

    class MonumentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.monumentName)
        val descripcionTextView: TextView = view.findViewById(R.id.monumentDescription)
        val categoriaTextView: TextView = view.findViewById(R.id.monumentCategory)
        val duracionTextView: TextView = view.findViewById(R.id.monumentDuration)
        val costoEntradaTextView: TextView = view.findViewById(R.id.monumentCost)
        val movilidadTextView: TextView = view.findViewById(R.id.monumentAccessibility)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monument, parent, false)
        return MonumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonumentViewHolder, position: Int) {
        val monument = monumentList[position]
        holder.nombreTextView.text = monument.nombre
        holder.descripcionTextView.text = monument.descripcion
        holder.categoriaTextView.text = monument.categoria
        holder.duracionTextView.text = "${monument.duracionVisita} horas"
        holder.costoEntradaTextView.text = if (monument.costoEntrada) "Gratis" else "De pago"
        holder.movilidadTextView.text = if (monument.movilidadReducida) "Accesible" else "No accesible"
    }

    override fun getItemCount(): Int = monumentList.size

    fun updateData(newMonuments: MutableList<Monument>) {
        monumentList.clear()
        monumentList.addAll(newMonuments)
        notifyDataSetChanged()
    }
}
