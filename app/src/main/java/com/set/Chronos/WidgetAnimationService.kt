/*
package com.set.Chronos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

class WidgetAnimationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    // Animation State
    private val particles = ArrayList<Particle>()
    private val stars = ArrayList<Star>()
    private val random = Random()
    private val width = 400
    private val height = 400
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private val paint = Paint()

    data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float, var size: Float,
        var color: Int
    )
    
    data class Star(var x: Float, var y: Float, var alpha: Float)

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateAndDraw()
                handler.postDelayed(this, 50) // 20 FPS
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        initStars()
        isRunning = true
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            handler.post(updateRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun startForegroundService() {
        val channelId = "widget_animation_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Widget Animation",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Chronos Widget")
            .setContentText("Animating widget...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(2, notification)
    }
    
    private fun initStars() {
        stars.clear()
        for (i in 0 until 20) {
            stars.add(Star(random.nextFloat() * width, random.nextFloat() * height * 0.6f, random.nextFloat()))
        }
    }

    private fun updateAndDraw() {
        // 1. Update State
        spawnParticles()
        updateParticles()

        // 2. Draw
        drawFrame()

        // 3. Push to Widget
        val context = this
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ChronosWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            val views = RemoteViews(packageName, R.layout.widget_Chronos)
            views.setImageViewBitmap(R.id.widget_image, bitmap)
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } else {
            stopSelf()
        }
    }

    private fun spawnParticles() {
        val cx = width / 2f
        val cy = height * 0.8f
        
        // Fire
        for (i in 0 until 2) {
            val angle = (random.nextFloat() - 0.5f) * PI * 0.6
            val speed = (4 + random.nextFloat() * 4)
            particles.add(Particle(
                x = cx + (random.nextFloat() - 0.5f) * 50,
                y = cy + (random.nextFloat() - 0.5f) * 15,
                vx = (sin(angle) * 0.8).toFloat(),
                vy = -speed.toFloat(),
                life = 1.0f,
                size = 10 + random.nextFloat() * 15,
                color = if(random.nextBoolean()) Color.rgb(255, 100, 0) else Color.rgb(255, 200, 50)
            ))
        }
    }

    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= 0.04f
            p.x += p.vx
            p.y += p.vy
            p.size *= 0.95f
            
            if (p.life <= 0) {
                iterator.remove()
            }
        }
    }

    private fun drawFrame() {
        // Background
        val bgPaint = Paint()
        bgPaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#051015"), Color.parseColor("#0a1a0f"), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Stars
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        for (star in stars) {
             paint.alpha = (star.alpha * 200).toInt() + 55
             canvas.drawCircle(star.x, star.y, 2f, paint)
        }

        // Logs
        val cx = width / 2f
        val cy = height * 0.8f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(62, 39, 35)
        paint.alpha = 255
        canvas.drawRoundRect(cx - 50, cy, cx + 50, cy + 25, 10f, 10f, paint)
        canvas.save()
        canvas.rotate(-20f, cx, cy)
        canvas.drawRoundRect(cx - 60, cy - 10, cx + 10, cy + 10, 10f, 10f, paint)
        canvas.restore()
        
        // Particles
        for (p in particles) {
            paint.color = p.color
            paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
        
        // Glow
        val glowPaint = Paint()
        glowPaint.shader = RadialGradient(cx, cy - 30, 120f, 
            Color.argb(80, 255, 100, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy - 30, 120f, glowPaint)
    }

}
*/
