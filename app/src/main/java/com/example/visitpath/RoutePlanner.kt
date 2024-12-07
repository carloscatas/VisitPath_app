package com.example.visitpath

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    fun calculateTravelTime(distanceKm: Double, transportType: String): Double {
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
        userLocation: GeoPoint,
        filteredMonuments: MutableList<Monument>
    ): List<List<Monument>> {
        val hoursPerDay = 10 // Tiempo máximo disponible por día
        val dailyRoutes = mutableListOf<List<Monument>>()
        val remainingFavorites = favoriteMonuments.toMutableList()
        val remainingFiltered = filteredMonuments.toMutableList()
        val remainingMonuments = allMonuments.toMutableList()
        val visitDurationQuick = 0.5 // 30 minutos por visita para tour rápido
        val availableTime = if (selectedTime <= 10) {
            selectedTime.toDouble()
        } else {
            (selectedTime - 10) * 24.0
        }

        // Crear listas para priorización
        val filteredNonFavoriteMonuments = filteredMonuments.filterNot { favoriteMonuments.contains(it) }.toMutableList()
        val route = mutableListOf<Monument>()
        var currentTime = 0.0
        var currentLocation = userLocation

        // Paso 1: Añadir favoritos
        val favoritesMutable = favoriteMonuments.toMutableList()
        while (favoritesMutable.isNotEmpty()) {
            val nextFavorite = favoritesMutable.minByOrNull {
                calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
            }

            if (nextFavorite != null) {
                val travelTime = calculateTravelTime(
                    calculateDistance(currentLocation, GeoPoint(nextFavorite.latitud, nextFavorite.longitud)),
                    transportType
                )
                val visitTime = if (visitType == "Tour rápido") visitDurationQuick else nextFavorite.duracionVisita
                val totalTime = travelTime + visitTime

                if (currentTime + totalTime <= availableTime) {
                    route.add(nextFavorite)
                    currentTime += totalTime
                    currentLocation = GeoPoint(nextFavorite.latitud, nextFavorite.longitud)
                    favoritesMutable.remove(nextFavorite)
                } else {
                    break // No hay tiempo suficiente para más favoritos
                }
            }
        }

        // Paso 2: Añadir puntos filtrados (no favoritos)
        while (currentTime < availableTime && filteredNonFavoriteMonuments.isNotEmpty()) {
            val nextFiltered = filteredNonFavoriteMonuments.minByOrNull {
                calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
            }

            if (nextFiltered != null) {
                val travelTime = calculateTravelTime(
                    calculateDistance(currentLocation, GeoPoint(nextFiltered.latitud, nextFiltered.longitud)),
                    transportType
                )
                val visitTime = if (visitType == "Tour rápido") visitDurationQuick else nextFiltered.duracionVisita
                val totalTime = travelTime + visitTime

                if (currentTime + totalTime <= availableTime) {
                    route.add(nextFiltered)
                    currentTime += totalTime
                    currentLocation = GeoPoint(nextFiltered.latitud, nextFiltered.longitud)
                    filteredNonFavoriteMonuments.remove(nextFiltered)
                } else {
                    break // No hay tiempo suficiente para más filtrados
                }
            }
        }

        // Paso 3: Añadir puntos restantes



        // Devolver la ruta completa, envuelta en una lista de listas para cumplir con el tipo de retorno requerido
        return if (route.isNotEmpty()) {
            listOf(route)
        } else {
            emptyList()
        }
    }


    fun handleRouteGeneration(
        allMonuments: List<Monument>,
        favoriteMonuments: List<Monument>,
        userLocation: GeoPoint,
        selectedTime: Int,
        visitType: String,
        transportType: String,
        filteredMonuments: List<Monument>
    ): Triple<MutableList<Monument>, MutableList<Monument>, Double> {
        val routes = generateRoute(
            allMonuments = allMonuments,
            favoriteMonuments = favoriteMonuments,
            selectedTime = selectedTime,
            visitType = visitType,
            transportType = transportType,
            userLocation = userLocation,
            filteredMonuments = filteredMonuments.toMutableList()
        )

        if (routes.isEmpty()) {
            return Triple(mutableListOf(), mutableListOf(), 0.0) // No se pudo generar ninguna ruta
        }

        val viableRoute = routes.first().toMutableList()
        var currentTime = 0.0

        // Calcular el tiempo total de la ruta generada
        for (i in 0 until viableRoute.size - 1) {
            val monument = viableRoute[i]
            val nextMonument = viableRoute[i + 1]

            val travelTime = calculateTravelTime(
                getDistanceBetweenPoints(
                    GeoPoint(monument.latitud, monument.longitud),
                    GeoPoint(nextMonument.latitud, nextMonument.longitud)
                ),
                transportType
            )

            val visitTime = if (visitType == "Tour rápido") 0.5 else monument.duracionVisita
            currentTime += travelTime + visitTime
        }

        // Añadir el tiempo de visita del último punto
        if (viableRoute.isNotEmpty()) {
            val lastMonument = viableRoute.last()
            currentTime += if (visitType == "Tour rápido") 0.5 else lastMonument.duracionVisita
        }

        // logs para ver los monumentos en la ruta viable
        Log.d("handleRouteGeneration", "Monumentos viables:")
        viableRoute.forEach {
            Log.d("handleRouteGeneration", "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}")
        }

        Log.d("handleRouteGeneration", "Todos los monumentos antes de filtrar:")
        allMonuments.forEach {
            Log.d("handleRouteGeneration", "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}")
        }

        // Los "monumentos restantes" deben ser aquellos que no están en la ruta viable ni en los favoritos
        val remainingMonuments = allMonuments.filterNot {
            viableRoute.contains(it) || favoriteMonuments.contains(it)
        }.toMutableList()

        Log.d("handleRouteGeneration", "Monumentos restantes después de calcular:")
        remainingMonuments.forEach {
            Log.d("handleRouteGeneration", "- ${it.nombre}, Latitud: ${it.latitud}, Longitud: ${it.longitud}")
        }

        return Triple(viableRoute, remainingMonuments, currentTime)
    }

    fun checkForExtraTimeAndShowDialog(
        context: Context,
        currentTime: Double,
        availableTime: Double,
        route: MutableList<Monument>,
        selectedTime: Int,
        remainingTime: Double,
        remainingMonuments: MutableList<Monument>,
        visitType: String,
        transportType: String,
        currentLocation: GeoPoint,
        filteredMonuments: MutableList<Monument>,
        favoriteMonuments: MutableList<Monument>
    ) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle("Optimizar Ruta")
        dialogBuilder.setMessage("Tiene tiempo de visitar más puntos de interés. ¿Quiere optimizar la ruta automáticamente o añadir más puntos a favoritos?")

        dialogBuilder.setPositiveButton("Optimizar Ruta") { _, _ ->
            // Añadir un log para verificar si `selectedTime` se está pasando correctamente
            Log.d("RoutePlanner", "Tiempo restante tras favoritos y filtrados para la optimización: $remainingTime")

            // En lugar de optimizar directamente, abre la actividad de optimización
            val intent = Intent(context, OptimizationActivity::class.java)

            // Transferir los monumentos restantes (excluyendo favoritos y ya visitados)
            intent.putParcelableArrayListExtra("remainingMonuments", ArrayList(remainingMonuments))

            // Transferir los favoritos
            intent.putParcelableArrayListExtra("favoriteMonuments", ArrayList(favoriteMonuments))

            // Transferir los filtrados
            intent.putParcelableArrayListExtra("filteredMonuments", ArrayList(filteredMonuments))

            // Transferir la ubicación del usuario desglosada en latitud y longitud
            intent.putExtra("userLatitude", currentLocation.latitude)
            intent.putExtra("userLongitude", currentLocation.longitude)

            // Transferir los tipos de visita,tiempo y transporte seleccionados
            intent.putExtra("transportType", transportType)
            intent.putExtra("visitType", visitType)
            intent.putExtra("remainingTime", remainingTime)
            intent.putExtra("selectedTime", selectedTime)


            // Iniciar la actividad de optimización
            (context as AppCompatActivity).startActivityForResult(intent, ROUTE_CONFIG_REQUEST_CODE)
        }

        dialogBuilder.setNegativeButton("Añadir Más Favoritos") { _, _ ->
            Toast.makeText(context, "Puede añadir más favoritos desde la pantalla principal.", Toast.LENGTH_SHORT).show()
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }


    // Función para calcular la distancia entre dos puntos
    fun getDistanceBetweenPoints(start: GeoPoint, end: GeoPoint): Double {
        return calculateDistance(start, end)
    }
    fun calculateTotalTimeForMonuments(
        monuments: List<Monument>,
        userLocation: GeoPoint,
        transportType: String,
        visitType: String
    ): Double {
        var totalTime = 0.0
        var currentLocation = userLocation
        val visitDurationQuick = 0.5
        val visitDurationNormal = 1.0

        monuments.forEach { monument ->
            val travelTime = calculateTravelTime(
                calculateDistance(currentLocation, GeoPoint(monument.latitud, monument.longitud)),
                transportType
            )
            val visitTime = if (visitType == "Tour rápido") visitDurationQuick else visitDurationNormal
            totalTime += travelTime + visitTime
            currentLocation = GeoPoint(monument.latitud, monument.longitud)
        }
        return totalTime
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
                "No es posible visitar los puntos de interés seleccionados, con los criterios de ruta establecidos.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Eliminar duplicados de la lista de monumentos (basado en latitud y longitud)
        val uniqueRoute = route.distinctBy { Pair(it.latitud, it.longitud) }

        // Verificar si hay múltiples waypoints
        val hasWaypoints = uniqueRoute.size > 2

        // Mostrar alerta si transporte público con múltiples waypoints
        if (transportMode.lowercase() == "público" && hasWaypoints) {
            AlertDialog.Builder(context)
                .setTitle("Transporte Público no soportado")
                .setMessage("No es posible generar rutas con múltiples paradas en transporte público. Elige otro medio de transporte.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return
        }

        // Establecer el punto de origen y el destino de la ruta
        val origin = "${userLocation.latitude},${userLocation.longitude}"
        val destination = "${uniqueRoute.last().latitud},${uniqueRoute.last().longitud}"

        // Construir waypoints (hasta 10 puntos debido a las limitaciones de Google Maps)
        val waypoints = uniqueRoute.dropLast(1).joinToString("|") { "${it.latitud},${it.longitud}" }

        // Valida el modo de transporte
        val validTransportMode = when (transportMode.lowercase()) {
            "caminando" -> "walking"
            "público" -> "transit"
            "privado" -> "driving"
            else -> "driving"
        }

        // Construir la URI según el modo de transporte
        val gmmIntentUri: Uri = if (validTransportMode == "walking" && waypoints.isNotEmpty()) {
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                        "&origin=$origin" +
                        "&destination=$destination" +
                        "&waypoints=$waypoints"+
                        "&travelmode=walking"
            )
        } else {
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                        "&origin=$origin" +
                        "&destination=$destination" +
                        if (waypoints.isNotEmpty()) "&waypoints=$waypoints" else "" +
                                "&travelmode=$validTransportMode"
            )
        }

        // Log para verificar la URI generada
        Log.d("openRouteInGoogleMaps", "URI generada: $gmmIntentUri")

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



    fun generateFinalRoute(
        favoriteMonuments: List<Monument>,
        filteredMonuments: List<Monument>,
        remainingMonuments: List<Monument>,
        userLocation: GeoPoint,
        transportType: String,
        availableTime: Int = 10 // Por defecto, máximo 10 horas disponibles
    ): List<Monument> {
        val route = mutableListOf<Monument>()
        var currentLocation = userLocation
        var currentTime = 0.0
        val visitDurationQuick = 0.5 // 30 minutos por visita para tour rápido

        // Paso 1: Añadir favoritos
        val favoritesMutable = favoriteMonuments.toMutableList()
        Log.d("RoutePlanner", "Favoritos recibidos (${favoritesMutable.size}):")
        favoritesMutable.forEach { Log.d("RoutePlanner", it.nombre) }

        while (favoritesMutable.isNotEmpty()) {
            val nextFavorite = favoritesMutable.minByOrNull {
                calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
            }

            if (nextFavorite != null) {
                val travelTime = calculateTravelTime(
                    calculateDistance(currentLocation, GeoPoint(nextFavorite.latitud, nextFavorite.longitud)),
                    transportType
                )
                val visitTime = visitDurationQuick
                val totalTime = travelTime + visitTime

                if (currentTime + totalTime <= availableTime) {
                    route.add(nextFavorite)
                    currentTime += totalTime
                    currentLocation = GeoPoint(nextFavorite.latitud, nextFavorite.longitud)
                    favoritesMutable.remove(nextFavorite)
                } else {
                    break // No hay tiempo suficiente para más favoritos
                }
            }
        }

        // Paso 2: Añadir puntos filtrados (no favoritos)
        val filteredNonFavoriteMonuments = filteredMonuments.filterNot { favoriteMonuments.contains(it) }.toMutableList()
        Log.d("RoutePlanner", "Filtrados recibidos (${filteredNonFavoriteMonuments.size}):")
        filteredNonFavoriteMonuments.forEach { Log.d("RoutePlanner", it.nombre) }

        while (currentTime < availableTime && filteredNonFavoriteMonuments.isNotEmpty()) {
            val nextFiltered = filteredNonFavoriteMonuments.minByOrNull {
                calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
            }

            if (nextFiltered != null) {
                val travelTime = calculateTravelTime(
                    calculateDistance(currentLocation, GeoPoint(nextFiltered.latitud, nextFiltered.longitud)),
                    transportType
                )
                val visitTime = visitDurationQuick
                val totalTime = travelTime + visitTime

                if (currentTime + totalTime <= availableTime) {
                    route.add(nextFiltered)
                    currentTime += totalTime
                    currentLocation = GeoPoint(nextFiltered.latitud, nextFiltered.longitud)
                    filteredNonFavoriteMonuments.remove(nextFiltered)
                } else {
                    break // No hay tiempo suficiente para más filtrados
                }
            }
        }

        // Paso 3: Añadir puntos restantes (si hay espacio)
        val remainingMonumentsMutable = remainingMonuments.toMutableList()
        Log.d("RoutePlanner", "Monumentos restantes (${remainingMonumentsMutable.size}):")
        remainingMonumentsMutable.forEach { Log.d("RoutePlanner", it.nombre) }

        while (currentTime < availableTime && remainingMonumentsMutable.isNotEmpty() && route.size < 10) {
            val nextMonument = remainingMonumentsMutable.minByOrNull {
                calculateDistance(currentLocation, GeoPoint(it.latitud, it.longitud))
            }

            if (nextMonument != null) {
                val travelTime = calculateTravelTime(
                    calculateDistance(currentLocation, GeoPoint(nextMonument.latitud, nextMonument.longitud)),
                    transportType
                )
                val visitTime = visitDurationQuick
                val totalTime = travelTime + visitTime

                if (currentTime + totalTime <= availableTime) {
                    route.add(nextMonument)
                    currentTime += totalTime
                    currentLocation = GeoPoint(nextMonument.latitud, nextMonument.longitud)
                    remainingMonumentsMutable.remove(nextMonument)
                } else {
                    break // No hay tiempo suficiente para más puntos restantes
                }
            }
        }

        return route.take(10) // Limitar la ruta a un máximo de 10 monumentos
    }

    fun optimizeRouteOrder(currentLocation: GeoPoint, monuments: List<Monument>): List<Monument> {
        val orderedRoute = mutableListOf<Monument>()
        val remainingMonuments = monuments.toMutableList()

        var location = currentLocation

        // Ordenar los monumentos por proximidad sucesiva
        while (remainingMonuments.isNotEmpty()) {
            // Buscar el monumento más cercano desde la ubicación actual
            val nextMonument = remainingMonuments.minByOrNull {
                calculateDistance(location, GeoPoint(it.latitud, it.longitud))
            }

            if (nextMonument != null) {
                orderedRoute.add(nextMonument)
                location = GeoPoint(nextMonument.latitud, nextMonument.longitud)
                remainingMonuments.remove(nextMonument)
            }
        }

        return orderedRoute
    }

}