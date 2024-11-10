package com.example.visitpath

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RouteConfigActivity : AppCompatActivity() {

    private var selectedTime: Int = 1 // Tiempo en horas (inicializa en 1)
    private var visitType: String = "Exterior"
    private var transportType: String = "Caminando"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_config)

        val timeSeekBar: SeekBar = findViewById(R.id.timeSeekBar)
        val selectedTimeText: TextView = findViewById(R.id.selectedTimeText)
        val visitTypeGroup: RadioGroup = findViewById(R.id.visitTypeGroup)
        val transportTypeGroup: RadioGroup = findViewById(R.id.transportTypeGroup)
        val applyRouteConfigButton: Button = findViewById(R.id.applyRouteConfigButton)

        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedTime = progress
                selectedTimeText.text = if (progress <= 10) {
                    "$progress ${if (progress == 1) "hora" else "horas"}"
                } else {
                    val days = progress - 10
                    "$days ${if (days == 1) "día" else "días"}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        visitTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            visitType = if (checkedId == R.id.radioQuickTour) "Tour rápido" else "Tour completo"
        }

        val infoQuickTour: ImageView = findViewById(R.id.infoQuickTour)
        val infoFullTour: ImageView = findViewById(R.id.infoFullTour)

        infoQuickTour.setOnClickListener {
            Toast.makeText(this, "Visita exterior de los puntos de interés", Toast.LENGTH_SHORT).show()
        }

        infoFullTour.setOnClickListener {
            Toast.makeText(this, "Visita completa, con entrada a los puntos de interés", Toast.LENGTH_SHORT).show()
        }


        transportTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            transportType = when (checkedId) {
                R.id.radioPublicTransport -> "Público"
                R.id.radioPrivateTransport -> "Privado"
                else -> "A pie"
            }
        }

        applyRouteConfigButton.setOnClickListener {
            // Aquí deberíamos guardar los valores seleccionados y calcular la ruta
            finish()
        }
    }
}
