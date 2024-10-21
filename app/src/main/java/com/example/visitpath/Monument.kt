package com.example.visitpath

data class Monument(
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val duracionVisita: Double = 0.0,
    val costoEntrada: Boolean = false,
    val movilidadReducida: Boolean = false
)
