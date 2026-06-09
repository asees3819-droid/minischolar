package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun VoiceInputButton(
    isRecording: Boolean,
    onRecordingToggle: (Boolean) -> Unit,
    onPhraseSimulated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse"
    )

    val sineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * java.lang.Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SineWave"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    // Simulated phrases
    val simulationOptions = listOf(
        "Analyze the current state of Solid State Batteries and major automakers' roadmaps.",
        "What are the ethical implications of genetic editing techniques like CRISPR in agriculture?",
        "Summary of the timeline of Space Exploration milestone achievements from 1957 to present.",
        "Provide key research findings and statistics on the global impact of Microplastics in marine biology.",
        "What is the history, math breakthroughs, and future landscape of Quantum Computing?"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .testTag("voice_input_trigger")
        ) {
            if (isRecording) {
                // Pulse waves
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.4f * (1f - pulseRatio)),
                        radius = size.minDimension / 2f * pulseRatio,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Audio waveform simulation
                Canvas(modifier = Modifier.size(60.dp)) {
                    val width = size.width
                    val height = size.height
                    val points = 30
                    val step = width / points
                    
                    for (i in 0 until points) {
                        val x = i * step
                        val waveHeight = sin(i * 0.4f + sineOffset) * 15.dp.toPx()
                        val barHeight = waveHeight.coerceAtLeast(4f)
                        drawLine(
                            color = primaryColor,
                            start = androidx.compose.ui.geometry.Offset(x, height / 2 - barHeight),
                            end = androidx.compose.ui.geometry.Offset(x, height / 2 + barHeight),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    .clickable {
                        val nextState = !isRecording
                        onRecordingToggle(nextState)
                        if (nextState) {
                            // Pick a random simulation phrase and push after 2.5 seconds
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val phrase = simulationOptions.random()
                                onPhraseSimulated(phrase)
                                onRecordingToggle(false)
                            }, 2500)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input Toggle Button",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isRecording) "Listening... (Simulating speech input)" else "Tap mic to speak",
            style = MaterialTheme.typography.bodySmall,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
