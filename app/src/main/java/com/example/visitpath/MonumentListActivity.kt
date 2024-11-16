package com.example.visitpath

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

class MonumentListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private val db = FirebaseFirestore.getInstance()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val AUTOCOMPLETE_REQUEST_CODE = 1
    private val ROUTE_CONFIG_REQUEST_CODE = 2
    private lateinit var monumentList: MutableList<Monument> // Lista completa de monumentos
    private var filteredMonuments: MutableList<Monument> = mutableListOf() // Lista para los monumentos filtrados
    private var selectedRadius: Int = 0
    private var selectedTransportType: String? = null
    private var userLocation: GeoPoint? = null

    // Variables para almacenar los filtros seleccionados
    private val selectedCategories = mutableListOf<String>()
    private val selectedDurations = mutableListOf<String>()
    private var selectedEntry: String? = null
    private var selectedAccessibility: String? = null
    private var selectedAudioGuide: String? = null
    private lateinit var locationText: TextView
    private val favoriteMonuments = mutableListOf<Monument>()

    // Añadimos un ActivityResultLauncher para obtener los cambios desde FavoritesActivity
    private val openFavoritesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedFavorites = result.data?.getParcelableArrayListExtra<Monument>("updatedFavorites")
            updatedFavorites?.let {
                // Actualizar la lista de favoritos con los datos más recientes
                favoriteMonuments.clear()
                favoriteMonuments.addAll(it)
                monumentAdapter.notifyDataSetChanged() // Actualizamos el RecyclerView
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.enterTransition = Fade()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_list)

        // Inicializar Google Places
        Places.initialize(applicationContext, "AIzaSyBRqF8SOEk36xfS8HWiFE5AJ2aIopQhTnE")

        // Configuramos el RecyclerView y el adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        monumentAdapter = MonumentAdapter(mutableListOf()) { monument ->
            if (monument.isFavorite) {
                if (!favoriteMonuments.contains(monument)) {
                    favoriteMonuments.add(monument)
                }
            } else {
                favoriteMonuments.remove(monument)
            }
        }
        recyclerView.adapter = monumentAdapter

        // Configurar el botón para abrir la pantalla de favoritos
        val openFavoritesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedFavorites = result.data?.getParcelableArrayListExtra<Monument>("updatedFavorites")
                updatedFavorites?.let {
                    favoriteMonuments.clear()
                    favoriteMonuments.addAll(it)

                    // Actualizar la lista principal con el estado de favoritos sincronizado
                    for (monument in monumentList) {
                        monument.isFavorite = favoriteMonuments.contains(monument)
                    }
                    monumentAdapter.notifyDataSetChanged()
                    monumentAdapter.updateFavorites(favoriteMonuments)
                }
            }
        }

        // Configurar el botón para abrir la pantalla de favoritos
        val openFavoritesButton: FloatingActionButton = findViewById(R.id.openFavoritesButton)
        openFavoritesButton.setOnClickListener {
            // Construir la lista de favoritos dinámica antes de abrir la actividad
            val currentFavorites = monumentList.filter { it.isFavorite } // Revisa qué monumentos tienen la estrella marcada
            favoriteMonuments.clear()
            favoriteMonuments.addAll(currentFavorites)

            val intent = Intent(this, FavoritesActivity::class.java)
            intent.putParcelableArrayListExtra("favorites", ArrayList(favoriteMonuments)) // Actualiza dinámicamente
            openFavoritesLauncher.launch(intent)
        }

        locationText = findViewById(R.id.locationText)

        getLocationAndUpdateTextView()

        // Configurar el botón de filtro
        val filterButton: ImageButton = findViewById(R.id.filterButton)
        val filterText: TextView = findViewById(R.id.filterText)

        val openFilterDialog = {
            showFilterDialog()
        }

        filterButton.setOnClickListener { openFilterDialog() }
        filterText.setOnClickListener { openFilterDialog() }

        requestLocationPermission()

        val fabCreateRoute: FloatingActionButton = findViewById(R.id.fab_create_route)
        fabCreateRoute.setOnClickListener {
            // Abre la actividad para configurar la ruta personalizada
            val intent = Intent(this, RouteConfigActivity::class.java)
            startActivityForResult(intent, ROUTE_CONFIG_REQUEST_CODE)
        }
    }

       private fun getLocationAndUpdateTextView() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        locationText.text = "${address.thoroughfare} ${address.featureName}"
                        userLocation = GeoPoint(location.latitude, location.longitude)
                    }
                } else {
                    locationText.text = "Ubicación desconocida"
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Manejo del AUTOCOMPLETE_REQUEST_CODE (no se modifica)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    locationText.text = place.address // Actualiza el globo con la nueva dirección
                    userLocation = GeoPoint(place.latLng!!.latitude, place.latLng!!.longitude)
                    applyFilters() // Actualizar la lista de monumentos basándose en la nueva ubicación
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Log.i("Location", "Error: ${status.statusMessage}")
                }
                RESULT_CANCELED -> {
                    // El usuario canceló la operación
                }
            }
        }

        // Manejo del ROUTE_CONFIG_REQUEST_CODE
        if (requestCode == ROUTE_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedTime = data?.getIntExtra("selectedTime", 1) ?: 1 // Tiempo disponible
            val visitType = data?.getStringExtra("visitType") ?: "Tour rápido" // Tipo de tour
            val transportType = data?.getStringExtra("transportType") ?: "Caminando" // Transporte

            // Calcula el tiempo total necesario para la ruta seleccionada
            val totalTime = calculateRouteTimes(filteredMonuments, visitType, transportType)

            if (totalTime <= selectedTime) {
                // Si el tiempo total es menor o igual al tiempo disponible, abre Google Maps
                openSingleDayRouteInGoogleMaps(filteredMonuments)
            } else {
                // Si el tiempo total excede el tiempo disponible, ofrece optimizar la ruta
                AlertDialog.Builder(this)
                    .setTitle("Ruta demasiado larga")
                    .setMessage("No es posible visitar todos los lugares en el tiempo seleccionado. ¿Quieres optimizar la ruta automáticamente?")
                    .setPositiveButton("Sí") { _, _ ->
                        optimizeRoute(selectedTime, visitType, transportType)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }




    private fun showFilterDialog() {
        val filterDialog = AlertDialog.Builder(this)
        filterDialog.setTitle("Selecciona Filtros")

        val inflater = LayoutInflater.from(this)
        val dialogLayout = inflater.inflate(R.layout.dialog_filter_options, null)
        filterDialog.setView(dialogLayout)

        // Configuración del radio de actuación
        val radiusSeekBar = dialogLayout.findViewById<SeekBar>(R.id.radiusSeekBar)
        val radiusTextView = dialogLayout.findViewById<TextView>(R.id.radiusTextView)

        radiusSeekBar.progress = selectedRadius
        radiusTextView.text = "$selectedRadius km"

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedRadius = progress
                radiusTextView.text = "$progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configuración de categorías
        val categoryCheckboxes = listOf(
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_natural_parks),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_museums),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_historic_monuments),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_religious_monuments),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_modern_architecture),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_cultural_attractions),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_urban_zones),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_natural_landscapes)
        )

        val categories = listOf(
            "Parques Naturales y Reservas",
            "Museos y Galerías de Arte",
            "Monumentos Históricos",
            "Monumentos Religiosos",
            "Edificios y Arquitectura Moderna",
            "Atracciones Culturales",
            "Zonas Urbanas y Barrios Históricos",
            "Paisajes Naturales"
        )

        // Actualizar el estado de los checkboxes de categorías según los filtros seleccionados
        for (i in categoryCheckboxes.indices) {
            categoryCheckboxes[i].isChecked = selectedCategories.contains(categories[i])
        }

        // Configuración de duración
        val durationCheckboxes = listOf(
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_duration_less_1),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_duration_1_2),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_duration_2_4),
            dialogLayout.findViewById<CheckBox>(R.id.checkbox_duration_more_4)
        )

        val durations = listOf("Menos de 1h", "Entre 1 y 2h", "Entre 2 y 4h", "Más de 4h")
        for (i in durationCheckboxes.indices) {
            durationCheckboxes[i].isChecked = selectedDurations.contains(durations[i])
        }

        // Configuración de accesibilidad, entrada y audioguía
        val checkBoxAccessibilityYes = dialogLayout.findViewById<CheckBox>(R.id.checkbox_accessibility_yes)
        val checkBoxAccessibilityNo = dialogLayout.findViewById<CheckBox>(R.id.checkbox_accessibility_no)
        val checkBoxEntryFree = dialogLayout.findViewById<CheckBox>(R.id.checkbox_entry_free)
        val checkBoxEntryPaid = dialogLayout.findViewById<CheckBox>(R.id.checkbox_entry_paid)
        val checkBoxAudioYes = dialogLayout.findViewById<CheckBox>(R.id.checkbox_audio_yes)
        val checkBoxAudioNo = dialogLayout.findViewById<CheckBox>(R.id.checkbox_audio_no)

        // Actualizar el estado de los checkboxes según los filtros seleccionados
        checkBoxAccessibilityYes.isChecked = selectedAccessibility == "Sí"
        checkBoxAccessibilityNo.isChecked = selectedAccessibility == "No"
        checkBoxEntryFree.isChecked = selectedEntry == "Gratuita"
        checkBoxEntryPaid.isChecked = selectedEntry == "De pago"
        checkBoxAudioYes.isChecked = selectedAudioGuide == "Sí"
        checkBoxAudioNo.isChecked = selectedAudioGuide == "No"

        // Configurar el botón de aplicar
        filterDialog.setPositiveButton("Aplicar") { dialog, _ ->
            // Guardar los valores seleccionados en los filtros
            selectedCategories.clear()
            for (i in categoryCheckboxes.indices) {
                if (categoryCheckboxes[i].isChecked) {
                    selectedCategories.add(categories[i])
                }
            }

            selectedDurations.clear()
            for (i in durationCheckboxes.indices) {
                if (durationCheckboxes[i].isChecked) {
                    selectedDurations.add(durations[i])
                }
            }

            selectedAccessibility = when {
                checkBoxAccessibilityYes.isChecked -> "Sí"
                checkBoxAccessibilityNo.isChecked -> "No"
                else -> null
            }

            selectedEntry = when {
                checkBoxEntryFree.isChecked -> "Gratuita"
                checkBoxEntryPaid.isChecked -> "De pago"
                else -> null
            }

            selectedAudioGuide = when {
                checkBoxAudioYes.isChecked -> "Sí"
                checkBoxAudioNo.isChecked -> "No"
                else -> null
            }

            applyFilters()
            dialog.dismiss()
        }

        // Configurar el botón de reestablecer
        filterDialog.setNeutralButton("Reestablecer") { dialog, _ ->
            resetFilters()
            dialog.dismiss()
        }

        filterDialog.show()
    }

    private val prioritizedMonuments: MutableList<Monument> = mutableListOf() // Lista de puntos prioritarios
    private fun applyFilters() {
        // Verificar si ningún filtro está seleccionado
        val noFiltersSelected = selectedCategories.isEmpty() &&
                selectedDurations.isEmpty() &&
                selectedEntry == null &&
                selectedAccessibility == null &&
                selectedAudioGuide == null &&
                selectedRadius == 0

        // Dividir los monumentos en favoritos y no favoritos
        val prioritized = mutableListOf<Monument>()
        val nonPrioritized = mutableListOf<Monument>()

        for (monument in monumentList) {
            if (favoriteMonuments.contains(monument)) {
                prioritized.add(monument) // Siempre prioriza favoritos
            } else {
                nonPrioritized.add(monument)
            }
        }

        // Aplicar filtros solo a los no favoritos
        val filteredNonPrioritized = if (noFiltersSelected) {
            nonPrioritized
        } else {
            nonPrioritized.filter { monument ->
                // Lógica de filtros
                val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(monument.categoria)
                val matchesDuration = selectedDurations.isEmpty() ||
                        (selectedDurations.contains("Menos de 1h") && monument.duracionVisita < 1) ||
                        (selectedDurations.contains("Entre 1 y 2h") && monument.duracionVisita in 1.0..2.0) ||
                        (selectedDurations.contains("Entre 2 y 4h") && monument.duracionVisita in 2.0..4.0) ||
                        (selectedDurations.contains("Más de 4h") && monument.duracionVisita > 4)
                val matchesEntry = selectedEntry == null || (selectedEntry == "Gratuita" && monument.costoEntrada) || (selectedEntry == "De pago" && !monument.costoEntrada)
                val matchesAccessibility = selectedAccessibility == null || (selectedAccessibility == "Sí" && monument.movilidadReducida) || (selectedAccessibility == "No" && !monument.movilidadReducida)
                val matchesAudioGuide = selectedAudioGuide == null || (selectedAudioGuide == "Sí" && monument.audioURL.isNotEmpty()) || (selectedAudioGuide == "No" && monument.audioURL.isEmpty())
                val matchesRadius = if (selectedRadius > 0) {
                    val distance = userLocation?.let { calculateDistance(it, GeoPoint(monument.latitud, monument.longitud)) }
                    distance != null && distance <= selectedRadius
                } else {
                    true // Ignora el filtro de radio si selectedRadius es 0
                }
                matchesCategory && matchesDuration && matchesEntry && matchesAccessibility && matchesAudioGuide && matchesRadius
            }.toMutableList()
        }

        // Ordenar por cercanía
        val sortedPrioritized = prioritized.sortedBy {
            calculateDistance(userLocation!!, GeoPoint(it.latitud, it.longitud))
        }
        val sortedNonPrioritized = filteredNonPrioritized.sortedBy {
            calculateDistance(userLocation!!, GeoPoint(it.latitud, it.longitud))
        }

        // Combinar ambas listas y actualizar el adaptador
        filteredMonuments = (sortedPrioritized + sortedNonPrioritized).toMutableList()
        monumentAdapter.updateData(filteredMonuments)
    }

    private fun resetFilters() {
        // Restablecer todas las opciones de filtro
        selectedCategories.clear()
        selectedDurations.clear()
        selectedEntry = null
        selectedAccessibility = null
        selectedAudioGuide = null
        selectedRadius = 0
        selectedTransportType = null

        // Mostrar todos los monumentos sin filtros
        filteredMonuments = monumentList
        monumentAdapter.updateData(filteredMonuments)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationPermission", "Permiso no concedido, solicitando permiso...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            Log.d("LocationPermission", "Permiso ya concedido")
            getUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LocationPermission", "Permiso concedido por el usuario")
                getUserLocation()
            } else {
                Log.d("LocationPermission", "Permiso denegado por el usuario")
            }
        }
    }

    private fun getUserLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val newLocation = locationResult.lastLocation
                if (newLocation != null) {
                    Log.d("Location", "Ubicación obtenida: Latitud: ${newLocation.latitude}, Longitud: ${newLocation.longitude}")
                    userLocation = GeoPoint(newLocation.latitude, newLocation.longitude)
                    fetchMonumentsFromFirestore(userLocation!!)
                    fusedLocationProviderClient.removeLocationUpdates(this)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun calculateDistance(userLocation: GeoPoint, monumentLocation: GeoPoint): Double {
        val earthRadius = 6371
        val dLat = Math.toRadians(monumentLocation.latitude - userLocation.latitude)
        val dLng = Math.toRadians(monumentLocation.longitude - userLocation.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(userLocation.latitude)) *
                Math.cos(Math.toRadians(monumentLocation.latitude)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
    // Simulamos la velocidad promedio en km/h
    private val AVERAGE_SPEED_PUBLIC_TRANSPORT = 20.0  // En km/h
    private val AVERAGE_SPEED_PRIVATE_TRANSPORT = 60.0 // En km/h

    // Función para calcular el tiempo de desplazamiento en función del tipo de transporte y la distancia
    private fun calculateTravelTime(distanceKm: Double): Double {
        return if (selectedTransportType == "Público") {
            distanceKm / AVERAGE_SPEED_PUBLIC_TRANSPORT
        } else {
            distanceKm / AVERAGE_SPEED_PRIVATE_TRANSPORT
        }
    }

    private fun calculateRouteTimes(points: List<Monument>, visitType: String, transportType: String): Double {
        val visitDurationQuick = 0.5 // 30 minutos para tour rápido
        val averageSpeed = when (transportType) {
            "A pie" -> 5.0 // Velocidad en km/h
            "Público" -> 20.0
            "Privado" -> 60.0
            else -> 5.0
        }

        var totalTime = 0.0

        for (i in points.indices) {
            // Añadir tiempo de visita
            val monumentVisitDuration = if (visitType == "Tour rápido") visitDurationQuick else points[i].duracionVisita
            totalTime += monumentVisitDuration

            // Añadir tiempo de desplazamiento (excepto el último punto)
            if (i < points.size - 1) {
                val start = points[i]
                val end = points[i + 1]
                val distance = calculateDistance(
                    GeoPoint(start.latitud, start.longitud),
                    GeoPoint(end.latitud, end.longitud)
                )
                totalTime += distance / averageSpeed
            }
        }

        // Añadir margen de error para descansos y comidas
        totalTime += 2.0 // 2 horas de margen

        return totalTime
    }

    private fun fetchMonumentsFromFirestore(userLocation: GeoPoint) {
        db.collection("monumentos").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null && !snapshot.isEmpty) {
                monumentList = snapshot.documents.mapNotNull { document ->
                    val data = document.data
                    val geoPoint = data?.get("ubicacion") as? GeoPoint
                    if (geoPoint != null) {
                        Monument(
                            nombre = data["nombre"] as? String ?: "Desconocido",
                            descripcion = data["descripcion"] as? String ?: "",
                            categoria = data["categoria"] as? String ?: "",
                            duracionVisita = (data["duracionVisita"] as? Number)?.toDouble() ?: 0.0,
                            costoEntrada = data["costoEntrada"] as? Boolean ?: false,
                            movilidadReducida = data["movilidadReducida"] as? Boolean ?: false,
                            imagenURL = data["imagenURL"] as? String ?: "",
                            audioURL = data["audioURL"] as? String ?: "",
                            latitud = geoPoint.latitude,
                            longitud = geoPoint.longitude
                        )
                    } else null
                }.toMutableList()

                monumentList = monumentList.sortedBy { calculateDistance(userLocation, GeoPoint(it.latitud, it.longitud)) }.toMutableList()
                filteredMonuments = monumentList
                monumentAdapter.updateData(filteredMonuments)
            }
        }
    }

    private fun openSingleDayRouteInGoogleMaps(points: List<Monument>) {
        // Verifica si hay suficientes puntos de interés para la ruta
        if (points.isEmpty()) {
            Toast.makeText(this, "No hay puntos de interés para mostrar en la ruta.", Toast.LENGTH_SHORT).show()
            return
        }

        // Construye la URI para Google Maps con los puntos de interés como waypoints
        val origin = "${userLocation?.latitude},${userLocation?.longitude}"
        val destination = "${points.last().latitud},${points.last().longitud}"

        // Crea la lista de waypoints (hasta 10 puntos debido a la limitación de Google Maps)
        val waypoints = points.take(9).joinToString("|") { "${it.latitud},${it.longitud}" }

        val gmmIntentUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                    "&origin=$origin" +
                    "&destination=$destination" +
                    "&waypoints=$waypoints" +
                    "&travelmode=walking"
        )

        // Inicia la intención para abrir Google Maps
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun optimizeRoute(selectedTime: Int, visitType: String, transportType: String) {
        var timeAvailable = selectedTime.toDouble()
        val optimizedMonuments = mutableListOf<Monument>()

        // Incluye primero los prioritarios
        for (monument in prioritizedMonuments) {
            val timeNeeded = calculateRouteTimes(listOf(monument), visitType, transportType)
            if (timeAvailable - timeNeeded >= 0) {
                optimizedMonuments.add(monument)
                timeAvailable -= timeNeeded
            }
        }

        // Incluye los demás puntos hasta llenar el tiempo disponible
        for (monument in filteredMonuments) {
            if (!prioritizedMonuments.contains(monument)) {
                val timeNeeded = calculateRouteTimes(listOf(monument), visitType, transportType)
                if (timeAvailable - timeNeeded >= 0) {
                    optimizedMonuments.add(monument)
                    timeAvailable -= timeNeeded
                }
            }
        }

        // Actualiza la lista filtrada y muestra la ruta optimizada
        filteredMonuments = optimizedMonuments
        openSingleDayRouteInGoogleMaps(filteredMonuments)
    }

}