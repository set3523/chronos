import React from 'react';

interface AndroidCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AndroidCodeModal({ isOpen, onClose }: AndroidCodeModalProps) {
  if (!isOpen) return null;

  const kotlinCode = `package com.example.Chronoswallpaper

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.widget.RemoteViews
import kotlin.math.*
import java.util.Random

/* ==========================================
   [1] AndroidManifest.xml ?§Ï†ï
   ========================================== */
/*
<uses-feature android:name="android.software.live_wallpaper" android:required="true" />
<uses-feature android:name="android.software.app_widgets" android:required="true" />

<service
    android:name=".ChronosWallpaperService"
    android:label="Î™®Îã•Î∂?Î∞∞Í≤Ω"
    android:permission="android.permission.BIND_WALLPAPER">
    <intent-filter>
        <action android:name="android.service.wallpaper.WallpaperService" />
    </intent-filter>
    <meta-data
        android:name="android.service.wallpaper"
        android:resource="@xml/wallpaper" />
</service>

<receiver android:name=".ChronosWidgetProvider" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/Chronos_widget_info" />
</receiver>
*/

class ChronosWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ChronosEngine()
    }

    inner class ChronosEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { draw() }
        private var visible = true
        private val paint = Paint()
        private val random = Random()
        
        // ?çÏä§Ï≤?(?¥Î?ÏßÄ ?ïÌÉú??Î∂àÍΩÉ??ÏΩîÎìúÎ°??ùÏÑ±)
        private var fireBitmap: Bitmap? = null
        
        // State
        private val particles = ArrayList<Particle>()
        private val stars = ArrayList<Star>()
        private var width = 0
        private var height = 0
        
        inner class Particle(
            var x: Float, var y: Float,
            var vx: Float, var vy: Float,
            var life: Float, var maxLife: Float,
            var size: Float,
            var type: String // "fire", "smoke", "spark"
        )
        inner class Star(var x: Float, var y: Float, var alpha: Float)

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            // ?∏Î? ?¥Î?ÏßÄ ?åÏùº ?ÜÏù¥, ÏΩîÎìúÎ°?'ÎπõÎÇò??Î∂àÍΩÉ ?¥Î?ÏßÄ'Î•??ùÏÑ±?©Îãà??
            createFireTexture()
        }

        private fun createFireTexture() {
            val size = 64
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val p = Paint()
            
            // Ï§ëÏã¨?Ä Î∞ùÏ? ?∏Îûë -> ?∏Í≥Ω?Ä Î∂âÏ? Ï£ºÌô© -> ?ùÏ? ?¨Î™Ö??Í∑∏Îùº?∞Ïù¥??            val gradient = RadialGradient(
                size / 2f, size / 2f, size / 2f,
                intArrayOf(
                    Color.argb(255, 255, 255, 200), // Core (White-Yellow)
                    Color.argb(200, 255, 100, 0),   // Middle (Orange-Red)
                    Color.TRANSPARENT               // Edge
                ),
                floatArrayOf(0.1f, 0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            p.shader = gradient
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, p)
            fireBitmap = bmp
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) handler.post(drawRunner) else handler.removeCallbacks(drawRunner)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            initStars()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunner)
        }

        private fun initStars() {
            stars.clear()
            for (i in 0 until 80) {
                stars.add(Star(random.nextFloat() * width, random.nextFloat() * height * 0.6f, random.nextFloat()))
            }
        }

        private fun spawnParticles() {
            val centerX = width / 2f
            val centerY = height * 0.85f
            
            // Fire (?¥Î?ÏßÄ ?åÌã∞??
            for (i in 0 until 4) {
                val angle = (random.nextFloat() - 0.5f) * Math.PI * 0.4
                val speed = (3 + random.nextFloat() * 4) * 2.0
                particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 80,
                    y = centerY + (random.nextFloat() - 0.5f) * 20,
                    vx = (sin(angle) * 1.0).toFloat(),
                    vy = -speed.toFloat(),
                    life = 1.0f, maxLife = 1.0f,
                    size = (60 + random.nextFloat() * 60), // ÎπÑÌä∏Îß??¨Í∏∞
                    type = "fire"
                ))
            }
            // Spark
            if (random.nextFloat() < 0.3) {
                 particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 60, y = centerY,
                    vx = (random.nextFloat() - 0.5f) * 2, vy = -4f - random.nextFloat() * 3,
                    life = 1.0f, maxLife = 1.0f, size = 3 + random.nextFloat() * 3,
                    type = "spark"
                ))
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    // 1. Î∞∞Í≤Ω
                    val bgPaint = Paint()
                    bgPaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                        Color.parseColor("#051015"), Color.parseColor("#0a1a0f"), Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // 2. Î≥?                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    paint.shader = null
                    for (star in stars) {
                        if (random.nextFloat() > 0.98) star.alpha = random.nextFloat()
                        paint.alpha = (star.alpha * 200).toInt()
                        canvas.drawCircle(star.x, star.y, 2f + random.nextFloat(), paint)
                    }

                    // 3. ?•Ïûë (?®Ïàú ?¨Í∞Å???Ä??PathÎ°?Í±∞Ïπú ?êÎÇå ?úÌòÑ)
                    val cx = width / 2f
                    val cy = height * 0.85f
                    paint.color = Color.rgb(50, 30, 25)
                    paint.alpha = 255
                    
                    val logPath = Path()
                    // ?ºÏ™Ω ?•Ïûë
                    logPath.moveTo(cx - 60, cy + 20)
                    logPath.lineTo(cx + 20, cy - 10)
                    logPath.lineTo(cx + 30, cy + 10)
                    logPath.lineTo(cx - 50, cy + 40)
                    canvas.drawPath(logPath, paint)
                    // ?§Î•∏Ï™??•Ïûë
                    logPath.reset()
                    logPath.moveTo(cx + 60, cy + 20)
                    logPath.lineTo(cx - 20, cy - 10)
                    logPath.lineTo(cx - 30, cy + 10)
                    logPath.lineTo(cx + 50, cy + 40)
                    canvas.drawPath(logPath, paint)

                    // 4. ?åÌã∞??Í∑∏Î¶¨Í∏?                    spawnParticles()
                    val iterator = particles.iterator()
                    val bmp = fireBitmap
                    
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.life -= 0.02f
                        p.x += p.vx
                        p.y += p.vy
                        
                        if (p.life <= 0) { iterator.remove(); continue }

                        if (p.type == "fire" && bmp != null) {
                            // ÎπÑÌä∏Îß??¥Î?ÏßÄ) Í∑∏Î¶¨Í∏?- Í∞Ä???ºÌï© ?®Í≥º(Lighter)??Paint ?§Ï†ï??Î≥µÏû°?òÎ?Î°??åÌåå Î∏îÎ†å???¨Ïö©
                            paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
                            
                            // ?¨Í∏∞ Ï°∞Ï†à???ÑÌïú Matrix
                            val scale = p.life * (p.size / bmp.width)
                            val matrix = Matrix()
                            matrix.postScale(scale, scale)
                            matrix.postTranslate(p.x - (bmp.width * scale / 2), p.y - (bmp.height * scale / 2))
                            
                            canvas.drawBitmap(bmp, matrix, paint)
                        } else if (p.type == "spark") {
                            paint.color = Color.rgb(255, 200, 100)
                            paint.alpha = (p.life * 255).toInt()
                            canvas.drawCircle(p.x, p.y, p.size, paint)
                        }
                    }
                    
                    // 5. Í∏ÄÎ°úÏö∞ ?®Í≥º (?ÑÏ≤¥?ÅÏù∏ Î∂âÏ? Í∏∞Ïö¥)
                    val glowPaint = Paint()
                    glowPaint.shader = RadialGradient(cx, cy - 50, 200f, 
                        Color.argb(40, 255, 100, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawCircle(cx, cy - 50, 200f, glowPaint)
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, 16)
        }
    }
}

class ChronosWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val width = 500
            val height = 500
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // ?ÑÏ†Ø???§ÎÉÖ??Í∑∏Î¶¨Í∏?            drawSnapshot(canvas, width, height)

            val views = RemoteViews(context.packageName, R.layout.widget_Chronos)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            val intent = Intent(context, ChronosWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun drawSnapshot(canvas: Canvas, w: Int, h: Int) {
            // Î∞∞Í≤Ω
            val bgPaint = Paint()
            bgPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#051015"), Color.parseColor("#0a1a0f"), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

            // Î∂àÍΩÉ ?çÏä§Ï≤??ùÏÑ± (?ÑÏãú)
            val size = 80
            val fireBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(fireBmp)
            val p = Paint()
            val gradient = RadialGradient(size/2f, size/2f, size/2f,
                intArrayOf(Color.argb(255,255,255,200), Color.argb(200,255,100,0), Color.TRANSPARENT),
                floatArrayOf(0.1f, 0.4f, 1f), Shader.TileMode.CLAMP)
            p.shader = gradient
            c.drawCircle(size/2f, size/2f, size/2f, p)

            // Î∂àÍΩÉ Í∑∏Î¶¨Í∏?            val random = Random()
            val cx = w / 2f
            val cy = h * 0.8f
            
            // ?•Ïûë
            p.shader = null
            p.color = Color.rgb(60, 40, 30)
            canvas.drawRect(cx - 50, cy, cx + 50, cy + 30, p)

            // Î∂àÍΩÉ Î∞∞Ïπò
            for(i in 0 until 40) {
                val scale = 0.5f + random.nextFloat()
                val fx = cx + (random.nextFloat() - 0.5f) * 100
                val fy = cy - random.nextFloat() * 120
                
                val matrix = Matrix()
                matrix.postScale(scale, scale)
                matrix.postTranslate(fx - (size * scale / 2), fy - (size * scale / 2))
                canvas.drawBitmap(fireBmp, matrix, null)
            }
        }
    }
}`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
      <div className="bg-[#18181b] w-full max-w-4xl h-[80vh] rounded-2xl border border-white/10 flex flex-col shadow-2xl">
        <div className="p-6 border-b border-white/10 flex justify-between items-center">
          <div>
            <h2 className="text-xl font-bold text-white">?àÎìúÎ°úÏù¥???åÏä§ ÏΩîÎìú (Kotlin)</h2>
            <p className="text-xs text-gray-400 mt-1">Î¶¨Ïñº??Î∂àÍΩÉ ?®Í≥º(Procedural Bitmap)Í∞Ä ?ÅÏö©??Î≤ÑÏ†Ñ?ÖÎãà??</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-full text-gray-400 hover:text-white">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>
        
        <div className="flex-1 overflow-auto p-0 relative bg-[#0a0a0c]">
           <pre className="p-6 text-sm font-mono text-gray-300 leading-relaxed whitespace-pre select-text">
             {kotlinCode}
           </pre>
        </div>

        <div className="p-4 border-t border-white/10 bg-[#18181b] flex justify-end gap-3 rounded-b-2xl">
           <button 
             onClick={() => navigator.clipboard.writeText(kotlinCode)}
             className="px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-sm font-bold flex items-center gap-2"
           >
             <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>
             ÏΩîÎìú Î≥µÏÇ¨?òÍ∏∞
           </button>
           <button onClick={onClose} className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg text-sm">?´Í∏∞</button>
        </div>
      </div>
    </div>
  );
}
