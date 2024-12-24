package com.example.visitpath

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
        val hoursPerDay = 10.0 // Tiempo máximo disponible por día
        val dailyRoutes = mutableListOf<List<Monument>>()

        // Eliminar duplicados entre las listas
        val uniqueFavorites = favoriteMonuments.distinctBy { Pair(it.latitud, it.longitud) }.toMutableList()
        val uniqueFiltered = filteredMonuments.distinctBy { Pair(it.latitud, it.longitud) }
            .filterNot { filtered -> uniqueFavorites.any { it.latitud == filtered.latitud && it.longitud == filtered.longitud } }
            .toMutableList()
        val uniqueMonuments = allMonuments.distinctBy { Pair(it.latitud, it.longitud) }
            .filterNot { monument ->
                uniqueFavorites.any { it.latitud == monument.latitud && it.longitud == monument.longitud } ||
                        uniqueFiltered.any { it.latitud == monument.latitud && it.longitud == monument.longitud }
            }
            .toMutableList()

        for (day in 1..totalDays) {
            val dailyRoute = mutableListOf<Monument>()
            var currentTime = 0.0
            var currentLocation = userLocation

            // Función interna para añadir POIs optimizados
            fun addPOIsToDay(
                poiList: MutableList<Monument>,
                maxTime: Double
            ) {
                while (poiList.isNotEmpty() && currentTime < maxTime && dailyRoute.size < 10) {
                    val nextPOI = poiList.minByOrNull {
                        calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
                    }

                    if (nextPOI != null) {
                        val travelTime = calculateTravelTime(
                            calculateDistance(currentLocation, GeoPoint(nextPOI.latitud, nextPOI.longitud)),
                            transportType
                        )
                        val visitTime = if (visitType == "Tour rápido") 0.5 else nextPOI.duracionVisita

                        if (currentTime + travelTime + visitTime <= maxTime) {
                            dailyRoute.add(nextPOI)
                            currentTime += travelTime + visitTime
                            currentLocation = GeoPoint(nextPOI.latitud, nextPOI.longitud)
                            poiList.remove(nextPOI)
                        } else {
                            break
                        }
                    }
                }
            }

            // Añadir favoritos al día
            addPOIsToDay(uniqueFavorites, hoursPerDay)

            // Añadir POIs filtrados al día
            addPOIsToDay(uniqueFiltered, hoursPerDay)

            // Añadir POIs restantes al día
            addPOIsToDay(uniqueMonuments, hoursPerDay)

            // Guardar la ruta diaria si contiene POIs
            if (dailyRoute.isNotEmpty()) {
                dailyRoutes.add(dailyRoute)
            } else {
                // Si no hay más POIs disponibles, terminamos de generar itinerarios
                break
            }
        }

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
