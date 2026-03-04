package de.vvo.glassapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.vvo.glassapp.data.model.Departure
import de.vvo.glassapp.data.model.Stop
import de.vvo.glassapp.data.model.StopPoint
import de.vvo.glassapp.data.model.VehiclePin
import de.vvo.glassapp.ui.theme.DvbYellow

@Composable
fun VehicleListSheet(
    pins: List<VehiclePin>,
    onVehicleClick: (VehiclePin) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(pins) { _, pin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVehicleClick(pin) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when {
                        (pin.punctuality ?: 0) < 0 -> Color(0xFF4CAF50)
                        (pin.punctuality ?: 0) == 0 -> Color(0xFFFFCC00)
                        else -> Color(0xFFF44336)
                    }

                    Box(
                        modifier = Modifier.background(statusColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(pin.line, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pin.direction, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        val delay = pin.punctuality ?: 0
                        val delayText = if (delay > 0) "+$delay min" else if (delay < 0) "$delay min" else "pünktlich"
                        Text(delayText, color = if (delay > 0) Color(0xFFF44336) else if (delay < 0) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StopDetailSheet(
    stop: Stop,
    departures: List<Departure>,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().heightIn(max = 450.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stop.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(stop.place ?: "Dresden", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(departures) { _, dep ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(DvbYellow, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when {
                                    dep.mot?.lowercase()?.contains("bus") == true -> Icons.Default.DirectionsBus
                                    dep.mot?.lowercase()?.contains("tram") == true -> Icons.Default.Tram
                                    else -> Icons.Default.DirectionsRailway
                                }
                                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(dep.lineName, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dep.direction, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dep.realTime ?: dep.scheduledTime, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (dep.delay != null && dep.delay != 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val delayText = if (dep.delay > 0) "+${dep.delay}" else dep.delay.toString()
                                    Text(
                                        text = "$delayText min",
                                        color = if (dep.delay > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                } else if (dep.realTime != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("pünktlich", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteDetailSheet(
    stops: List<StopPoint>,
    line: String?,
    onStopClick: (StopPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth().heightIn(max = 450.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (line != null) {
                    Box(
                        modifier = Modifier
                            .background(DvbYellow, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(line, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text("Verlauf", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn {
                itemsIndexed(stops) { index, stop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStopClick(stop) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Fieberthermometer (Thermometer) logic
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(32.dp)
                        ) {
                            val isFuture = stop.time != null // Simple heuristic

                            // Pulse animation for current/first stop
                            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f, targetValue = 1.3f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(1500),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        if (index == 0) {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                        }
                                    }
                                    .background(
                                        if (index == 0) DvbYellow
                                        else if (isFuture) Color.White
                                        else Color.White.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                            )
                            if (index < stops.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(52.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    if (isFuture) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                                                    if (isFuture) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                                                )
                                            ),
                                            androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stop.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                stop.time ?: "--:--",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (stop.delay != null && stop.delay != 0) {
                            Text(
                                if (stop.delay > 0) "+${stop.delay}" else stop.delay.toString(),
                                color = if (stop.delay > 0) Color.Red else Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
