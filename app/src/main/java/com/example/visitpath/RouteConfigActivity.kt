package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RouteConfigActivity : AppCompatActivity() {

    private var selectedTime: Int = 1
    private var visitType: String = "Tour completo"
    private var transportType: String = "Caminando"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_config)

        val timeSeekBar: SeekBar = findViewById(R.id.timeSeekBar)
        val selectedTimeText: TextView = findViewById(R.id.selectedTimeText)
        val visitTypeGroup: RadioGroup = findViewById(R.id.visitTypeGroup)
        val transportTypeGroup: RadioGroup = findViewById(R.id.transportTypeGroup)
        val applyRouteConfigButton: Button = findViewById(R.id.applyRouteConfigButton)

        timeSeekBar.max = 14
        timeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedTime = progress
                selectedTimeText.text = if (progress <= 10) {
                    "$progress ${if (progress == 1) "hora" else "horas"}"
                } else {
                    val days = progress - 9
                    "$days ${if (days == 1) "día" else "días"}"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        visitTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            visitType = when (checkedId) {
                R.id.radioQuickTour -> "Tour rápido"
                R.id.radioFullTour -> "Tour completo"
                else -> "" // Valor por defecto
            }
        }

        // Configurar los clics para mostrar la información de cada tour
        val radioQuickTour: RadioButton = findViewById(R.id.radioQuickTour)
        val radioFullTour: RadioButton = findViewById(R.id.radioFullTour)

        radioQuickTour.setOnClickListener {
            Toast.makeText(this, "Visita exterior de los puntos de interés", Toast.LENGTH_SHORT).show()
        }

        radioFullTour.setOnClickListener {
            Toast.makeText(this, "Visita completa, con entrada a los puntos de interés", Toast.LENGTH_SHORT).show()
        }

        // Configurar RadioGroup para el tipo de transporte
        transportTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            transportType = when (checkedId) {
                R.id.radioPublicTransport -> "Público"
                R.id.radioPrivateTransport -> "Privado"
                else -> "Caminando"
            }
        }

        applyRouteConfigButton.setOnClickListener {
            // Validar el tipo de visita seleccionado
            if (visitType.isBlank()) {
                Toast.makeText(this, "Por favor, selecciona un tipo de tour.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent()
            resultIntent.putExtra("selectedTime", selectedTime) // Pasar horas o días
            resultIntent.putExtra("visitType", visitType) // Tour rápido o completo
            resultIntent.putExtra("transportType", transportType) // Transporte
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
