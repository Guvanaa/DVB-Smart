package de.vvo.glassapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.vvo.glassapp.R
import de.vvo.glassapp.ui.components.GlassCard
import de.vvo.glassapp.ui.theme.DvbYellow

data class ChatMessage(val text: String, val isFromUser: Boolean)

@Composable
fun AssistantScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(ChatMessage("", false)) } // Placeholder for initial welcome
    val welcomeMessage = stringResource(R.string.assistant_welcome)

    LaunchedEffect(Unit) {
        if (messages[0].text.isEmpty()) {
            messages[0] = ChatMessage(welcomeMessage, false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                text = stringResource(R.string.assistant_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        // Input Area
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.assistant_hint),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userText = inputText
                            messages.add(ChatMessage(userText, true))
                            inputText = ""
                            // Simple mock response logic
                            messages.add(ChatMessage(getMockResponse(userText), false))
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = DvbYellow)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val textColor = Color.White

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        GlassCard(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 20.dp
            )
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp
            )
        }
    }
}

fun getMockResponse(input: String): String {
    val lowInput = input.lowercase()
    return when {
        lowInput.contains("hallo") || lowInput.contains("hi") -> "Hallo! Wie kann ich dir helfen?"
        lowInput.contains("bahn") || lowInput.contains("tram") || lowInput.contains("bus") ->
            "Ich schaue mal nach... Die nächste Linie 7 kommt in 3 Minuten am Pirnaischen Platz an."
        lowInput.contains("verspätung") -> "Aktuell gibt es leichte Verzögerungen auf der Linie 3 wegen einer Baustelle."
        lowInput.contains("danke") -> "Gerne geschehen! Hast du noch weitere Fragen?"
        else -> "Das ist eine gute Frage! Ich lerne ständig dazu, um dir besser bei deinen Fahrten in Dresden zu helfen."
    }
}
