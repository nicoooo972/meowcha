package com.meowcha.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Title(text: String, size: Int = 34, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        fontSize = size.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Cursive,
        color = Pink.Deep,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun CuteButton(text: String, modifier: Modifier = Modifier, color: Color = Pink.Main, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(4.dp),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
fun Coins(amount: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text("$amount", fontWeight = FontWeight.Bold, color = Pink.Text)
    }
}

@Composable
fun Chip(text: String, background: Color = Color.White.copy(alpha = 0.85f), color: Color = Pink.Text) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(20.dp)).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp,
    )
}

@Composable
fun RoundIconButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.White).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Pink.Deep, fontSize = 18.sp) }
}

@Composable
fun TopBar(title: String, coins: Int, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("◀", onBack)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { Title(title, 28) }
        Coins(coins)
    }
}
