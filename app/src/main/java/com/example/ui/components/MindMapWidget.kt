package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

data class MindNode(
    val id: Int,
    val text: String,
    val type: NodeType,
    val description: String,
    var xOffset: Float = 0f,
    var yOffset: Float = 0f
)

enum class NodeType {
    ROOT, INSIGHT, TIMELINE, SOURCE
}

@Composable
fun MindMapWidget(
    topic: String,
    insights: List<String>,
    timelineNames: List<String>,
    sourcesNames: List<String>,
    modifier: Modifier = Modifier
) {
    val items = remember(topic, insights, timelineNames, sourcesNames) {
        val rootNode = MindNode(0, topic, NodeType.ROOT, "Primary Workspace Focus Area")
        val nodes = mutableListOf(rootNode)
        
        var idCounter = 1
        
        // Add insights
        insights.take(4).forEachIndexed { index, text ->
            nodes.add(MindNode(idCounter++, text, NodeType.INSIGHT, "Insight #${index+1}: $text"))
        }

        // Add Timelines
        timelineNames.take(3).forEachIndexed { index, text ->
            nodes.add(MindNode(idCounter++, text, NodeType.TIMELINE, "Milestone Event: $text"))
        }

        // Add Sources
        sourcesNames.take(3).forEachIndexed { index, text ->
            nodes.add(MindNode(idCounter++, text, NodeType.SOURCE, "Reference Citation: $text"))
        }
        
        nodes
    }

    var selectedNode by remember { mutableStateOf(items.firstOrNull()) }
    
    // Wave pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "Waves")
    val pulseStrength by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseStrength"
    )

    val emerald = MaterialTheme.colorScheme.primary
    val azure = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Guidance Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Mind Map Node",
                    tint = emerald,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Interactive Logic Map",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap nodes on the research web to expand academic details, links, and contextual notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val height = size.height
                            val centerX = width / 2f
                            val centerY = height / 2f
                            val radiusOuter = Math.min(width, height) / 2.5f

                            // Compute positions
                            var foundNode: MindNode? = null
                            items.forEachIndexed { index, node ->
                                val x: Float
                                val y: Float
                                if (node.type == NodeType.ROOT) {
                                    x = centerX
                                    y = centerY
                                } else {
                                    val count = items.size - 1
                                    val angle = ((index - 1) * (2f * 3.14159265f / count))
                                    x = centerX + radiusOuter * cos(angle)
                                    y = centerY + radiusOuter * sin(angle)
                                }
                                
                                val distance = Math.hypot((offset.x - x).toDouble(), (offset.y - y).toDouble())
                                if (distance <= 45.dp.toPx()) {
                                    foundNode = node
                                }
                            }
                            if (foundNode != null) {
                                selectedNode = foundNode
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f
                val radiusOuter = size.minDimension / 2.5f

                // Draw background scholarly research grids
                val cols = 8
                val rows = 8
                val colSpacing = width / cols
                val rowSpacing = height / rows
                for (i in 0..cols) {
                    drawLine(gridColor, Offset(i * colSpacing, 0f), Offset(i * colSpacing, height), 1f)
                }
                for (j in 0..rows) {
                    drawLine(gridColor, Offset(0f, j * rowSpacing), Offset(width, j * rowSpacing), 1f)
                }

                // First pass: Draw connections with Beziers
                items.forEachIndexed { index, node ->
                    if (node.type != NodeType.ROOT) {
                        val count = items.size - 1
                        val angle = ((index - 1) * (2f * 3.14159265f / count))
                        val endX = centerX + radiusOuter * cos(angle)
                        val endY = centerY + radiusOuter * sin(angle)

                        val path = Path().apply {
                            moveTo(centerX, centerY)
                            cubicTo(
                                centerX + (endX - centerX) * 0.5f, centerY,
                                centerX, centerY + (endY - centerY) * 0.5f,
                                endX, endY
                            )
                        }

                        val strokeColor = when (node.type) {
                            NodeType.INSIGHT -> emerald.copy(alpha = 0.6f)
                            NodeType.TIMELINE -> azure.copy(alpha = 0.6f)
                            NodeType.SOURCE -> tertiary.copy(alpha = 0.6f)
                            else -> emerald
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = if (selectedNode?.id == node.id) 3.dp.toPx() else 1.5.dp.toPx()
                            )
                        )
                    }
                }

                // Second pass: Draw nodes
                items.forEachIndexed { index, node ->
                    val x: Float
                    val y: Float
                    if (node.type == NodeType.ROOT) {
                        x = centerX
                        y = centerY
                    } else {
                        val count = items.size - 1
                        val angle = ((index - 1) * (2f * 3.14159265f / count))
                        x = centerX + radiusOuter * cos(angle)
                        y = centerY + radiusOuter * sin(angle)
                    }

                    val isSelected = selectedNode?.id == node.id
                    val nodeColor = when (node.type) {
                        NodeType.ROOT -> emerald
                        NodeType.INSIGHT -> azure
                        NodeType.TIMELINE -> tertiary
                        NodeType.SOURCE -> AccentAzure
                    }

                    // Pulse outer circle for selected node
                    if (isSelected) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.3f),
                            radius = 28.dp.toPx() + pulseStrength,
                        )
                    }

                    // Main circular node
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(nodeColor, nodeColor.copy(alpha = 0.8f)),
                            center = Offset(x, y),
                            radius = 20.dp.toPx()
                        ),
                        radius = 20.dp.toPx(),
                        center = Offset(x, y)
                    )

                    // Simple white core
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Focus Node Description Card
        selectedNode?.let { node ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = when (node.type) {
                        NodeType.ROOT -> emerald
                        NodeType.INSIGHT -> azure
                        NodeType.TIMELINE -> tertiary
                        NodeType.SOURCE -> AccentAzure
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (node.type) {
                                NodeType.ROOT -> "Central Topic Coordinate"
                                NodeType.INSIGHT -> "Scientific Insight Insight Value"
                                NodeType.TIMELINE -> "Milestone Evolution"
                                NodeType.SOURCE -> "Reliable Citation Reference"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (node.type) {
                                NodeType.ROOT -> emerald
                                NodeType.INSIGHT -> azure
                                NodeType.TIMELINE -> tertiary
                                NodeType.SOURCE -> AccentAzure
                            },
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = node.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = node.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
