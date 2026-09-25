package com.meowcha.game.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Titre façon "sticker" : contour blanc épais + remplissage coloré + ombre portée douce.
 * N'a besoin d'aucune police externe : l'effet vient entièrement du contour tracé.
 */
@Composable
fun Title(text: String, size: Int = 34, modifier: Modifier = Modifier, color: Color = Pink.Deep) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = LocalTextStyle.current.copy(
                drawStyle = Stroke(width = size * 0.3f, join = StrokeJoin.Round),
            ),
        )
        Text(
            text,
            fontSize = size.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = color,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, size * 0.07f), size * 0.18f),
            ),
        )
    }
}

/**
 * Bouton façon bonbon : relief 3D (lèvre plus sombre en bas), dégradé et reflet brillant en haut.
 */
@Composable
fun CuteButton(text: String, modifier: Modifier = Modifier, color: Color = Pink.Main, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val base = if (enabled) color else color.copy(alpha = 0.5f)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonPress",
    )
    Box(
        modifier
            .scale(pressScale)
            .height(56.dp)
            .shadow(if (enabled) 5.dp else 0.dp, shape, ambientColor = base, spotColor = base)
            .clip(shape)
            .background(base.shade(-0.3f))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(bottom = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(base.shade(0.14f), base))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(15.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f)),
            )
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.25f), Offset(0f, 2f), 3f),
                ),
            )
        }
    }
}

@Composable
fun Coins(amount: Int) {
    Row(
        Modifier
            .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = Pink.Deep.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Pink.Cream)))
            .border(1.dp, Pink.Light.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text("$amount", fontWeight = FontWeight.ExtraBold, color = Pink.Text)
    }
}

@Composable
fun Chip(text: String, background: Color = Color.White.copy(alpha = 0.9f), color: Color = Pink.Text) {
    Text(
        text,
        Modifier
            .shadow(2.dp, RoundedCornerShape(20.dp), ambientColor = Pink.Deep.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp,
    )
}

@Composable
fun RoundIconButton(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconButtonPress",
    )
    Box(
        Modifier
            .scale(pressScale)
            .size(44.dp)
            .shadow(3.dp, CircleShape, ambientColor = Pink.Deep.copy(alpha = 0.3f))
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color.White, Pink.Cream)))
            .border(1.dp, Pink.Light.copy(alpha = 0.7f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Pink.Deep, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun TopBar(title: String, coins: Int, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton("◀", onBack)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { Title(title, 26) }
        Coins(coins)
    }
}

/** Carte de base réutilisable : dégradé doux, ombre légère, fine bordure claire. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, corner: Int = 20, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(corner.dp)
    Box(
        modifier
            .shadow(5.dp, shape, ambientColor = Pink.Deep.copy(alpha = 0.18f), spotColor = Pink.Deep.copy(alpha = 0.28f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Pink.Cream)))
            .border(BorderStroke(1.dp, Pink.Light.copy(alpha = 0.55f)), shape),
        content = content,
    )
}
