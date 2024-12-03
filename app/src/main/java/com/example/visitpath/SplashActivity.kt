package com.example.visitpath

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Obtener referencia al ImageView y aplicar la animación de zoom
        val splashImage: ImageView = findViewById(R.id.splashImage)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        splashImage.startAnimation(zoomInAnimation)

        // Cambiar a MonumentListActivity después del tiempo de la splash screen
        val splashScreenDuration = 3000L // 3 segundos

        Handler(Looper.getMainLooper()).postDelayed({
            // Navegar a MonumentListActivity
            val intent = Intent(this@SplashActivity, MonumentListActivity::class.java)
            startActivity(intent)
            finish()
        }, splashScreenDuration)
    }
}
