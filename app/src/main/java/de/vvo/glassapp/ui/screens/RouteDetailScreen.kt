package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.vvo.glassapp.R
import de.vvo.glassapp.data.model.StopPoint
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow
import de.vvo.glassapp.ui.theme.GreenPunctual
import de.vvo.glassapp.ui.theme.RedLate

@Composable
fun RouteDetailSheet(stops: List<StopPoint>, onStopClick: (StopPoint) -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.route_gradient),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (stops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DvbYellow)
                }
            } else {
                LazyColumn {
                    itemsIndexed(stops) { index, stop ->
                        // Simple logic for isPassed (if time is provided and it's not the first/last, normally API would handle this but we can approximate)
                        ThermometerStopItem(
                            stop = stop,
                            isFirst = index == 0,
                            isLast = index == stops.size - 1,
                            isPassed = false, // In a real app, calculate based on vehicle position
                            onClick = { onStopClick(stop) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThermometerStopItem(
    stop: StopPoint,
    isFirst: Boolean,
    isLast: Boolean,
    isPassed: Boolean = false,
    onClick: () -> Unit
) {
    val opacity = if (isPassed) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .alpha(opacity),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thermometer Line and Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(30.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isFirst) 12.dp else 24.dp)
                    .background(if (isFirst) Color.Transparent else Color.White.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(DvbYellow, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (isLast) 12.dp else 24.dp)
                    .background(if (isLast) Color.Transparent else Color.White.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Stop Info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            stop.time?.let {
                Text(text = it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        // Delay Info
        stop.delay?.let { delay ->
            val color = when {
                delay < 0 -> GreenPunctual
                delay == 0 -> DvbYellow
                else -> RedLate
            }
            val text = when {
                delay == 0 -> stringResource(id = R.string.punctual)
                delay > 0 -> "+$delay"
                else -> "$delay"
            }
            Text(
                text = text,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
