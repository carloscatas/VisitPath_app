package com.example.visitpath

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Monument(
    val nombre: String = "",
    val descripcion: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val categoria: String = "",
    val duracionVisita: Double = 0.0,
    val costoEntrada: Boolean = false,
    val movilidadReducida: Boolean = false,
    val imagenURL: String = "",
    val audioURL: String = "",
    var isFavorite: Boolean = false // Campo para indicar si es favorito

) : Parcelable {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", 0.0, 0.0, "", 0.0, false, false, "", "")
}

