/*
package com.set.Chronos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.widget.RemoteViews
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

class ChronosWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        // If you have a service to animate the widget, you might want to restart it.
        val serviceIntent = Intent(context, WidgetAnimationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val serviceIntent = Intent(context, WidgetAnimationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.stopService(Intent(context, WidgetAnimationService::class.java))
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("ChronosPrefs", Context.MODE_PRIVATE)
            val temperature = prefs.getFloat("temperature", 2500f)
            val intensity = prefs.getFloat("intensity", 1.0f)
            val windSpeed = prefs.getFloat("windSpeed", 0.0f)
            val logSize = prefs.getFloat("logSize", 1.0f)
            val environment = prefs.getString("environment", "Forest") ?: "Forest"
            val weather = prefs.getString("weather", "Clear") ?: "Clear"

            val width = 500
            val height = 500
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawSnapshot(canvas, width, height, temperature, intensity, windSpeed, logSize, environment, weather)

            val views = RemoteViews(context.packageName, R.layout.widget_Chronos)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getTemperatureColor(temp: Float): Int {
            return when {
                temp < 2000 -> Color.rgb(255, 60, 0)
                temp < 3500 -> Color.rgb(255, 140, 0)
                temp < 5000 -> Color.rgb(255, 200, 50)
                temp < 7500 -> Color.rgb(255, 255, 255)
                else -> Color.rgb(100, 200, 255)
            }
        }

        fun drawSnapshot(canvas: Canvas, w: Int, h: Int, temperature: Float, intensity: Float, windSpeed: Float, logSize: Float, environment: String, weather: String) {
            val paint = Paint()
            val random = Random()
            val width = w.toFloat()
            val height = h.toFloat()

            // --- Draw Background ---
            val bgTop: Int
            val bgBottom: Int
            
            when (environment) {
                "Forest" -> {
                    bgTop = Color.parseColor("#051015")
                    bgBottom = Color.parseColor("#0a1a0f")
                }
                "Beach" -> {
                    bgTop = Color.parseColor("#020510")
                    bgBottom = Color.parseColor("#0a0f20")
                }
                "Snowy" -> {
                    bgTop = Color.parseColor("#0a0a10")
                    bgBottom = Color.parseColor("#e0e0ec")
                }
                "SimpleBlack" -> {
                    bgTop = Color.BLACK
                    bgBottom = Color.BLACK
                }
                else -> {
                    bgTop = Color.parseColor("#051015")
                    bgBottom = Color.parseColor("#0a1a0f")
                }
            }
            
            val bgPaint = Paint()
            if (bgTop == bgBottom) {
                bgPaint.color = bgTop
            } else {
                bgPaint.shader = LinearGradient(0f, 0f, 0f, height, bgTop, bgBottom, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, bgPaint)

            // --- Draw Stars/Weather ---
            if (weather == "Clear" || weather == "Windy") {
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                for (i in 0..150) {
                    paint.alpha = (random.nextFloat() * 200).toInt() + 55 // Twinkle effect
                    canvas.drawCircle(random.nextFloat() * width, random.nextFloat() * height * 0.75f, 1f + random.nextFloat() * 2, paint)
                }
            }

            // --- Beach Water ---
            if (environment == "Beach") {
                val horizonY = height * 0.6f
                val waterPaint = Paint()
                waterPaint.shader = LinearGradient(0f, horizonY, 0f, height, Color.parseColor("#051025"), Color.parseColor("#0a1a30"), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, horizonY, width, height, waterPaint)
                
                // Reflection
                val centerX = width / 2f
                val ChronosCy = height * 0.85f
                val reflectionY = ChronosCy + (ChronosCy - horizonY) * 0.2f
                val reflectionWidth = 120f * intensity
                val reflectPaint = Paint()
                reflectPaint.shader = RadialGradient(centerX, reflectionY, reflectionWidth, Color.argb(80, 255, 150, 50), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(centerX, reflectionY, reflectionWidth, reflectPaint)
            }
            
             // --- Forest Grass ---
            if (environment == "Forest") {
                val path = Path()
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                val horizonY = height * 0.75f
                for (i in 0 until 150) {
                    val bladeX = random.nextFloat() * width
                    val bladeY = horizonY + random.nextFloat() * (height - horizonY)
                    val depthFactor = (bladeY - horizonY) / (height - horizonY)
                    val bladeHeight = (20 + random.nextFloat() * 30) * (0.5f + depthFactor)
                    val lean = (random.nextFloat() - 0.5f) * 10
                    val sway = (random.nextFloat() - 0.5f) * windSpeed * 15
                    
                    val r = (20 + random.nextInt(20))
                    val g = (50 + depthFactor * 40 + random.nextInt(20)).toInt()
                    val b = (20 + random.nextInt(20))
                    paint.color = Color.rgb(r,g,b)
                    paint.strokeWidth = 1 + depthFactor * 2

                    path.reset()
                    path.moveTo(bladeX, bladeY)
                    path.quadTo(bladeX + lean + sway, bladeY - bladeHeight * 0.5f, bladeX + lean*2 + sway*1.5f, bladeY - bladeHeight)
                    canvas.drawPath(path, paint)
                }
            }

            // --- Draw Logs ---
            val cx = width / 2f
            val cy = height * 0.85f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.color = Color.rgb(62, 39, 35)
            paint.strokeWidth = 2f
            
            val s = logSize
            canvas.drawRoundRect(cx - 40*s, cy, cx + 40*s, cy + 20*s, 10f*s, 10f*s, paint)
            canvas.save()
            canvas.rotate(-30f, cx - 20*s, cy - 5*s)
            canvas.drawRoundRect(cx - 60*s, cy - 15*s, cx + 20*s, cy + 5*s, 10f*s, 10f*s, paint)
            canvas.restore()
            canvas.save()
            canvas.rotate(30f, cx + 20*s, cy - 5*s)
            canvas.drawRoundRect(cx - 20*s, cy - 15*s, cx + 60*s, cy + 5*s, 10f*s, 10f*s, paint)
            canvas.restore()

            // --- Draw Particles (Fire, Smoke, Sparks) ---
            val particleCount = (60 * intensity).toInt().coerceIn(20, 150)
            val baseColor = getTemperatureColor(temperature)

            for (i in 0 until particleCount) {
                val life = random.nextFloat() // Represents how far along its life a particle is (0.0=dead, 1.0=new)
                val invLife = 1.0f - life

                // Fire
                val angle = (random.nextFloat() - 0.5f) * PI * 0.5
                val speed = (2 + random.nextFloat() * 3) * 2.0 * (temperature / 3000.0)
                val startX = cx + (random.nextFloat() - 0.5f) * 60 * intensity
                val startY = cy + (random.nextFloat() - 0.5f) * 20
                val finalX = startX + (sin(angle) * 0.5 + windSpeed * 0.2).toFloat() * 40 * invLife
                val finalY = startY - (speed.toFloat() * 40 * invLife)
                paint.color = baseColor
                paint.alpha = (life * 255).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(finalX, finalY, ((15 + random.nextFloat() * 20) * intensity) * life, paint)

                // Sparks
                if (random.nextFloat() < 0.1 * intensity) {
                    val sparkLife = random.nextFloat()
                    val sparkInvLife = 1.0f - sparkLife
                    val sparkVx = ((random.nextFloat() - 0.5f) * 2 + windSpeed * 0.3).toFloat()
                    val sparkVy = -3 - random.nextFloat() * 3
                    val sparkStartX = cx + (random.nextFloat() - 0.5f) * 40
                    val sp_x = sparkStartX + sparkVx * sparkInvLife * 20
                    val sp_y = cy + sparkVy * sparkInvLife * 20
                    paint.color = Color.rgb(255, 220, 100)
                    paint.alpha = (sparkLife * 255).toInt()
                    canvas.drawCircle(sp_x, sp_y, 2 + random.nextFloat(), paint)
                }
                 // Smoke
                if (random.nextFloat() < 0.2 * intensity) {
                    val smokeLife = random.nextFloat()
                    val smokeInvLife = 1.0f - smokeLife
                    val sm_x = cx + (random.nextFloat() - 0.5f) * 50 + (windSpeed * smokeLife * 40)
                    val sm_y = cy - 40 - (smokeInvLife * 120)
                    paint.color = Color.rgb(100, 100, 100)
                    paint.alpha = (smokeLife * 40).toInt().coerceIn(0, 40)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(sm_x, sm_y, (20 + random.nextFloat() * 20) * (smokeLife), paint)
                }
            }

             // --- Weather Particles ---
            if (weather == "Rainy") {
                 paint.strokeWidth = 1.5f
                 for(i in 0..60) {
                     val p_x = random.nextFloat() * width
                     val p_y = random.nextFloat() * height
                     paint.color = Color.argb(random.nextInt(80) + 50, 180, 200, 255)
                     canvas.drawLine(p_x, p_y, p_x + windSpeed * 3, p_y + 25, paint)
                 }
            } else if (weather == "Snowy") {
                paint.style = Paint.Style.FILL
                for(i in 0..80) {
                    val p_x = random.nextFloat() * width
                    val p_y = random.nextFloat() * height
                    val life = random.nextFloat()
                    paint.color = Color.WHITE
                    paint.alpha = (life * 150).toInt() + 50
                    canvas.drawCircle(p_x + (1-life) * windSpeed * 10, p_y, 2f + life * 3, paint)
                }
            }
            
            // --- Glow Overlay ---
            val glowColor = getTemperatureColor(temperature)
            val glowPaint = Paint()
            glowPaint.shader = RadialGradient(cx, cy - 50, 220f * intensity, 
                Color.argb((90 * intensity).toInt().coerceIn(0,150), Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)), 
                Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width, height, glowPaint)
        }
    }
}
*/
