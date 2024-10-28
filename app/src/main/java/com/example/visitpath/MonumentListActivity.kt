package com.example.visitpath

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.Fade
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        window.enterTransition = Fade()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_list)

        Log.d("ActivityLifecycle", "MonumentListActivity started")

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        monumentAdapter = MonumentAdapter(mutableListOf())
        recyclerView.adapter = monumentAdapter

        requestLocationPermission()
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
        Log.d("Firestore", "Intentando obtener monumentos desde Firestore...")

        db.collection("monumentos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "Error en la solicitud a Firestore: ", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val monumentList = mutableListOf<Monument>()
                    Log.d("Firestore", "Cantidad de documentos recibidos: ${snapshot.size()}")

                    for (document in snapshot.documents) {
                        val data = document.data
                        val geoPoint = data?.get("ubicacion") as? GeoPoint
                        val duracionVisita = (data?.get("duracionVisita") as? Double)
                            ?: (data?.get("duracionVisita") as? Long)?.toDouble() ?: 0.0

                        if (geoPoint != null) {
                            val monument = Monument(
                                nombre = data["nombre"] as? String ?: "Desconocido",
                                descripcion = data["descripcion"] as? String ?: "",
                                categoria = data["categoria"] as? String ?: "",
                                duracionVisita = duracionVisita,
                                costoEntrada = data["costoEntrada"] as? Boolean ?: false,
                                movilidadReducida = data["movilidadReducida"] as? Boolean ?: false,
                                imagenURL = data["imagenURL"] as? String ?: "",
                                audioURL = data["audioURL"] as? String ?: "",
                                latitud = geoPoint.latitude,
                                longitud = geoPoint.longitude,
                            )
                            monumentList.add(monument)
                        } else {
                            Log.e("Firestore", "Error: El campo 'ubicacion' no es un GeoPoint en el documento con ID ${document.id}")
                        }
                    }

                    if (monumentList.isNotEmpty()) {
                        monumentList.sortBy { calculateDistance(userLocation, GeoPoint(it.latitud, it.longitud)) }
                        Log.d("Firestore", "Total monumentos después de ordenar: ${monumentList.size}")
                        monumentAdapter.updateData(monumentList)
                    } else {
                        Log.d("Firestore", "No se recibieron monumentos de Firestore")
                    }
                } else {
                    Log.d("Firestore", "No se recibieron documentos en tiempo real")
                }
            }
    }
}
