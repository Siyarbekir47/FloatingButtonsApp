package com.ramazan.floatingbuttonsapp

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var layout: LinearLayout? = null
    private var isAdded = false
    private val uiHandler = Handler(Looper.getMainLooper())

    private val API_OUTSIDE = "TRIGGER HERE"
    private val API_INSIDE  = "TRIGGER HERE"

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isAdded) {
            addOverlay()
            isAdded = true
        }
        return START_STICKY
    }

    private fun addOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(24, 0, 24, 0)
        }

        layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            val btnOutside = Button(this@FloatingButtonService).apply {
                text = "Tür Aussen"
                setTextColor(Color.WHITE)
                textSize = 16f
                minWidth = 260
                background = createRoundedDrawable("#2196F3")
                setOnClickListener { onButtonPressed(this, API_OUTSIDE) }
            }

            val btnInside = Button(this@FloatingButtonService).apply {
                text = "Tür Innen"
                setTextColor(Color.WHITE)
                textSize = 16f
                minWidth = 260
                background = createRoundedDrawable("#2196F3")
                setOnClickListener { onButtonPressed(this, API_INSIDE) }
            }

            addView(btnOutside, buttonParams)
            addView(btnInside, buttonParams)
        }

        windowManager.addView(layout, params)
    }

    private fun createRoundedDrawable(hexColor: String): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 48f
            setColor(Color.parseColor(hexColor))
        }

    private fun onButtonPressed(button: Button, url: String) {
        button.background = createRoundedDrawable("#1976D2")
        uiHandler.postDelayed({
            button.background = createRoundedDrawable("#2196F3")
        }, 150)

        callApi(url)
    }

    private fun callApi(url: String) {
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val ok = response.isSuccessful
                val code = response.code
                Log.d("API", "Response($code): ${response.body?.string()}")
                uiToast(if (ok) "Gesendet ($code)" else "Fehler ($code)")
            } catch (e: IOException) {
                Log.e("API", "Error: ${e.message}", e)
                uiToast("Netzwerkfehler")
            }
        }
    }

    private fun uiToast(msg: String) {
        uiHandler.post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        layout?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        layout = null
        isAdded = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
