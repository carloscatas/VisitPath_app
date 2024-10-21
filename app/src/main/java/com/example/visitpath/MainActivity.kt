package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Usamos el layout en XML

        // Inicialización de Firebase
        FirebaseApp.initializeApp(this)

        val btnStart: Button = findViewById(R.id.btnStart) // Asocia el botón por ID
        btnStart.setOnClickListener {
            // Navegar a MainMenuActivity cuando se haga clic en el botón
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
        }
    }
}
