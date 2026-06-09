package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ==========================================
// 1. NETWORK RELATION GRAPH WIDGET
// ==========================================

@Composable
fun NetworkGraphWidget(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    modifier: Modifier = Modifier
) {
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    
    // Fallback if empty
    val finalNodes = remember(nodes) {
        if (nodes.isEmpty()) {
            listOf(
                GraphNode("n1", "Primary Subject Focus", "Concept"),
                GraphNode("n2", "Related Sub-technology", "Technology"),
                GraphNode("n3", "Core System", "Policy"),
                GraphNode("n4", "Global Impact Factor", "Impact")
            )
        } else nodes
    }
    
    val finalEdges = remember(edges) {
        if (edges.isEmpty()) {
            listOf(
                GraphEdge("n1", "n2", "Enables"),
                GraphEdge("n1", "n3", "Governed by"),
                GraphEdge("n3", "n4", "Triggers")
            )
        } else edges
    }

    var activeNodeDetails by remember { mutableStateOf<GraphNode?>(null) }
    
    LaunchedEffect(selectedNodeId) {
        activeNodeDetails = finalNodes.find { it.id == selectedNodeId }
    }

    // Positions mapping (using radial/spring layout distribution on bounds)
    val positions = remember(finalNodes) {
        val calculated = mutableMapOf<String, Offset>()
        val size = finalNodes.size
        
        // Root or central first node
        if (size > 0) {
            calculated[finalNodes[0].id] = Offset(0.5f, 0.5f) // centered
        }
        
        // Distribute others in a circular pattern around center
        for (i in 1 until size) {
            val angle = (2 * Math.PI * (i - 1) / (size - 1)).toFloat()
            val radius = 0.3f // normalized offset radius
            val x = 0.5f + radius * cos(angle)
            val y = 0.5f + radius * sin(angle)
            calculated[finalNodes[i].id] = Offset(x, y)
        }
        calculated
    }

    // Dynamic scale and pulse
    val transition = rememberInfiniteTransition(label = "RadarPulse")
    val waveSize by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val waveAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // HOIST COLOR OPTIONS OUTSIDE CANVAS FOR COMPILATION COMPLIANCE
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorOutlineVariant = MaterialTheme.colorScheme.outlineVariant
    val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val densityScope = LocalDensity.current
    val minDistancePx = remember { with(densityScope) { 50.dp.toPx() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = colorPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Interactive Concept Web",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tap on entities to isolate connections, view relational descriptions, and evaluate structural logic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    .testTag("entity_network_canvas")
                    .pointerInput(finalNodes, positions) {
                        detectTapGestures { tapLoc ->
                            val cWidth = size.width
                            val cHeight = size.height
                            
                            // Find closest tapped node
                            var clicked: String? = null
                            var minDistance = minDistancePx
                            
                            finalNodes.forEach { node ->
                                val pos = positions[node.id] ?: Offset(0.5f, 0.5f)
                                val actualX = pos.x * cWidth
                                val actualY = pos.y * cHeight
                                val dist = sqrt((tapLoc.x - actualX) * (tapLoc.x - actualX) + (tapLoc.y - actualY) * (tapLoc.y - actualY))
                                if (dist < minDistance) {
                                    minDistance = dist
                                    clicked = node.id
                                }
                            }
                            selectedNodeId = if (clicked == selectedNodeId) null else clicked
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 1. Draw logical background research grid lines
                val gridColor = Color.Magenta.copy(alpha = 0.05f)
                val lineThickness = 1.dp.toPx()
                for (i in 1..9) {
                    val step = i / 10f
                    drawLine(gridColor, Offset(step * canvasWidth, 0f), Offset(step * canvasWidth, canvasHeight), lineThickness / 2f)
                    drawLine(gridColor, Offset(0f, step * canvasHeight), Offset(canvasWidth, step * canvasHeight), lineThickness / 2f)
                }

                // 2. Draw Connections (Lines)
                finalEdges.forEach { edge ->
                    val posSource = positions[edge.source]
                    val posTarget = positions[edge.target]
                    if (posSource != null && posTarget != null) {
                        val start = Offset(posSource.x * canvasWidth, posSource.y * canvasHeight)
                        val end = Offset(posTarget.x * canvasWidth, posTarget.y * canvasHeight)
                        
                        val isHighlighted = selectedNodeId == null || 
                                            selectedNodeId == edge.source || 
                                            selectedNodeId == edge.target
                                            
                        val strokeColor = if (isHighlighted) {
                            if (selectedNodeId != null) colorPrimary else colorOutlineVariant
                        } else {
                            colorOutlineVariant.copy(alpha = 0.15f)
                        }

                        // Draw curved path
                        val path = Path().apply {
                            moveTo(start.x, start.y)
                            val midX = (start.x + end.x) / 2f
                            val midY = (start.y + end.y) / 2f
                            quadraticTo(midX + 25f, midY - 25f, end.x, end.y)
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = if (selectedNodeId != null && isHighlighted) 3.dp.toPx() else 1.5.dp.toPx())
                        )
                    }
                }

                // 3. Draw Nodes (Points)
                finalNodes.forEach { node ->
                    val pos = positions[node.id] ?: Offset(0.5f, 0.5f)
                    val actualX = pos.x * canvasWidth
                    val actualY = pos.y * canvasHeight
                    
                    val isSelected = selectedNodeId == node.id
                    val isDirectlyConnected = selectedNodeId == null || isSelected || finalEdges.any { 
                        (it.source == selectedNodeId && it.target == node.id) || (it.target == selectedNodeId && it.source == node.id)
                    }

                    val colorSchemeColor = when (node.group.lowercase().trim()) {
                        "concept", "primary" -> colorPrimary
                        "technology", "tech" -> colorSecondary
                        "policy", "regulation", "law" -> colorTertiary
                        else -> Color(0xFF00B4D8)
                    }

                    val alpha = if (isDirectlyConnected) 1.0f else 0.15f
                    val outerColor = colorSchemeColor.copy(alpha = alpha)

                    // Expand ring for selected node
                    if (isSelected) {
                        drawCircle(
                            color = colorSchemeColor.copy(alpha = waveAlpha),
                            radius = 24.dp.toPx() + waveSize,
                            center = Offset(actualX, actualY)
                        )
                    }

                    // Draw outer border ring
                    drawCircle(
                        color = if (isSelected) Color.White else outerColor.copy(alpha = 0.3f),
                        radius = 22.dp.toPx(),
                        center = Offset(actualX, actualY)
                    )

                    // Draw center node solid circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colorSchemeColor.copy(alpha = alpha), colorSchemeColor.copy(alpha = alpha * 0.7f)),
                            center = Offset(actualX, actualY),
                            radius = 16.dp.toPx()
                        ),
                        radius = 16.dp.toPx(),
                        center = Offset(actualX, actualY)
                    )

                    // Simple clean white dot core
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 4.dp.toPx(),
                        center = Offset(actualX, actualY)
                    )
                }
            }
        }

        // Animated Info Card
        AnimatedVisibility(
            visible = activeNodeDetails != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.padding(12.dp)
        ) {
            activeNodeDetails?.let { node ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colorPrimary.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = node.group.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            IconButton(onClick = { selectedNodeId = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = node.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Dynamically find connections count
                        val relativeConnections = finalEdges.filter { it.source == node.id || it.target == node.id }
                        Text(
                            text = "Linked relations detected: ${relativeConnections.size} direct academic pipelines.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (relativeConnections.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            relativeConnections.forEach { edge ->
                                val targetNodeName = finalNodes.find { 
                                    it.id == (if (edge.source == node.id) edge.target else edge.source) 
                                }?.label ?: "Unknown Node"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowRight, contentDescription = null, tint = colorPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${edge.type} ➔ $targetNodeName",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. GEOGRAPHICAL COORDINATE MAP WIDGET
// ==========================================

@Composable
fun GeographicalMapWidget(
    points: List<GeoMapPoint>,
    modifier: Modifier = Modifier
) {
    val finalPoints = remember(points) {
        if (points.isEmpty()) {
            listOf(
                GeoMapPoint("Cambridge Research Center, UK", 52.2053, 0.1218, "Primary administrative mapping and database tracking core.", "Research Hub"),
                GeoMapPoint("Geneva Laboratory Space, CH", 46.2044, 6.1432, "High-energy testing node coordinating geographic findings.", "Collaborator Laboratory"),
                GeoMapPoint("Silicon Innovation Valley, CA", 37.7749, -122.4194, "Global commercialization epicentre and manufacturing corridor.", "Headquarters Division")
            )
        } else points
    }

    var selectedPointIdx by remember { mutableStateOf<Int?>(null) }

    // Map Animation Core
    val infinitePulse = rememberInfiniteTransition(label = "RadarRing")
    val pulseSize by infinitePulse.animateFloat(
        initialValue = 4.dp.value,
        targetValue = 20.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SizeAnimated"
    )

    // HOIST COLOR SCHEME ATTRIBUTES FOR COMPILATION ROBUSTNESS
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorOutlineVariant = MaterialTheme.colorScheme.outlineVariant
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val densityScope = LocalDensity.current
    val radarTapRadiusPx = remember { with(densityScope) { 35.dp.toPx() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Map guidance summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = colorSecondary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Spatial Geographic Mapping",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "A dynamic projection matrix representing global institutional centers, event coordinates, or regional research footprints.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .padding(horizontal = 8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("geospatial_map_canvas")
                    .pointerInput(finalPoints) {
                        detectTapGestures { tapLoc ->
                            val cWidth = size.width
                            val cHeight = size.height
                            val centerX = cWidth / 2f
                            val centerY = cHeight / 2f
                            
                            var clickedIndex: Int? = null
                            var minDistance = radarTapRadiusPx

                            finalPoints.forEachIndexed { idx, pt ->
                                // Project points onto Canvas coordinate mapping
                                // Max bounds Latitude: -90..90, Longitude: -180..180
                                val x = centerX + (pt.lng.toFloat() / 180f) * (cWidth / 2.2f)
                                val y = centerY - (pt.lat.toFloat() / 90f) * (cHeight / 2.3f)
                                
                                val d = sqrt((tapLoc.x - x) * (tapLoc.x - x) + (tapLoc.y - y) * (tapLoc.y - y))
                                if (d < minDistance) {
                                    minDistance = d
                                    clickedIndex = idx
                                }
                            }
                            selectedPointIdx = if (clickedIndex == selectedPointIdx) null else clickedIndex
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                // 1. Draw World Matrix Grid (concentric circles and meridians to represent the globe)
                val strokeColor = colorOutlineVariant.copy(alpha = 0.25f)
                val strokeWidthVal = 1.dp.toPx()
                
                // Equatorial lines
                drawLine(strokeColor, Offset(0f, centerY), Offset(canvasWidth, centerY), strokeWidthVal)
                drawLine(strokeColor, Offset(centerX, 0f), Offset(centerX, canvasHeight), strokeWidthVal)
                
                // Parallels of Latitude representation
                drawCircle(color = strokeColor, radius = canvasHeight / 5f, center = Offset(centerX, centerY), style = Stroke(strokeWidthVal / 2f))
                drawCircle(color = strokeColor, radius = canvasHeight / 2.8f, center = Offset(centerX, centerY), style = Stroke(strokeWidthVal / 2f))
                drawCircle(color = strokeColor, radius = canvasHeight / 1.8f, center = Offset(centerX, centerY), style = Stroke(strokeWidthVal / 2f))

                // Stylized outline dots for major continent approximations (Matrix Look)
                val dotColor = colorOnSurfaceVariant.copy(alpha = 0.12f)
                val dotLocations = listOf(
                    // Americas approx
                    Offset(-0.35f, -0.1f), Offset(-0.3f, 0.1f), Offset(-0.25f, 0.25f), Offset(-0.2f, 0.4f),
                    // Europe / Africa approx
                    Offset(0.05f, -0.2f), Offset(0.12f, 0.1f), Offset(0.08f, 0.3f), Offset(0.18f, 0.45f),
                    // Asia approx
                    Offset(0.35f, -0.25f), Offset(0.42f, -0.1f), Offset(0.38f, 0.05f),
                    // Oceania approx
                    Offset(0.45f, 0.3f), Offset(0.48f, 0.38f)
                )

                dotLocations.forEach { dotOffset ->
                    val dx = centerX + dotOffset.x * canvasWidth
                    val dy = centerY + dotOffset.y * canvasHeight
                    drawCircle(color = dotColor, radius = 6.dp.toPx(), center = Offset(dx, dy))
                }

                // 2. Draw Geo Location Markers
                finalPoints.forEachIndexed { index, pt ->
                    // Mercator approx projection to size
                    val x = centerX + (pt.lng.toFloat() / 180f) * (canvasWidth / 2.2f)
                    val y = centerY - (pt.lat.toFloat() / 90f) * (canvasHeight / 2.3f)
                    val isSelected = selectedPointIdx == index
                    
                    val color = if (isSelected) colorTertiary else colorPrimary

                    // Pulse echo
                    if (isSelected) {
                        drawCircle(
                            color = color.copy(alpha = 0.25f),
                            radius = pulseSize.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Solid ring anchor
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(x, y)
                    )

                    drawCircle(
                        color = color,
                        radius = 7.dp.toPx(),
                        center = Offset(x, y)
                    )

                    // Small mini coordinate label text representation
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Details Panel Card matching selection
        AnimatedVisibility(
            visible = selectedPointIdx != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.padding(12.dp)
        ) {
            selectedPointIdx?.let { idx ->
                val pt = finalPoints.getOrNull(idx)
                if (pt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colorSurfaceVariant.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, colorSecondary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = pt.significance,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorSecondary
                                    )
                                }

                                IconButton(onClick = { selectedPointIdx = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = pt.locationName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Coordinates: Lat ${"%.4f".format(pt.lat)}° · Lng ${"%.4f".format(pt.lng)}°",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pt.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 3. INTERACTIVE CHRONOLOGICAL TIMELINE
// ==========================================

@Composable
fun InteractiveTimelineWidget(
    milestones: List<TimelineMilestone>,
    modifier: Modifier = Modifier
) {
    if (milestones.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chronological milestone records available.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var isAscending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val sortedAndFilteredMilestones = remember(milestones, isAscending, searchQuery) {
        var baseList = milestones
        if (searchQuery.isNotBlank()) {
            baseList = milestones.filter { 
                it.event.contains(searchQuery, ignoreCase = true) || 
                it.details.contains(searchQuery, ignoreCase = true) ||
                it.date.contains(searchQuery, ignoreCase = true)
            }
        }
        if (isAscending) {
            baseList.sortedBy { it.date }
        } else {
            baseList.sortedByDescending { it.date }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Controls / Sort / Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Milestones") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            IconButton(
                onClick = { isAscending = !isAscending },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = if (isAscending) Icons.Default.VerticalAlignBottom else Icons.Default.VerticalAlignTop,
                    contentDescription = "Toggle Order Sorting",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sortedAndFilteredMilestones) { milestone ->
                var isExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Modern Timeline Column Anchor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = milestone.date,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Connecting continuous timeline vector thread line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(50.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Expandable narrative container card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isExpanded = !isExpanded }
                            .testTag("timeline_milestone_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = milestone.event,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand details details indicator",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = milestone.details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
