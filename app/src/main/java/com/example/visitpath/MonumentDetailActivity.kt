package com.example.visitpath

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MonumentDetailActivity : AppCompatActivity() {

    private var monument: Monument? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false // Variable mutable para controlar el estado de reproducción

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monument_detail)

        // Obtener el monumento del intent de forma segura
        monument = intent.getParcelableExtra("monument")
        Log.d("MonumentDetailActivity", "Received monument: ${monument?.nombre}")

        // Si no se recibió un monumento, salir de la actividad
        if (monument == null) {
            finish() // Termina la actividad si no hay datos
            return
        }

        // Asignar el nombre y detalles del monumento a los elementos de la UI
        findViewById<TextView>(R.id.monumentName).text = monument?.nombre
        findViewById<TextView>(R.id.monumentDetails).text = monument?.descripcion
        findViewById<TextView>(R.id.monumentCategory).text = "Categoría: ${monument?.categoria}"
        findViewById<TextView>(R.id.monumentDuration).text = "Duración: ${monument?.duracionVisita} horas"
        findViewById<TextView>(R.id.monumentCost).text = if (monument?.costoEntrada == true) "Entrada: Gratis" else "Entrada: De pago"
        findViewById<TextView>(R.id.monumentAccessibility).text = if (monument?.movilidadReducida == true) "Accesibilidad: Sí" else "Accesibilidad: No"
        findViewById<TextView>(R.id.monumentAudio).text = if (monument?.audioURL?.isNotEmpty() == true) "Audioguía: Sí" else "Audioguía: No"

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
}
