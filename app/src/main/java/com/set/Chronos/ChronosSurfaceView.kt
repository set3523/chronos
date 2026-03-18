package com.set.Chronos

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.PI
import kotlin.math.sin
import java.util.Random

class ChronosSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val drawThread = DrawThread(holder)

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawThread.running = true
        drawThread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawThread.updateSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        drawThread.running = false
        while (retry) {
            try {
                drawThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }
        }
    }

    // Public methods to update settings
    fun setTemperature(temp: Int) {
        drawThread.setTemperature(temp)
    }
    
    fun setIntensity(intensity: Float) {
        drawThread.setIntensity(intensity)
    }

    fun setWindSpeed(speed: Float) {
        drawThread.setWindSpeed(speed)
    }
    
    fun setEnvironment(env: String) {
        drawThread.setEnvironment(env)
    }
    
    fun setWeather(w: String) {
        drawThread.setWeather(w)
    }
    
    fun setLogSize(size: Float) {
        drawThread.setLogSize(size)
    }

    class DrawThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        var running = false
        private val paint = Paint()
        private val random = Random()
        
        // State
        private val particles = ArrayList<Particle>()
        private val stars = ArrayList<Star>()
        private val grassBlades = ArrayList<GrassBlade>()
        private val ripples = ArrayList<Ripple>()
        
        private var width = 0
        private var height = 0
        
        // Settings - Volatile for thread safety
        @Volatile private var intensity = 1.0f
        @Volatile private var windSpeed = 0.0f
        @Volatile private var temperature = 3000 
        @Volatile private var environment = "Forest"
        @Volatile private var weather = "Clear"
        @Volatile private var logSize = 1.0f
        
        @Volatile private var envChanged = false

        fun setTemperature(temp: Int) { this.temperature = temp }
        fun setIntensity(i: Float) { this.intensity = i }
        fun setWindSpeed(s: Float) { this.windSpeed = s }
        fun setEnvironment(env: String) { 
            if (this.environment != env) {
                this.environment = env
                this.envChanged = true
            }
        }
        fun setWeather(w: String) { this.weather = w }
        fun setLogSize(s: Float) { this.logSize = s }

        // Data classes
        inner class Particle(
            var x: Float, var y: Float,
            var vx: Float, var vy: Float,
            var life: Float, var maxLife: Float,
            var size: Float,
            var color: Int,
            val type: String,
            var targetY: Float = 0f
        )
        inner class Star(var x: Float, var y: Float, var alpha: Float)
        inner class GrassBlade(
            var x: Float, var y: Float,
            var height: Float, var lean: Float,
            var color: Int,
            var swaySpeed: Float,
            var swayOffset: Float
        )
        inner class Ripple(
            var x: Float, var y: Float,
            var radius: Float,
            var maxRadius: Float,
            var alpha: Float,
            var life: Float
        )

        fun updateSize(w: Int, h: Int) {
            this.width = w
            this.height = h
            initStars()
            initGrass()
        }

        private fun initStars() {
            stars.clear()
            for (i in 0 until 150) {
                stars.add(Star(random.nextFloat() * width, random.nextFloat() * height * 0.75f, random.nextFloat()))
            }
        }

        private fun initGrass() {
            grassBlades.clear()
            if (environment != "Forest") return

            val horizonY = height * 0.75f
            for (i in 0 until 400) {
                val y = horizonY + random.nextFloat() * (height - horizonY)
                val depthFactor = (y - horizonY) / (height - horizonY)
                val r = (20 + random.nextInt(20))
                val g = (50 + depthFactor * 40 + random.nextInt(20)).toInt()
                val b = (20 + random.nextInt(20))
                grassBlades.add(GrassBlade(
                    x = random.nextFloat() * width,
                    y = y,
                    height = (30 + random.nextFloat() * 40) * (0.5f + depthFactor),
                    lean = (random.nextFloat() - 0.5f) * 10,
                    color = Color.rgb(r, g, b),
                    swaySpeed = 0.002f + random.nextFloat() * 0.003f,
                    swayOffset = random.nextFloat() * PI.toFloat() * 2
                ))
            }
            grassBlades.sortBy { it.y }
        }

        private fun getTemperatureColor(temp: Int): Int {
            return when {
                temp < 2000 -> Color.rgb(255, 60, 0)
                temp < 3500 -> Color.rgb(255, 140, 0)
                temp < 5000 -> Color.rgb(255, 200, 50)
                temp < 7500 -> Color.rgb(255, 255, 255)
                else -> Color.rgb(100, 200, 255)
            }
        }

        private fun spawnParticles() {
            if (weather == "Rainy") {
                if (random.nextFloat() < 0.5) return // 50% ?•ë¥ ë¡??Œí‹°???ì„± ?¤í‚µ
            }

            val centerX = width / 2f
            val centerY = height * 0.85f
            
            // Fire Particles
            val count = (3 * intensity).toInt()
            val baseColor = getTemperatureColor(temperature)

            for (i in 0 until count) {
                // 1. (?µì‹¬) ?•ê·œ ë¶„í¬ë¥??´ìš©???„ìž¬ ?¨ë„???¸ì°¨ë¥?ë§Œë“­?ˆë‹¤.
                //    ?œì??¸ì°¨ë¥?400 ?•ë„ë¡??¤ì •?˜ì—¬ ?‰ìƒ ë³€?”ì˜ ??„ ì¡°ì ˆ?©ë‹ˆ??
                val tempVariation = (random.nextGaussian() * 400).toInt()
                val particleTemp = (temperature + tempVariation).coerceIn(1000, 15000)

                // 2. ?¸ì°¨ê°€ ?ìš©??ê°œë³„ ?¨ë„(particleTemp)??ë§žëŠ” ?‰ìƒ??ê³„ì‚°?©ë‹ˆ??
                val particleColor = getTemperatureColor(particleTemp)

                // 3. ?Œí‹°?´ì„ ?ì„±?©ë‹ˆ??
                val angle = (random.nextFloat() - 0.5f) * PI * 0.5
                val speed = (2 + random.nextFloat() * 3) * 2.0 * (particleTemp / 4000.0) // ?ë„?ë„ ê°œë³„ ?¨ë„ë¥?ë°˜ì˜?˜ë©´ ???ì—°?¤ëŸ¬?€
                particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 60 * intensity,
                    y = centerY + (random.nextFloat() - 0.5f) * 20,
                    vx = (sin(angle) * 0.5 + windSpeed * 0.2).toFloat(),
                    vy = -speed.toFloat(),
                    life = 1.0f, maxLife = 1.0f,
                    size = (15 + random.nextFloat() * 20) * intensity,
                    color = particleColor, // 4. ë°©ê¸ˆ ê³„ì‚°??'ê°œë³„ ?‰ìƒ'??? ë‹¹?©ë‹ˆ??
                    type = "fire"
                ))
            }

            // Smoke
            if (random.nextFloat() < 0.2 * intensity) {
                particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 50, 
                    y = centerY - 40,
                    vx = (random.nextFloat() - 0.5 + windSpeed * 0.5).toFloat(), 
                    vy = -1f - random.nextFloat(),
                    life = 1.0f, maxLife = 1.0f, 
                    size = 20 + random.nextFloat() * 20,
                    color = Color.rgb(100, 100, 100), 
                    type = "smoke"
                ))
            }

            // Sparks
            if (random.nextFloat() < 0.1 * intensity) {
                particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 40,
                    y = centerY,
                    vx = ((random.nextFloat() - 0.5f) * 2 + windSpeed * 0.3).toFloat(),
                    vy = -3 - random.nextFloat() * 3,
                    life = 1.0f, maxLife = 1.0f,
                    size = 2 + random.nextFloat(),
                    color = Color.rgb(255, 220, 100),
                    type = "spark"
                ))
            }

            // Weather (Rain/Snow)
            if (weather == "Rain" || weather == "Snow") {
                val precipCount = if (weather == "Rain") 5 else 2
                val waterHorizonY = if (environment == "Beach") height * 0.6f else height.toFloat()
                
                for(i in 0 until precipCount) {
                    val targetY = if (environment == "Beach") {
                        waterHorizonY + random.nextFloat() * (height - waterHorizonY)
                    } else {
                        height + 10f
                    }
                    
                    particles.add(Particle(
                        x = random.nextFloat() * width,
                        y = -10f,
                        vx = (windSpeed + (random.nextFloat() - 0.5f)).toFloat(),
                        vy = if (weather == "Rain") (25 + random.nextFloat() * 5).toFloat() else (4 + random.nextFloat() * 2).toFloat(),
                        life = 100.0f, maxLife = 100.0f,
                        size = if (weather == "Rain") 20f else 5f,
                        color = Color.WHITE,
                        type = if (weather == "Rain") "rain" else "snow",
                        targetY = targetY
                    ))
                }
            }
        }

        override fun run() {
            while (running) {
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        if (envChanged) {
                            initGrass()
                            envChanged = false
                        }
                        draw(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        private fun draw(canvas: Canvas) {
            val time = System.currentTimeMillis()
            
            // --- Draw Background ---
            var bgTop = Color.parseColor("#051015")
            var bgBottom = Color.parseColor("#0a1a0f")
            
            when (environment) {
                "Forest" -> {
                    bgTop = Color.parseColor("#051015")
                    bgBottom = Color.parseColor("#0a1a0f")
                }
                "Beach" -> {
                    bgTop = Color.parseColor("#020510")
                    bgBottom = Color.parseColor("#0a0f20")
                }
                "Snow" -> {
                    bgTop = Color.parseColor("#0a0a10")
                    bgBottom = Color.parseColor("#e0e0ec")
                }
                "SimpleBlack" -> {
                    bgTop = Color.BLACK
                    bgBottom = Color.BLACK

                }
            }
            
            val bgPaint = Paint()
            bgPaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                bgTop, bgBottom, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Draw Stars
            if (weather == "Clear" || weather == "Windy") {
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                for (star in stars) {
                    if (random.nextFloat() > 0.99) star.alpha = random.nextFloat()
                    paint.alpha = (star.alpha * 255).toInt()
                    canvas.drawCircle(star.x, star.y, 3f, paint)
                }
            }

            // Beach Water
            if (environment == "Beach") {
                val horizonY = height * 0.6f
                val waterPaint = Paint()
                waterPaint.shader = LinearGradient(0f, horizonY, 0f, height.toFloat(),
                    Color.parseColor("#051025"), Color.parseColor("#0a1a30"), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, horizonY, width.toFloat(), height.toFloat(), waterPaint)
                
                // Reflection
                val centerX = width / 2f
                val centerY = height * 0.85f
                val reflectionY = centerY + 20
                val reflectionWidth = 100f * intensity
                
                val reflectPaint = Paint()
                reflectPaint.shader = RadialGradient(centerX, reflectionY, reflectionWidth,
                     Color.argb(100, 255, 150, 50), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                // Simple oval approximation
                canvas.drawCircle(centerX, reflectionY, reflectionWidth/2, reflectPaint)
                
                // Ripples
                val rippleIterator = ripples.iterator()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = Color.argb(50, 200, 220, 255)
                
                while(rippleIterator.hasNext()) {
                    val r = rippleIterator.next()
                    r.life -= 0.02f
                    r.radius += 0.5f
                    if (r.life <= 0) {
                        rippleIterator.remove()
                    } else {
                        paint.alpha = (r.life * 255).toInt().coerceIn(0, 255)
                        // Flattened oval for perspective
                        canvas.drawOval(r.x - r.radius, r.y - r.radius * 0.3f, 
                                        r.x + r.radius, r.y + r.radius * 0.3f, paint)
                    }
                }
            }

            // Forest Grass
            if (environment == "Forest") {
                val path = Path()
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                for (blade in grassBlades) {
                    val sway = sin(time * blade.swaySpeed + blade.swayOffset) * (blade.height * 0.2 + windSpeed * 5)
                    paint.color = blade.color
                    paint.strokeWidth = 2 + (blade.y / height) * 3
                    path.reset()
                    path.moveTo(blade.x, blade.y)
                    val cpX = blade.x + blade.lean + sway.toFloat()
                    val cpY = blade.y - blade.height * 0.5f
                    val endX = blade.x + blade.lean * 2 + sway.toFloat() * 1.5f
                    val endY = blade.y - blade.height
                    path.quadTo(cpX, cpY, endX, endY)
                    canvas.drawPath(path, paint)
                }
            }

            // --- Draw Logs ---
            val cx = width / 2f
            val cy = height * 0.85f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.color = Color.rgb(62, 39, 35)
            paint.strokeWidth = 2f
            
            // Use logSize scale
            val s = logSize
            
            canvas.drawRoundRect(cx - 40*s, cy, cx + 40*s, cy + 20*s, 10f*s, 10f*s, paint)
            // Additional logs for 3D look
            canvas.save()
            canvas.rotate(-30f, cx - 20*s, cy - 5*s)
            canvas.drawRoundRect(cx - 60*s, cy - 15*s, cx + 20*s, cy + 5*s, 10f*s, 10f*s, paint)
            canvas.restore()
            
            canvas.save()
            canvas.rotate(30f, cx + 20*s, cy - 5*s)
            canvas.drawRoundRect(cx - 20*s, cy - 15*s, cx + 60*s, cy + 5*s, 10f*s, 10f*s, paint)
            canvas.restore()

            // --- Draw Particles ---
            spawnParticles()
            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.life -= 0.015f
                p.x += p.vx
                p.y += p.vy
                
                // Removal conditions
                if (p.life <= 0 || p.y > height + 50 || p.x < -50 || p.x > width + 50) { 
                    // Rain hit water check
                    if ((p.type == "rain" || p.type == "snow") && environment == "Beach" && p.targetY != 0f && p.y >= p.targetY) {
                        if (random.nextFloat() > 0.3) {
                            ripples.add(Ripple(
                                x = p.x, y = p.targetY,
                                radius = 1f, maxRadius = 15 + random.nextFloat() * 10,
                                alpha = 1.0f, life = 1.0f
                            ))
                        }
                    }
                    iterator.remove()
                    continue 
                }

                val alpha = (p.life * 255).toInt().coerceIn(0, 255)
                paint.color = p.color
                
                if (p.type == "fire") {
                        paint.alpha = alpha
                        paint.style = Paint.Style.FILL
                        canvas.drawCircle(p.x, p.y, p.size, paint)
                } else if (p.type == "smoke") {
                    paint.alpha = (p.life * 50).toInt().coerceIn(0, 255)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                } else if (p.type == "rain") {
                    paint.color = Color.argb(150, 200, 200, 255)
                    paint.strokeWidth = 2f
                    canvas.drawLine(p.x, p.y, p.x + p.vx * 2, p.y + p.size, paint)
                } else if (p.type == "snow") {
                     p.x += (sin(time / 1000.0 + p.life * 10) * 0.5).toFloat()
                     paint.color = Color.WHITE
                     paint.alpha = 200
                     canvas.drawCircle(p.x, p.y, p.size, paint)
                } else {
                    paint.alpha = alpha
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
            }
            
            // Glow Overlay
            val glowColor = getTemperatureColor(temperature)
            val glowPaint = Paint()
            glowPaint.shader = RadialGradient(cx, cy - 50, 200f, 
                Color.argb(50, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)), 
                Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
        }
    }
}
