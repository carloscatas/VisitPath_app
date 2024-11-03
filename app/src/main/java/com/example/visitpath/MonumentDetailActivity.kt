package com.example.visitpath

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MonumentDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private var monument: Monument? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false // Variable mutable para controlar el estado de reproducción
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_detail)

        // Inicializar el fragmento de mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Obtener el monumento del intent de forma segura
        monument = intent.getParcelableExtra("monument")
        Log.d("MonumentDetailActivity", "Received monument: ${monument?.nombre}")

        // Si no se recibió un monumento, salir de la actividad
        if (monument == null) {
            finish() // Termina la actividad si no hay datos
            return
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Regresar a la actividad anterior
        }

        // Asignar el nombre y detalles del monumento a los elementos de la UI
        findViewById<TextView>(R.id.monumentName).text = monument?.nombre
        findViewById<TextView>(R.id.monumentDetails).text = monument?.descripcion

        val headerImageView = findViewById<ImageView>(R.id.monumentHeaderImage)
        Glide.with(this)
            .load(monument?.imagenURL) // Usa el URL o recurso que tengas para la imagen del monumento
            .into(findViewById(R.id.monumentHeaderImage))


        findViewById<TextView>(R.id.monumentCategory).text = "Categoría: ${monument?.categoria}"
        findViewById<TextView>(R.id.monumentDuration).text = "Duración: ${monument?.duracionVisita} horas"
        findViewById<TextView>(R.id.monumentCost).text = if (monument?.costoEntrada == true) "Entrada: Gratis" else "Entrada: De pago"
        findViewById<TextView>(R.id.monumentAccessibility).text = if (monument?.movilidadReducida == true) "Accesibilidad: Sí" else "Accesibilidad: No"
        findViewById<TextView>(R.id.monumentAudio).text = if (monument?.audioURL?.isNotEmpty() == true) "Audioguía: Sí" else "Audioguía: No"

        // Controlar el icono de accesibilidad
        val accessibilityTextView = findViewById<TextView>(R.id.monumentAccessibility)
        val accessibilityIcon = findViewById<ImageView>(R.id.monumentAccessibilityIcon)

        if (monument?.movilidadReducida == true) {
            accessibilityTextView.text = "Accesibilidad PMR: Sí"
            accessibilityIcon.setImageResource(R.drawable.ic_accessibility)
        } else {
            accessibilityTextView.text = "Accesibilidad PMR: No"
            accessibilityIcon.setImageResource(R.drawable.ic_not_accessibility)
        }

        /// Configurar el botón para reproducir la audioguía
        val playButton: FloatingActionButton = findViewById(R.id.playAudioGuideButton)
        playButton.setOnClickListener {
            val audioUrl = monument?.audioURL

            if (audioUrl.isNullOrEmpty()) {
                Toast.makeText(this, "No hay audioguía disponible para este monumento", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPlaying) {
                // Iniciar la reproducción
                playAudio(audioUrl)
                playButton.setImageResource(R.drawable.ic_pause) // Cambiar el ícono a pausa
            } else {
                // Pausar la reproducción
                pauseAudio()
                playButton.setImageResource(R.drawable.ic_play) // Cambiar el ícono a reproducir
            }
        }

    }

    private fun playAudio(audioUrl: String) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener {
                        it.start()
                        setPlaying() // Método para asignar el estado de reproducción
                    }
                    setOnCompletionListener {
                        setNotPlaying() // Método para asignar el estado de no reproducción
                        findViewById<Button>(R.id.playAudioGuideButton).text = "Reproducir Audioguía"
                        releaseMediaPlayer()
                    }
                    prepareAsync() // Método asíncrono que no bloquea el hilo principal
                }
            } else {
                mediaPlayer?.start()
                setPlaying()
            }
        } catch (e: Exception) {
            Log.e("MonumentDetailActivity", "Error al reproducir el audio: ${e.message}")
            Toast.makeText(this, "Error al reproducir la audioguía", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPlaying() {
        isPlaying = true
    }

    private fun setNotPlaying() {
        isPlaying = false
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        setNotPlaying()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar los recursos del MediaPlayer
        releaseMediaPlayer()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Verificar si el monumento tiene una ubicación válida
        if (monument != null && monument?.latitud != 0.0 && monument?.longitud != 0.0) {
            // Crear una ubicación usando latitud y longitud
            val location = LatLng(monument!!.latitud, monument!!.longitud)
            map?.addMarker(MarkerOptions().position(location).title(monument?.nombre))
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
        }
    }

}
