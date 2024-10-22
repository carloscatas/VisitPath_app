package com.example.visitpath

import com.google.firebase.firestore.GeoPoint

data class Monument(
    val nombre: String = "",
    val descripcion: String = "",
    val ubicacion: GeoPoint = GeoPoint(0.0, 0.0),
    val categoria: String = "",
    val duracionVisita: Double = 0.0,
    val costoEntrada: Boolean = false,
    val movilidadReducida: Boolean = false,
    val imagenURL: String = "",
    val audioURL: String = ""
)
{
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", GeoPoint(0.0, 0.0), "", 0.0, false, false, "", "")
}