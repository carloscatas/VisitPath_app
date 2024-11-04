package com.example.visitpath

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.widget.ImageButton
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

        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_filter_options, null)
        filterDialog.setView(dialogLayout)

        // Configurar el botón de aplicar
        filterDialog.setPositiveButton("Aplicar") { dialog, _ ->
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
        // Aplicar los filtros seleccionados a la lista de monumentos
        filteredMonuments = monumentList.filter { monument ->
            // Filtrar por categoría
            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(monument.categoria)
            // Filtrar por duración
            val matchesDuration = selectedDurations.isEmpty() ||
                    (selectedDurations.contains("Menos de 1h") && monument.duracionVisita < 1) ||
                    (selectedDurations.contains("Entre 1 y 2h") && monument.duracionVisita in 1.0..2.0) ||
                    (selectedDurations.contains("Entre 2 y 4h") && monument.duracionVisita in 2.0..4.0) ||
                    (selectedDurations.contains("Más de 4h") && monument.duracionVisita > 4)

            // Filtrar por entrada
            val matchesEntry = selectedEntry == null ||
                    (selectedEntry == "Gratuita" && monument.costoEntrada) ||
                    (selectedEntry == "De pago" && !monument.costoEntrada)

            // Filtrar por accesibilidad
            val matchesAccessibility = selectedAccessibility == null ||
                    (selectedAccessibility == "Sí" && monument.movilidadReducida) ||
                    (selectedAccessibility == "No" && !monument.movilidadReducida)
            // Filtrar por audioguía
            val matchesAudioGuide = selectedAudioGuide == null ||
                    (selectedAudioGuide == "Sí" && monument.audioURL.isNotEmpty()) ||
                    (selectedAudioGuide == "No" && monument.audioURL.isEmpty())

            matchesCategory && matchesDuration && matchesEntry && matchesAccessibility && matchesAudioGuide
        }.toMutableList()

        monumentAdapter.updateData(filteredMonuments)
    }

    private fun resetFilters() {
        // Restablecer todas las opciones de filtro
        selectedCategories.clear()
        selectedDurations.clear()
        selectedEntry = null
        selectedAccessibility = null
        selectedAudioGuide = null

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
                    val userLocation = GeoPoint(newLocation.latitude, newLocation.longitude)
                    fetchMonumentsFromFirestore(userLocation)
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
