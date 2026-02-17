package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import de.vvo.glassapp.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .fillMaxHeight(0.6f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Linienverlauf",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                itemsIndexed(stops) { index, stop ->
                    ThermometerStopItem(
                        stop = stop,
                        isFirst = index == 0,
                        isLast = index == stops.size - 1,
                        onClick = { onStopClick(stop) }
                    )
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thermometer Line and Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isFirst) 10.dp else 20.dp)
                    .background(if (isFirst) Color.Transparent else Color.Gray)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(DvbYellow, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isLast) 10.dp else 20.dp)
                    .background(if (isLast) Color.Transparent else Color.Gray)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Stop Info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stop.name, fontWeight = FontWeight.Medium)
            stop.time?.let {
                Text(text = it, fontSize = 12.sp, color = Color.Gray)
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
