package com.example.visitpath

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class MonumentListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentAdapter: MonumentAdapter
    private val db = FirebaseFirestore.getInstance()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private lateinit var monumentList: MutableList<Monument> // Lista completa de monumentos
    private var filteredMonuments: MutableList<Monument> = mutableListOf() // Lista para los monumentos filtrados
    private var selectedRadius: Int = 0
    private var selectedLocationType: String = "Ubicación Actual" // O "Ubicación Manual"
    private var selectedTransportType: String? = null
    private var userLocation: GeoPoint? = null

    // Variables para almacenar los filtros seleccionados
    private val selectedCategories = mutableListOf<String>()
    private val selectedDurations = mutableListOf<String>()
    private var selectedEntry: String? = null
    private var selectedAccessibility: String? = null
    private var selectedAudioGuide: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.enterTransition = Fade()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_list)

        Log.d("ActivityLifecycle", "MonumentListActivity started")

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        monumentAdapter = MonumentAdapter(mutableListOf())
        recyclerView.adapter = monumentAdapter

        // Configurar el botón de filtro
        val filterButton: ImageButton = findViewById(R.id.filterButton)
        val filterText: TextView = findViewById(R.id.filterText)
        val locationSelector: LinearLayout = findViewById(R.id.locationSelector)
        val locationText: TextView = findViewById(R.id.locationText)

        locationSelector.setOnClickListener {
            // TODO: Abrir pantalla de selección de ubicación (Autocomplete de Google Maps)
        }

        val openFilterDialog = {
            showFilterDialog()
        }

        filterButton.setOnClickListener { openFilterDialog() }
        filterText.setOnClickListener { openFilterDialog() }

        requestLocationPermission()
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

        // Configuración del tipo de transporte
        val transportRadioGroup = dialogLayout.findViewById<RadioGroup>(R.id.transportRadioGroup)
        val radioPrivateTransport = dialogLayout.findViewById<RadioButton>(R.id.radio_private_transport)
        val radioPublicTransport = dialogLayout.findViewById<RadioButton>(R.id.radio_public_transport)

        // Restaurar selección anterior
        selectedTransportType?.let {
            if (it == "Privado") radioPrivateTransport.isChecked = true
            if (it == "Público") radioPublicTransport.isChecked = true
        }

        transportRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransportType = if (checkedId == R.id.radio_private_transport) "Privado" else "Público"
        }

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

    private fun applyFilters() {
        // Verificar si ningún filtro está seleccionado (categorías, duración, entrada, etc.)
        val noFiltersSelected = selectedCategories.isEmpty() &&
                selectedDurations.isEmpty() &&
                selectedEntry == null &&
                selectedAccessibility == null &&
                selectedAudioGuide == null &&
                selectedRadius == 0

        // Si no hay filtros seleccionados, mostramos todos los monumentos ordenados por cercanía
        filteredMonuments = if (noFiltersSelected) {
            monumentList.sortedBy { monument ->
                userLocation?.let { calculateDistance(it, GeoPoint(monument.latitud, monument.longitud)) } ?: 0.0
            }.toMutableList()
        } else {
            // Aquí aplicaríamos la lógica de filtros si hay filtros seleccionados
            monumentList.filter { monument ->
                // Mantener la lógica de cada filtro aquí
                val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(monument.categoria)
                val matchesDuration = selectedDurations.isEmpty() ||
                        (selectedDurations.contains("Menos de 1h") && monument.duracionVisita < 1) ||
                        (selectedDurations.contains("Entre 1 y 2h") && monument.duracionVisita in 1.0..2.0) ||
                        (selectedDurations.contains("Entre 2 y 4h") && monument.duracionVisita in 2.0..4.0) ||
                        (selectedDurations.contains("Más de 4h") && monument.duracionVisita > 4)
                val matchesEntry = selectedEntry == null || (selectedEntry == "Gratuita" && monument.costoEntrada) || (selectedEntry == "De pago" && !monument.costoEntrada)
                val matchesAccessibility = selectedAccessibility == null || (selectedAccessibility == "Sí" && monument.movilidadReducida) || (selectedAccessibility == "No" && !monument.movilidadReducida)
                val matchesAudioGuide = selectedAudioGuide == null || (selectedAudioGuide == "Sí" && monument.audioURL.isNotEmpty()) || (selectedAudioGuide == "No" && monument.audioURL.isEmpty())
                val distance = userLocation?.let { calculateDistance(it, GeoPoint(monument.latitud, monument.longitud)) }
                val matchesRadius = if (selectedRadius > 0) {
                    val distance = userLocation?.let { calculateDistance(it, GeoPoint(monument.latitud, monument.longitud)) }
                    distance != null && distance <= selectedRadius
                } else {
                    true // Ignora el filtro de radio si selectedRadius es 0
                }
                // Combinar todos los filtros
                matchesCategory && matchesDuration && matchesEntry && matchesAccessibility && matchesAudioGuide && matchesRadius
            }.toMutableList()
        }

        // Calcula los tiempos de la ruta en función de los monumentos filtrados y el transporte seleccionado
        calculateRouteTimes()
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
        selectedLocationType = "Ubicación Actual"
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

    private fun calculateRouteTimes() {
        var totalTime = 0.0

        for (i in 0 until filteredMonuments.size - 1) {
            val startMonument = filteredMonuments[i]
            val endMonument = filteredMonuments[i + 1]

            // Calcula la distancia entre dos monumentos
            val distance = calculateDistance(
                GeoPoint(startMonument.latitud, startMonument.longitud),
                GeoPoint(endMonument.latitud, endMonument.longitud)
            )

            // Calcula el tiempo de desplazamiento basado en el tipo de transporte
            val travelTime = calculateTravelTime(distance)
            totalTime += travelTime

        }

        // Muestra o utiliza el tiempo total estimado para la ruta
        Log.d("Route", "Tiempo total estimado para la ruta: $totalTime horas")
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
}
