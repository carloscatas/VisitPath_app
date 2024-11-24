package com.example.visitpath

import android.util.Log
import com.google.firebase.firestore.GeoPoint

class ItineraryManager {

    fun generateMultiDayItineraries(
        allMonuments: List<Monument>,
        favoriteMonuments: List<Monument>,
        userLocation: GeoPoint,
        totalDays: Int,
        visitType: String,
        transportType: String,
        filteredMonuments: List<Monument>
    ): List<List<Monument>> {
        val hoursPerDay = 10 // Tiempo máximo disponible por día
        val dailyRoutes = mutableListOf<List<Monument>>()
        val remainingFavorites = favoriteMonuments.toMutableList()
        val remainingFiltered = filteredMonuments.toMutableList()
        var remainingMonuments = allMonuments.filterNot { it in favoriteMonuments || it in filteredMonuments }.toMutableList()

        Log.d("ItineraryManager", "Inicialización: Total favoritos: ${remainingFavorites.size}, Filtrados: ${remainingFiltered.size}, Restantes: ${remainingMonuments.size}")

        for (day in 1..totalDays) {
            Log.d("ItineraryManager", "Día $day: Comenzando cálculo de ruta. Monumentos restantes: ${remainingMonuments.size}")

            val dailyRoute = mutableListOf<Monument>()
            var currentTime = 0.0
            var currentLocation = userLocation

            // Priorizar favoritos
            while (remainingFavorites.isNotEmpty() && currentTime < hoursPerDay && dailyRoute.size < 10) {
                val nextFavorite = remainingFavorites.minByOrNull {
                    calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
                }
                if (nextFavorite != null) {
                    val travelTime = calculateTravelTime(
                        calculateDistance(currentLocation, GeoPoint(nextFavorite.latitud, nextFavorite.longitud)),
                        transportType
                    )
                    val visitTime = if (visitType == "Tour rápido") 0.5 else nextFavorite.duracionVisita
                    if (currentTime + travelTime + visitTime <= hoursPerDay) {
                        dailyRoute.add(nextFavorite)
                        currentTime += travelTime + visitTime
                        currentLocation = GeoPoint(nextFavorite.latitud, nextFavorite.longitud)
                        remainingFavorites.remove(nextFavorite)
                        Log.d("ItineraryManager", "Día $day: Añadido favorito '${nextFavorite.nombre}'. Tiempo acumulado: $currentTime")
                    } else {
                        break
                    }
                }
            }

            // Añadir filtrados
            while (remainingFiltered.isNotEmpty() && currentTime < hoursPerDay && dailyRoute.size < 10) {
                val nextFiltered = remainingFiltered.minByOrNull {
                    calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
                }
                if (nextFiltered != null) {
                    val travelTime = calculateTravelTime(
                        calculateDistance(currentLocation, GeoPoint(nextFiltered.latitud, nextFiltered.longitud)),
                        transportType
                    )
                    val visitTime = if (visitType == "Tour rápido") 0.5 else nextFiltered.duracionVisita
                    if (currentTime + travelTime + visitTime <= hoursPerDay) {
                        dailyRoute.add(nextFiltered)
                        currentTime += travelTime + visitTime
                        currentLocation = GeoPoint(nextFiltered.latitud, nextFiltered.longitud)
                        remainingFiltered.remove(nextFiltered)
                        Log.d("ItineraryManager", "Día $day: Añadido filtrado '${nextFiltered.nombre}'. Tiempo acumulado: $currentTime")
                    } else {
                        break
                    }
                }
            }

            // Añadir restantes
            while (remainingMonuments.isNotEmpty() && currentTime < hoursPerDay && dailyRoute.size < 10) {
                val nextMonument = remainingMonuments.minByOrNull {
                    calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
                }
                if (nextMonument != null) {
                    val travelTime = calculateTravelTime(
                        calculateDistance(currentLocation, GeoPoint(nextMonument.latitud, nextMonument.longitud)),
                        transportType
                    )
                    val visitTime = if (visitType == "Tour rápido") 0.5 else nextMonument.duracionVisita
                    if (currentTime + travelTime + visitTime <= hoursPerDay) {
                        dailyRoute.add(nextMonument)
                        currentTime += travelTime + visitTime
                        currentLocation = GeoPoint(nextMonument.latitud, nextMonument.longitud)
                        remainingMonuments.remove(nextMonument)
                        Log.d("ItineraryManager", "Día $day: Añadido restante '${nextMonument.nombre}'. Tiempo acumulado: $currentTime")
                    } else {
                        break
                    }
                }
            }

            Log.d("ItineraryManager", "Día $day completado. Monumentos añadidos: ${dailyRoute.size}")
            dailyRoutes.add(dailyRoute)

            if (dailyRoute.isEmpty() && remainingMonuments.isEmpty()) {
                Log.d("ItineraryManager", "Día $day: No quedan monumentos por añadir.")
            }
        }

        Log.d("ItineraryManager", "Generación completada. Total de días: ${dailyRoutes.size}")
        return dailyRoutes
    }


    private fun calculateDistance(start: GeoPoint, end: GeoPoint): Double {
        val earthRadius = 6371 // Radio de la Tierra en km
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(start.latitude)) *
                Math.cos(Math.toRadians(end.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun calculateTravelTime(distanceKm: Double, transportType: String): Double {
        val averageSpeed = when (transportType) {
            "Público" -> 20.0
            "Privado" -> 60.0
            else -> 5.0 // Caminando
        }
        return distanceKm / averageSpeed
    }
}
