package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.vvo.glassapp.data.model.Departure
import de.vvo.glassapp.data.model.Stop
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow

@Composable
fun StopDetailSheet(stop: Stop, departures: List<Departure>) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stop.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stop.place ?: "Dresden",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (departures.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DvbYellow)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(departures) { departure ->
                        DepartureListItem(departure)
                    }
                }
            }
        }
    }
}

@Composable
fun DepartureListItem(departure: Departure) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = DvbYellow,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 40.dp, height = 24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = departure.lineName,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = departure.direction,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
        Text(
            text = departure.scheduledTime,
            color = DvbYellow,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
        )
    }
}
