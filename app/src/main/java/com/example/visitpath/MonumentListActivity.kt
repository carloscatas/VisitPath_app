package com.example.visitpath

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class MonumentListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_list)

        Log.d("ActivityLifecycle", "MonumentListActivity started")

        // Configurar el RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        monumentAdapter = MonumentAdapter(mutableListOf())
        recyclerView.adapter = monumentAdapter

        // Llamada a Firestore para obtener los monumentos
        fetchMonumentsFromFirestore()
    }

    private fun fetchMonumentsFromFirestore() {
        db.collection("monumentos")
            .get()
            .addOnSuccessListener { result ->
                val monumentList = mutableListOf<Monument>()
                for (document in result) {
                    val data = document.data
                    // Verifica si 'ubicacion' es un GeoPoint o un Map y conviértelo en GeoPoint
                    val geoPoint = if (data["ubicacion"] is GeoPoint) {
                        data["ubicacion"] as GeoPoint
                    } else {
                        val ubicacionMap = data["ubicacion"] as Map<String, Double>
                        GeoPoint(ubicacionMap["latitud"] ?: 0.0, ubicacionMap["longitud"] ?: 0.0)
                    }

                    // Verificación de tipo de duracionVisita para aceptar tanto Long como Double
                    val duracionVisita = (data["duracionVisita"] as? Double) ?: (data["duracionVisita"] as? Long)?.toDouble() ?: 0.0

                    val monument = Monument(
                        nombre = data["nombre"] as String,
                        descripcion = data["descripcion"] as String,
                        categoria = data["categoria"] as String,
                        duracionVisita = duracionVisita,
                        costoEntrada = data["costoEntrada"] as Boolean,
                        movilidadReducida = data["movilidadReducida"] as Boolean,
                        imagenURL = data["imagenURL"] as String,
                        audioURL = data["imagenURL"] as String,
                        ubicacion = geoPoint
                    )

                    monumentList.add(monument)
                }
                Log.d("Firestore", "Total monumentos recibidos: ${monumentList.size}")
                monumentAdapter.updateData(monumentList.toMutableList())
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error al obtener monumentos: ", exception)
            }
    }
}
