package com.set.Chronos

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun RainEffect() {
    val particles = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(Unit) {
        for (i in 0..100) {
            particles.add(Offset(Random.nextFloat(), Random.nextFloat()))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rain_transition")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "rain_y_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        particles.forEach { particle ->
            val x = particle.x * canvasWidth
            val y = (particle.y * canvasHeight + yOffset) % canvasHeight
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(x, y),
                end = Offset(x, y + 40),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun SnowEffect() {
    val particles = remember { mutableStateListOf<Pair<Offset, Float>>() } // (position, radius)
    LaunchedEffect(Unit) {
        for (i in 0..100) {
            particles.add(Offset(Random.nextFloat(), Random.nextFloat()) to Random.nextFloat() * 4f + 2f)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "snow_transition")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(tween(5000), RepeatMode.Restart),
        label = "snow_y_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        particles.forEach { (particle, radius) ->
            val x = particle.x * canvasWidth
            val y = (particle.y * canvasHeight + yOffset) % canvasHeight
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun ThunderEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "thunder_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, delayMillis = 3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thunder_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = alpha))
    )
}
