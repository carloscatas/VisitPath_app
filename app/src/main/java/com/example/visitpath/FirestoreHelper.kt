package com.example.visitpath

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    fun getMonumentos(onSuccess: (List<Monumento>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("monumentos")
            .get()
            .addOnSuccessListener { result ->
                val monumentos = result.map { document ->
                    val data = document.data
                    Monumento(
                        nombre = data["nombre"] as String,
                        descripcion = data["descripcion"] as String,
                        ubicacion = data["ubicacion"] as Map<String, Double>,
                        audioURL = data["audioURL"] as String,
                        movilidadReducida = data["movilidadReducida"] as Boolean,
                        categoria = data["categoria"] as String,
                        costoEntrada = data["costoEntrada"] as Boolean,
                        duracionVisita = (data["duracionVisita"] as Double).toInt()
                    )
                }
                onSuccess(monumentos)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

data class Monumento(
    val nombre: String,
    val descripcion: String,
    val ubicacion: Map<String, Double>,
    val audioURL: String,
    val movilidadReducida: Boolean,
    val categoria: String,
    val costoEntrada: Boolean,
    val duracionVisita: Int
)




