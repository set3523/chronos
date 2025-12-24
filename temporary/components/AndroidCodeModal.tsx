import React from 'react';

interface AndroidCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function AndroidCodeModal({ isOpen, onClose }: AndroidCodeModalProps) {
  if (!isOpen) return null;

  const kotlinCode = `package com.example.bonfirewallpaper

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
   [1] AndroidManifest.xml 설정
   ========================================== */
/*
<uses-feature android:name="android.software.live_wallpaper" android:required="true" />
<uses-feature android:name="android.software.app_widgets" android:required="true" />

<service
    android:name=".BonfireWallpaperService"
    android:label="모닥불 배경"
    android:permission="android.permission.BIND_WALLPAPER">
    <intent-filter>
        <action android:name="android.service.wallpaper.WallpaperService" />
    </intent-filter>
    <meta-data
        android:name="android.service.wallpaper"
        android:resource="@xml/wallpaper" />
</service>

<receiver android:name=".BonfireWidgetProvider" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/bonfire_widget_info" />
</receiver>
*/

class BonfireWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return BonfireEngine()
    }

    inner class BonfireEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { draw() }
        private var visible = true
        private val paint = Paint()
        private val random = Random()
        
        // 텍스처 (이미지 형태의 불꽃을 코드로 생성)
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
            // 외부 이미지 파일 없이, 코드로 '빛나는 불꽃 이미지'를 생성합니다.
            createFireTexture()
        }

        private fun createFireTexture() {
            val size = 64
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val p = Paint()
            
            // 중심은 밝은 노랑 -> 외곽은 붉은 주황 -> 끝은 투명한 그라데이션
            val gradient = RadialGradient(
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
            
            // Fire (이미지 파티클)
            for (i in 0 until 4) {
                val angle = (random.nextFloat() - 0.5f) * Math.PI * 0.4
                val speed = (3 + random.nextFloat() * 4) * 2.0
                particles.add(Particle(
                    x = centerX + (random.nextFloat() - 0.5f) * 80,
                    y = centerY + (random.nextFloat() - 0.5f) * 20,
                    vx = (sin(angle) * 1.0).toFloat(),
                    vy = -speed.toFloat(),
                    life = 1.0f, maxLife = 1.0f,
                    size = (60 + random.nextFloat() * 60), // 비트맵 크기
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
                    // 1. 배경
                    val bgPaint = Paint()
                    bgPaint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                        Color.parseColor("#051015"), Color.parseColor("#0a1a0f"), Shader.TileMode.CLAMP)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // 2. 별
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    paint.shader = null
                    for (star in stars) {
                        if (random.nextFloat() > 0.98) star.alpha = random.nextFloat()
                        paint.alpha = (star.alpha * 200).toInt()
                        canvas.drawCircle(star.x, star.y, 2f + random.nextFloat(), paint)
                    }

                    // 3. 장작 (단순 사각형 대신 Path로 거친 느낌 표현)
                    val cx = width / 2f
                    val cy = height * 0.85f
                    paint.color = Color.rgb(50, 30, 25)
                    paint.alpha = 255
                    
                    val logPath = Path()
                    // 왼쪽 장작
                    logPath.moveTo(cx - 60, cy + 20)
                    logPath.lineTo(cx + 20, cy - 10)
                    logPath.lineTo(cx + 30, cy + 10)
                    logPath.lineTo(cx - 50, cy + 40)
                    canvas.drawPath(logPath, paint)
                    // 오른쪽 장작
                    logPath.reset()
                    logPath.moveTo(cx + 60, cy + 20)
                    logPath.lineTo(cx - 20, cy - 10)
                    logPath.lineTo(cx - 30, cy + 10)
                    logPath.lineTo(cx + 50, cy + 40)
                    canvas.drawPath(logPath, paint)

                    // 4. 파티클 그리기
                    spawnParticles()
                    val iterator = particles.iterator()
                    val bmp = fireBitmap
                    
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.life -= 0.02f
                        p.x += p.vx
                        p.y += p.vy
                        
                        if (p.life <= 0) { iterator.remove(); continue }

                        if (p.type == "fire" && bmp != null) {
                            // 비트맵(이미지) 그리기 - 가산 혼합 효과(Lighter)는 Paint 설정이 복잡하므로 알파 블렌딩 사용
                            paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
                            
                            // 크기 조절을 위한 Matrix
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
                    
                    // 5. 글로우 효과 (전체적인 붉은 기운)
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

class BonfireWidgetProvider : AppWidgetProvider() {
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

            // 위젯용 스냅샷 그리기
            drawSnapshot(canvas, width, height)

            val views = RemoteViews(context.packageName, R.layout.widget_bonfire)
            views.setImageViewBitmap(R.id.widget_image, bitmap)

            val intent = Intent(context, BonfireWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun drawSnapshot(canvas: Canvas, w: Int, h: Int) {
            // 배경
            val bgPaint = Paint()
            bgPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#051015"), Color.parseColor("#0a1a0f"), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

            // 불꽃 텍스처 생성 (임시)
            val size = 80
            val fireBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(fireBmp)
            val p = Paint()
            val gradient = RadialGradient(size/2f, size/2f, size/2f,
                intArrayOf(Color.argb(255,255,255,200), Color.argb(200,255,100,0), Color.TRANSPARENT),
                floatArrayOf(0.1f, 0.4f, 1f), Shader.TileMode.CLAMP)
            p.shader = gradient
            c.drawCircle(size/2f, size/2f, size/2f, p)

            // 불꽃 그리기
            val random = Random()
            val cx = w / 2f
            val cy = h * 0.8f
            
            // 장작
            p.shader = null
            p.color = Color.rgb(60, 40, 30)
            canvas.drawRect(cx - 50, cy, cx + 50, cy + 30, p)

            // 불꽃 배치
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
            <h2 className="text-xl font-bold text-white">안드로이드 소스 코드 (Kotlin)</h2>
            <p className="text-xs text-gray-400 mt-1">리얼한 불꽃 효과(Procedural Bitmap)가 적용된 버전입니다.</p>
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
             코드 복사하기
           </button>
           <button onClick={onClose} className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg text-sm">닫기</button>
        </div>
      </div>
    </div>
  );
}