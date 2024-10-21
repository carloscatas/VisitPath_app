package com.example.visitpath

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MonumentListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private val db = FirebaseFirestore.getInstance()

       override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_list)


           Log.d("ActivityLifecycle", "MonumentListActivity started") // Añade este log

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
                for (document in result) {
                    Log.d("Firestore", "Documento ID: ${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error obteniendo documentos: ", exception)
            }
    }

}
