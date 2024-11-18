package com.example.visitpath

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.GeoPoint

class RoutePlanner {

    // Constantes de velocidad media (en km/h)
    private val AVERAGE_SPEED_PUBLIC_TRANSPORT = 20.0
    private val AVERAGE_SPEED_PRIVATE_TRANSPORT = 60.0
    private val AVERAGE_SPEED_WALKING = 5.0

    companion object {
        const val ROUTE_CONFIG_REQUEST_CODE = 100
    }

    /**
     * Calcula la distancia entre dos puntos geográficos usando la fórmula de Haversine.
     */
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

    /**
     * Calcula el tiempo necesario para desplazarse una distancia dada dependiendo del tipo de transporte.
     */
    private fun calculateTravelTime(distanceKm: Double, transportType: String): Double {
        return when (transportType) {
            "Público" -> distanceKm / AVERAGE_SPEED_PUBLIC_TRANSPORT
            "Privado" -> distanceKm / AVERAGE_SPEED_PRIVATE_TRANSPORT
            else -> distanceKm / AVERAGE_SPEED_WALKING // Caminando
        }
    }

    /**
     * Genera una ruta optimizada basada en la lista de monumentos, los favoritos y las preferencias del usuario.
     */
    fun generateRoute(
        allMonuments: List<Monument>,
        favoriteMonuments: List<Monument>,
        selectedTime: Int,
        visitType: String,
        transportType: String,
        userLocation: GeoPoint
    ): List<List<Monument>> {
        val visitDurationQuick = 0.5 // 30 minutos por visita para tour rápido
        val availableTimePerDay =
            if (selectedTime <= 10) {
                selectedTime.toDouble()
            } else { (selectedTime - 10) * 24.0
            }
        Log.d("RoutePlanner", "Available time per day: $availableTimePerDay horas")

        val prioritized = mutableListOf<Monument>()
        val nonPrioritized = mutableListOf<Monument>()

        for (monument in allMonuments) {
            if (favoriteMonuments.contains(monument)) {
                prioritized.add(monument)
            } else {
                nonPrioritized.add(monument)
            }
        }

        val sortedPrioritized = prioritized.sortedBy {
            calculateDistance(
                userLocation,
                GeoPoint(it.latitud, it.longitud)
            )
        }
        val sortedNonPrioritized = nonPrioritized.sortedBy {
            calculateDistance(
                userLocation,
                GeoPoint(it.latitud, it.longitud)
            )
        }

        val allMonumentsSorted = sortedPrioritized + sortedNonPrioritized

        val itineraries = mutableListOf<MutableList<Monument>>()
        var currentItinerary = mutableListOf<Monument>()
        var currentTime = 0.0

        for (monument in allMonumentsSorted) {
            val travelTime = if (currentItinerary.isNotEmpty()) {
                calculateTravelTime(
                    calculateDistance(
                        GeoPoint(currentItinerary.last().latitud, currentItinerary.last().longitud),
                        GeoPoint(monument.latitud, monument.longitud)
                    ),
                    transportType
                )
            } else {
                calculateTravelTime(
                    calculateDistance(userLocation, GeoPoint(monument.latitud, monument.longitud)),
                    transportType
                )
            }

            // Log para depurar
            Log.d("RoutePlanner", "Travel time to ${monument.nombre}: $travelTime horas")

            val visitTime =
                if (visitType == "Tour rápido") visitDurationQuick else monument.duracionVisita

            Log.d("RoutePlanner", "Visit time for ${monument.nombre}: $visitTime horas")

            val totalTime = travelTime + visitTime
            Log.d("RoutePlanner", "Total time for ${monument.nombre}: $totalTime horas")

            if (currentTime + totalTime > availableTimePerDay) {
                Log.d("RoutePlanner", "Exceeded available time: $currentTime + $totalTime > $availableTimePerDay")
                itineraries.add(currentItinerary)
                currentItinerary = mutableListOf()
                currentTime = 0.0
            }

            currentItinerary.add(monument)
            currentTime += totalTime
        }

        if (currentItinerary.isNotEmpty()) {
            itineraries.add(currentItinerary)
        }

        return itineraries
    }
    // Nueva función pública para calcular la distancia entre dos puntos
    fun getDistanceBetweenPoints(start: GeoPoint, end: GeoPoint): Double {
        return calculateDistance(start, end)
    }

    fun generateOptimizedRoute(
        allMonuments: List<Monument>,
        favoriteMonuments: List<Monument>,
        selectedTime: Int,
        visitType: String,
        transportType: String,
        userLocation: GeoPoint
    ): List<Monument> {
        val prioritizedMonuments = favoriteMonuments + allMonuments
        val optimizedRoute = mutableListOf<Monument>()
        var totalTime = 0.0

        for (monument in prioritizedMonuments) {
            val travelTime = calculateTravelTime(
                calculateDistance(
                    if (optimizedRoute.isEmpty()) userLocation
                    else GeoPoint(optimizedRoute.last().latitud, optimizedRoute.last().longitud),
                    GeoPoint(monument.latitud, monument.longitud)
                ),
                transportType
            )

            val visitTime = if (visitType == "Tour rápido") 0.5 else monument.duracionVisita
            if (totalTime + travelTime + visitTime <= selectedTime) {
                optimizedRoute.add(monument)
                totalTime += travelTime + visitTime
            } else {
                break
            }
        }
        return optimizedRoute
    }


    /**
     * Construye una URL para abrir la ruta en Google Maps con los puntos seleccionados.
     */
    fun openRouteInGoogleMaps(
        context: Context,
        userLocation: GeoPoint,
        route: List<Monument>,
        transportMode: String
    ) {
        if (route.isEmpty()) {
            Toast.makeText(
                context,
                "No hay puntos de interés para mostrar en la ruta.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Eliminar duplicados de la lista de monumentos (basado en latitud y longitud)
        val uniqueRoute = route.distinctBy { Pair(it.latitud, it.longitud) }

        // Establecer el punto de origen y el destino de la ruta
        val origin = "${userLocation.latitude},${userLocation.longitude}"
        val destination = "${uniqueRoute.last().latitud},${uniqueRoute.last().longitud}"

        // Construir waypoints (hasta 10 puntos debido a las limitaciones de Google Maps)
        val waypoints = uniqueRoute.dropLast(1).joinToString("|") { "${it.latitud},${it.longitud}" }

        val gmmIntentUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                    "&origin=$origin" +
                    "&destination=$destination" +
                    if (waypoints.isNotEmpty()) "&waypoints=$waypoints" else "" +
                            "&travelmode=${transportMode.lowercase()}"
        )

        // Crear un Intent para abrir Google Maps
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        // Verificar si hay una aplicación que pueda manejar el Intent y luego abrir Google Maps
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            Toast.makeText(
                context,
                "Google Maps no está disponible en este dispositivo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}