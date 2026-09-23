package com.meowcha.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.meowcha.game.game.Accessory
import com.meowcha.game.game.CatCustomer
import com.meowcha.game.game.FurPattern
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.IngredientKind
import com.meowcha.game.game.Mood
import com.meowcha.game.game.Mug
import com.meowcha.game.game.MugPattern
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF4E342E)
private val Blush = Color(0x88FF80AB)
private val InnerEar = Color(0xFFF8BBD0)

/** Éclaircit (f > 0) ou assombrit (f < 0) une couleur : sert à donner du volume. */
fun Color.shade(f: Float): Color = if (f >= 0f) {
    Color(red + (1f - red) * f, green + (1f - green) * f, blue + (1f - blue) * f, alpha)
} else {
    Color(red * (1f + f), green * (1f + f), blue * (1f + f), alpha)
}

private fun Color.luma(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Paramètres d'animation du chat (respiration, clignement, queue). */
data class CatAnim(val breath: Float = 0f, val blink: Float = 0f, val tail: Float = 0f)

/* ===================================== CHAT ===================================== */

/**
 * Chat en volume : dégradés de lumière (source en haut à gauche), ombre portée,
 * reflets dans les yeux, respiration, clignement des yeux et queue qui remue.
 */
@Composable
fun CatPortrait(cat: CatCustomer, mood: Mood, modifier: Modifier = Modifier, animated: Boolean = true) {
    if (!animated) {
        Canvas(modifier) { drawCat(cat, mood, CatAnim()) }
        return
    }
    val inf = rememberInfiniteTransition(label = "cat")
    val breath by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "breath")
    val tailSpeed = if (mood == Mood.IMPATIENT) 500 else 1400
    val tail by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(tailSpeed), RepeatMode.Reverse), label = "tail")
    // Clignement des yeux à intervalle aléatoire
    val blink = remember { Animatable(0f) }
    LaunchedEffect(cat.id) {
        while (true) {
            delay(Random.nextLong(1800, 4500))
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(110))
        }
    }
    Canvas(modifier) { drawCat(cat, mood, CatAnim(breath, blink.value, tail)) }
}

fun DrawScope.drawCat(cat: CatCustomer, mood: Mood, anim: CatAnim = CatAnim()) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val r = minOf(w, h) * 0.30f
    val breathe = anim.breath * r * 0.03f
    val cy = h * 0.46f - breathe
    val fur = cat.fur
    val outline = Stroke(width = r * 0.04f)
    val light = Offset(cx - r * 0.6f, cy - r * 0.8f) // source de lumière

    // Ombre portée sur le comptoir
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent), Offset(cx, cy + r * 2.25f), r * 1.4f),
        Offset(cx - r * 1.4f, cy + r * 2.0f), Size(r * 2.8f, r * 0.5f),
    )

    // Queue (derrière le corps), qui balance
    val tailBase = Offset(cx + r * 0.85f, cy + r * 1.9f)
    val swing = anim.tail * r * 0.35f
    val tailPath = Path().apply {
        moveTo(tailBase.x, tailBase.y)
        cubicTo(tailBase.x + r * 0.9f, tailBase.y - r * 0.1f, tailBase.x + r * 0.6f + swing, tailBase.y - r * 1.0f,
            tailBase.x + r * 0.35f + swing * 1.4f, tailBase.y - r * 1.35f)
    }
    drawPath(tailPath, Ink, style = Stroke(r * 0.3f, cap = StrokeCap.Round))
    drawPath(tailPath, Brush.linearGradient(listOf(fur.shade(0.2f), fur.shade(-0.2f)), tailBase, Offset(tailBase.x + r, tailBase.y - r)),
        style = Stroke(r * 0.22f, cap = StrokeCap.Round))

    // Corps (volume par dégradé radial)
    val body = Rect(cx - r * 1.05f, cy + r * 0.55f, cx + r * 1.05f, cy + r * 2.2f + breathe)
    val bodyBrush = Brush.radialGradient(
        listOf(fur.shade(0.35f), fur, fur.shade(-0.28f)),
        center = Offset(body.left + body.width * 0.35f, body.top + body.height * 0.3f),
        radius = body.width * 0.9f,
    )
    drawRoundRect(bodyBrush, body.topLeft, body.size, CornerRadius(r * 0.9f))
    if (cat.pattern == FurPattern.TUXEDO) {
        drawOval(Brush.verticalGradient(listOf(cat.accent, cat.accent.shade(-0.12f)), body.top, body.bottom),
            Offset(cx - r * 0.45f, cy + r * 0.7f), Size(r * 0.9f, r * 1.4f))
    }
    drawRoundRect(Ink, body.topLeft, body.size, CornerRadius(r * 0.9f), style = outline)
    // Ombre de la tête sur le corps (occlusion ambiante)
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent), Offset(cx, cy + r * 0.9f), r * 0.9f),
        Offset(cx - r * 0.9f, cy + r * 0.6f), Size(r * 1.8f, r * 0.6f),
    )

    // Oreilles
    fun ear(sign: Float) {
        val base1 = Offset(cx + sign * r * 0.25f, cy - r * 0.8f)
        val base2 = Offset(cx + sign * r * 0.95f, cy - r * 0.35f)
        val tip = Offset(cx + sign * r * 0.85f, cy - r * 1.3f)
        val p = Path().apply { moveTo(base1.x, base1.y); lineTo(tip.x, tip.y); lineTo(base2.x, base2.y); close() }
        val earColor = if (cat.pattern == FurPattern.POINTS) cat.accent else fur
        drawPath(p, Brush.linearGradient(listOf(earColor.shade(0.25f), earColor.shade(-0.2f)), tip, base2))
        drawPath(p, Ink, style = outline)
        val inner = Path().apply {
            moveTo(base1.x + (tip.x - base1.x) * 0.2f + sign * r * 0.08f, base1.y + r * 0.02f)
            lineTo(tip.x - sign * r * 0.04f, tip.y + r * 0.2f)
            lineTo(base2.x - sign * r * 0.12f, base2.y - r * 0.12f)
            close()
        }
        drawPath(inner, Brush.linearGradient(listOf(InnerEar.shade(-0.15f), InnerEar.shade(0.3f)), tip, base1))
    }
    ear(-1f); ear(1f)

    // Tête
    val head = Rect(cx - r * 1.1f, cy - r, cx + r * 1.1f, cy + r * 0.95f)
    drawOval(
        Brush.radialGradient(listOf(fur.shade(0.4f), fur, fur.shade(-0.3f)), Offset(head.left + head.width * 0.35f, head.top + head.height * 0.3f), head.width * 0.8f),
        head.topLeft, head.size,
    )

    // Motifs du pelage
    val headPath = Path().apply { addOval(head) }
    clipPath(headPath) {
        when (cat.pattern) {
            FurPattern.TABBY -> for (i in -1..1) {
                drawLine(cat.accent, Offset(cx + i * r * 0.25f, cy - r), Offset(cx + i * r * 0.2f, cy - r * 0.55f), r * 0.1f, StrokeCap.Round)
                drawLine(cat.accent, Offset(cx - r * 1.1f, cy + i * r * 0.15f), Offset(cx - r * 0.75f, cy + i * r * 0.12f), r * 0.08f, StrokeCap.Round)
                drawLine(cat.accent, Offset(cx + r * 1.1f, cy + i * r * 0.15f), Offset(cx + r * 0.75f, cy + i * r * 0.12f), r * 0.08f, StrokeCap.Round)
            }
            FurPattern.CALICO -> {
                drawCircle(cat.accent, r * 0.55f, Offset(cx - r * 0.8f, cy - r * 0.6f))
                drawCircle(Color(0xFF5D4037), r * 0.45f, Offset(cx + r * 0.9f, cy - r * 0.4f))
            }
            FurPattern.TUXEDO -> drawOval(cat.accent, Offset(cx - r * 0.55f, cy + r * 0.05f), Size(r * 1.1f, r * 1.2f))
            FurPattern.POINTS -> drawOval(
                Brush.radialGradient(listOf(cat.accent.copy(alpha = 0.7f), Color.Transparent), Offset(cx, cy + r * 0.3f), r * 0.7f),
                Offset(cx - r * 0.7f, cy - r * 0.3f), Size(r * 1.4f, r * 1.2f),
            )
            FurPattern.PLAIN -> {}
        }
        // Ombrage global par-dessus les motifs (garde le relief)
        drawOval(
            Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.18f)), Offset(head.left + head.width * 0.4f, head.top + head.height * 0.35f), head.width * 0.75f),
            head.topLeft, head.size,
        )
        // Reflet de lumière sur le haut de la tête
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), light + Offset(r * 0.3f, r * 0.2f), r * 0.6f),
            Offset(light.x - r * 0.3f, light.y - r * 0.2f), Size(r * 1.2f, r * 0.8f),
        )
    }
    drawOval(Ink, head.topLeft, head.size, style = outline)

    // Museau bombé (deux joues)
    val muzzle = fur.shade(0.25f)
    for (s in listOf(-1f, 1f)) {
        drawCircle(
            Brush.radialGradient(listOf(muzzle.shade(0.3f), muzzle, muzzle.shade(-0.08f)), Offset(cx + s * r * 0.14f, cy + r * 0.3f), r * 0.26f),
            r * 0.24f, Offset(cx + s * r * 0.16f, cy + r * 0.36f),
        )
    }

    // Yeux
    val eyeY = cy - r * 0.05f
    val eyeDx = r * 0.45f
    val blinkScale = 1f - anim.blink
    for (s in listOf(-1f, 1f)) {
        val c = Offset(cx + s * eyeDx, eyeY)
        val closedArc = mood == Mood.DELIGHTED || blinkScale < 0.25f
        when {
            closedArc && mood != Mood.SAD -> {
                val p = Path().apply {
                    moveTo(c.x - r * 0.18f, c.y + r * 0.05f)
                    quadraticBezierTo(c.x, c.y - r * 0.2f, c.x + r * 0.18f, c.y + r * 0.05f)
                }
                drawPath(p, Ink, style = Stroke(r * 0.07f, cap = StrokeCap.Round))
            }
            mood == Mood.SAD -> {
                val p = Path().apply {
                    moveTo(c.x - r * 0.18f, c.y - r * 0.05f)
                    quadraticBezierTo(c.x, c.y + r * 0.15f, c.x + r * 0.18f, c.y - r * 0.05f)
                }
                drawPath(p, Ink, style = Stroke(r * 0.07f, cap = StrokeCap.Round))
                drawOval(
                    Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF4FC3F7)), c.y + r * 0.1f, c.y + r * 0.3f),
                    Offset(c.x + s * r * 0.08f, c.y + r * 0.1f), Size(r * 0.1f, r * 0.2f),
                )
            }
            else -> {
                val eh = when (mood) { Mood.IMPATIENT -> r * 0.22f; Mood.WAITING -> r * 0.3f; else -> r * 0.36f } * blinkScale
                val ew = r * 0.32f
                val eye = Rect(c.x - ew / 2, c.y - eh / 2 - r * 0.02f, c.x + ew / 2, c.y + eh / 2 - r * 0.02f)
                drawOval(Ink, eye.topLeft - Offset(r * 0.02f, r * 0.02f), Size(eye.width + r * 0.04f, eye.height + r * 0.04f))
                drawOval(
                    Brush.radialGradient(listOf(cat.eyes.shade(0.45f), cat.eyes, cat.eyes.shade(-0.35f)), Offset(c.x, eye.bottom - eh * 0.2f), ew * 0.7f),
                    eye.topLeft, eye.size,
                )
                val pw = if (mood == Mood.IMPATIENT) r * 0.07f else r * 0.15f
                drawOval(Ink, Offset(c.x - pw / 2, eye.top + eh * 0.12f), Size(pw, eh * 0.76f))
                if (blinkScale > 0.5f) {
                    drawCircle(Color.White, r * 0.065f, Offset(c.x + r * 0.05f, eye.top + eh * 0.3f))
                    drawCircle(Color.White.copy(alpha = 0.8f), r * 0.03f, Offset(c.x - r * 0.06f, eye.bottom - eh * 0.25f))
                }
                if (mood == Mood.IMPATIENT) {
                    drawLine(Ink, Offset(c.x - s * r * 0.2f, c.y - r * 0.26f), Offset(c.x + s * r * 0.18f, c.y - r * 0.16f), r * 0.06f, StrokeCap.Round)
                }
            }
        }
    }

    // Joues roses
    drawOval(Brush.radialGradient(listOf(Blush, Color.Transparent), Offset(cx - r * 0.68f, cy + r * 0.3f), r * 0.22f),
        Offset(cx - r * 0.9f, cy + r * 0.18f), Size(r * 0.44f, r * 0.26f))
    drawOval(Brush.radialGradient(listOf(Blush, Color.Transparent), Offset(cx + r * 0.68f, cy + r * 0.3f), r * 0.22f),
        Offset(cx + r * 0.46f, cy + r * 0.18f), Size(r * 0.44f, r * 0.26f))

    // Nez avec reflet
    val nose = Path().apply {
        moveTo(cx - r * 0.1f, cy + r * 0.15f); lineTo(cx + r * 0.1f, cy + r * 0.15f); lineTo(cx, cy + r * 0.27f); close()
    }
    drawPath(nose, Brush.verticalGradient(listOf(Color(0xFFFFA8C5), Color(0xFFEC5E8E)), cy + r * 0.15f, cy + r * 0.27f))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.025f, Offset(cx - r * 0.03f, cy + r * 0.18f))

    // Bouche
    if (mood == Mood.SAD || mood == Mood.IMPATIENT) {
        val frown = Path().apply {
            moveTo(cx - r * 0.14f, cy + r * 0.44f)
            quadraticBezierTo(cx, cy + r * 0.32f, cx + r * 0.14f, cy + r * 0.44f)
        }
        drawPath(frown, Ink, style = Stroke(r * 0.05f, cap = StrokeCap.Round))
    } else {
        val mouth = Path().apply {
            moveTo(cx - r * 0.2f, cy + r * 0.33f)
            quadraticBezierTo(cx - r * 0.1f, cy + r * 0.45f, cx, cy + r * 0.29f)
            quadraticBezierTo(cx + r * 0.1f, cy + r * 0.45f, cx + r * 0.2f, cy + r * 0.33f)
        }
        if (mood == Mood.DELIGHTED) {
            val open = Path().apply {
                moveTo(cx - r * 0.12f, cy + r * 0.36f)
                quadraticBezierTo(cx, cy + r * 0.62f, cx + r * 0.12f, cy + r * 0.36f)
                close()
            }
            drawPath(open, Color(0xFF8D2E4A))
            drawOval(Color(0xFFFF8A80), Offset(cx - r * 0.07f, cy + r * 0.43f), Size(r * 0.14f, r * 0.1f))
        }
        drawPath(mouth, Ink, style = Stroke(r * 0.05f, cap = StrokeCap.Round))
    }

    // Moustaches
    val whisker = if (fur.luma() < 0.3f) Color(0xFFEEEEEE) else Ink.copy(alpha = 0.6f)
    for (s in listOf(-1f, 1f)) for (k in -1..1) {
        drawLine(
            whisker,
            Offset(cx + s * r * 0.5f, cy + r * 0.32f + k * r * 0.06f),
            Offset(cx + s * r * 1.25f, cy + r * 0.24f + k * r * 0.14f),
            r * 0.022f, StrokeCap.Round,
        )
    }

    // Accessoires
    when (cat.accessory) {
        Accessory.BOW -> drawBow(Offset(cx + r * 0.7f, cy - r * 0.85f), r * 0.32f, cat.accessoryColor)
        Accessory.FLOWER -> drawFlower(Offset(cx - r * 0.7f, cy - r * 0.8f), r * 0.26f, cat.accessoryColor)
        Accessory.GLASSES -> for (s in listOf(-1f, 1f)) {
            drawCircle(Color.White.copy(alpha = 0.25f), r * 0.26f, Offset(cx + s * eyeDx, eyeY))
            drawCircle(cat.accessoryColor, r * 0.26f, Offset(cx + s * eyeDx, eyeY), style = Stroke(r * 0.06f))
            drawLine(cat.accessoryColor, Offset(cx - r * 0.19f, eyeY), Offset(cx + r * 0.19f, eyeY), r * 0.05f)
        }
        Accessory.BELL -> {
            drawLine(Color(0xFFE91E63), Offset(cx - r * 0.75f, cy + r * 0.92f), Offset(cx + r * 0.75f, cy + r * 0.92f), r * 0.12f, StrokeCap.Round)
            val bc = Offset(cx, cy + r * 1.07f)
            drawCircle(Brush.radialGradient(listOf(Color.White, cat.accessoryColor, cat.accessoryColor.shade(-0.35f)), bc - Offset(r * 0.05f, r * 0.06f), r * 0.2f), r * 0.16f, bc)
            drawLine(Ink, bc + Offset(0f, r * 0.04f), bc + Offset(0f, r * 0.14f), r * 0.03f)
        }
        Accessory.CROWN -> {
            val base = cy - r * 0.95f
            val p = Path().apply {
                moveTo(cx - r * 0.4f, base); lineTo(cx - r * 0.45f, base - r * 0.4f); lineTo(cx - r * 0.2f, base - r * 0.2f)
                lineTo(cx, base - r * 0.5f); lineTo(cx + r * 0.2f, base - r * 0.2f); lineTo(cx + r * 0.45f, base - r * 0.4f)
                lineTo(cx + r * 0.4f, base); close()
            }
            drawPath(p, Brush.linearGradient(listOf(cat.accessoryColor.shade(0.5f), cat.accessoryColor, cat.accessoryColor.shade(-0.3f)),
                Offset(cx - r * 0.4f, base - r * 0.5f), Offset(cx + r * 0.4f, base)))
            drawPath(p, Ink, style = Stroke(r * 0.03f))
            drawCircle(Color(0xFFFF4081), r * 0.07f, Offset(cx, base - r * 0.18f))
            drawCircle(Color.White, r * 0.025f, Offset(cx - r * 0.02f, base - r * 0.2f))
        }
        Accessory.NONE -> {}
    }

    // Pattes posées sur le comptoir
    for (s in listOf(-1f, 1f)) {
        val pc = Offset(cx + s * r * 0.55f, cy + r * 2.15f + breathe)
        val paw = Rect(pc.x - r * 0.3f, pc.y - r * 0.2f, pc.x + r * 0.3f, pc.y + r * 0.2f)
        drawOval(Brush.radialGradient(listOf(fur.shade(0.4f), fur, fur.shade(-0.25f)), Offset(paw.left + paw.width * 0.35f, paw.top), paw.width * 0.8f), paw.topLeft, paw.size)
        drawOval(Ink, paw.topLeft, paw.size, style = Stroke(r * 0.035f))
        for (k in -1..1) drawLine(Ink.copy(alpha = 0.5f), Offset(pc.x + k * r * 0.1f, paw.bottom - r * 0.02f), Offset(pc.x + k * r * 0.1f, paw.bottom - r * 0.12f), r * 0.025f)
    }
}

/* =================================== MOTIFS =================================== */

fun DrawScope.drawBow(c: Offset, s: Float, color: Color) {
    val left = Path().apply { moveTo(c.x, c.y); lineTo(c.x - s, c.y - s * 0.6f); lineTo(c.x - s, c.y + s * 0.6f); close() }
    val right = Path().apply { moveTo(c.x, c.y); lineTo(c.x + s, c.y - s * 0.6f); lineTo(c.x + s, c.y + s * 0.6f); close() }
    val brush = Brush.verticalGradient(listOf(color.shade(0.35f), color, color.shade(-0.3f)), c.y - s * 0.6f, c.y + s * 0.6f)
    drawPath(left, brush); drawPath(right, brush)
    drawPath(left, Ink, style = Stroke(s * 0.08f)); drawPath(right, Ink, style = Stroke(s * 0.08f))
    drawCircle(color.shade(-0.1f), s * 0.25f, c)
    drawCircle(Ink, s * 0.25f, c, style = Stroke(s * 0.08f))
}

fun DrawScope.drawFlower(c: Offset, s: Float, color: Color) {
    for (i in 0 until 5) {
        val a = Math.toRadians(i * 72.0 - 90.0)
        val pc = Offset(c.x + cos(a).toFloat() * s * 0.6f, c.y + sin(a).toFloat() * s * 0.6f)
        drawCircle(Brush.radialGradient(listOf(color.shade(0.45f), color, color.shade(-0.15f)), c, s * 1.1f), s * 0.5f, pc)
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107)), c, s * 0.35f), s * 0.35f, c)
}

fun DrawScope.drawHeart(c: Offset, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + s * 0.8f)
        cubicTo(c.x - s * 1.2f, c.y - s * 0.1f, c.x - s * 0.5f, c.y - s * 0.9f, c.x, c.y - s * 0.3f)
        cubicTo(c.x + s * 0.5f, c.y - s * 0.9f, c.x + s * 1.2f, c.y - s * 0.1f, c.x, c.y + s * 0.8f)
        close()
    }
    drawPath(p, color)
}

fun DrawScope.drawPaw(c: Offset, s: Float, color: Color) {
    drawOval(color, Offset(c.x - s * 0.5f, c.y - s * 0.1f), Size(s, s * 0.8f))
    for (i in 0 until 4) {
        val a = Math.toRadians(-150.0 + i * 40.0)
        drawCircle(color, s * 0.2f, Offset(c.x + cos(a).toFloat() * s * 0.7f, c.y + sin(a).toFloat() * s * 0.6f))
    }
}

private inline fun DrawScope.scatter(left: Float, top: Float, width: Float, height: Float, draw: DrawScope.(Offset) -> Unit) {
    var row = 0
    var y = top + height * 0.12f
    while (y < top + height) {
        var x = left + width * (if (row % 2 == 0) 0.14f else 0.32f)
        while (x < left + width) { draw(Offset(x, y)); x += width * 0.36f }
        y += height * 0.2f; row++
    }
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

/* ===================================== MUG ===================================== */

/** État d'animation du mug : niveau de liquide, vapeur, versement en cours. */
data class MugAnim(
    val level: Float = -1f,
    val steam: Float = 0f,
    val pour: Ingredient? = null,
    val pourT: Float = 0f,
)

/**
 * Mug en céramique vu de 3/4 : ombrage, reflet brillant, soucoupe, intérieur visible,
 * liquide qui monte, latte art sur la mousse, chantilly en spirale, vapeur animée.
 * Quand un ingrédient est ajouté, on voit un filet couler dans la tasse.
 */
@Composable
fun MugView(mug: Mug, contents: List<Ingredient>, modifier: Modifier = Modifier, animated: Boolean = true) {
    if (!animated) {
        Canvas(modifier) { drawMug(mug, contents, MugAnim()) }
        return
    }
    val liquids = contents.count { it.kind == IngredientKind.LIQUID }
    val level by animateFloatAsState(targetLevel(liquids), tween(650), label = "level")
    val inf = rememberInfiniteTransition(label = "steam")
    val steam by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "s")
    // Animation de versement à chaque ajout
    var lastSize by remember { mutableIntStateOf(contents.size) }
    var pouring by remember { mutableStateOf<Ingredient?>(null) }
    val pourT = remember { Animatable(1f) }
    LaunchedEffect(contents.size) {
        if (contents.size > lastSize) {
            pouring = contents.last()
            pourT.snapTo(0f)
            pourT.animateTo(1f, tween(700, easing = LinearEasing))
            pouring = null
        }
        lastSize = contents.size
    }
    Canvas(modifier) { drawMug(mug, contents, MugAnim(level, steam, pouring, pourT.value)) }
}

private fun targetLevel(liquids: Int) = if (liquids == 0) 0f else (0.35f + 0.16f * liquids).coerceAtMost(0.95f)

fun DrawScope.drawMug(mug: Mug, contents: List<Ingredient>, anim: MugAnim = MugAnim()) {
    val w = size.width
    val h = size.height
    val bodyW = w * 0.66f
    val left = w * 0.1f
    val right = left + bodyW
    val top = h * 0.32f
    val bottom = h * 0.86f
    val stroke = w * 0.015f
    val inset = bodyW * 0.07f
    val rimH = bodyW * 0.26f
    val liquids = contents.filter { it.kind == IngredientKind.LIQUID }
    val toppings = contents.filter { it.kind == IngredientKind.TOPPING }
    val level = if (anim.level >= 0f) anim.level else targetLevel(liquids.size)

    // Soucoupe + ombre
    val saucer = Rect(w * 0.02f, bottom - h * 0.05f, w * 0.9f, bottom + h * 0.11f)
    drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent), saucer.center + Offset(0f, h * 0.03f), saucer.width * 0.55f),
        saucer.topLeft + Offset(0f, h * 0.03f), saucer.size)
    drawOval(Brush.verticalGradient(listOf(mug.body.shade(-0.05f), mug.body.shade(-0.3f)), saucer.top, saucer.bottom), saucer.topLeft, saucer.size)
    drawOval(Brush.verticalGradient(listOf(Color.White, mug.body.shade(-0.08f)), saucer.top, saucer.bottom),
        saucer.topLeft + Offset(saucer.width * 0.08f, saucer.height * 0.1f), Size(saucer.width * 0.84f, saucer.height * 0.65f))
    drawOval(Ink.copy(alpha = 0.5f), saucer.topLeft, saucer.size, style = Stroke(stroke * 0.8f))

    val bodyPath = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right - inset, bottom - h * 0.06f)
        quadraticBezierTo(right - inset, bottom, right - inset - h * 0.06f, bottom)
        lineTo(left + inset + h * 0.06f, bottom)
        quadraticBezierTo(left + inset, bottom, left + inset, bottom - h * 0.06f)
        close()
    }

    // Anse en volume
    val handleTopLeft = Offset(right - bodyW * 0.2f, top + h * 0.08f)
    val handleSize = Size(w * 0.34f, h * 0.36f)
    drawArc(Ink, -85f, 170f, false, handleTopLeft, handleSize, style = Stroke(w * 0.085f, cap = StrokeCap.Round))
    drawArc(Brush.horizontalGradient(listOf(mug.body.shade(0.2f), mug.body.shade(-0.3f)), handleTopLeft.x, handleTopLeft.x + handleSize.width),
        -85f, 170f, false, handleTopLeft, handleSize, style = Stroke(w * 0.06f, cap = StrokeCap.Round))
    drawArc(Color.White.copy(alpha = 0.5f), -70f, 60f, false, handleTopLeft + Offset(w * 0.01f, 0f), handleSize, style = Stroke(w * 0.012f, cap = StrokeCap.Round))

    // Oreilles de chat sur le bord (derrière l'ouverture)
    if (mug.catEars) {
        for (s in listOf(0.2f, 0.8f)) {
            val ex = left + bodyW * s
            val ear = Path().apply { moveTo(ex - bodyW * 0.13f, top + 2f); lineTo(ex, top - rimH * 0.2f - h * 0.1f); lineTo(ex + bodyW * 0.13f, top + 2f); close() }
            drawPath(ear, Brush.verticalGradient(listOf(mug.body.shade(0.2f), mug.body.shade(-0.2f)), top - h * 0.15f, top))
            drawPath(ear, Ink, style = Stroke(stroke))
            val inner = Path().apply { moveTo(ex - bodyW * 0.06f, top); lineTo(ex, top - rimH * 0.2f - h * 0.05f); lineTo(ex + bodyW * 0.06f, top); close() }
            drawPath(inner, mug.detail)
        }
    }

    // Corps en céramique
    drawPath(bodyPath, mug.body)
    clipPath(bodyPath) {
        when (mug.pattern) {
            MugPattern.DOTS -> {
                var y = top + h * 0.08f; var row = 0
                while (y < bottom) {
                    var x = left + (if (row % 2 == 0) bodyW * 0.1f else bodyW * 0.22f)
                    while (x < right) { drawCircle(mug.detail, w * 0.024f, Offset(x, y)); x += bodyW * 0.24f }
                    y += h * 0.09f; row++
                }
            }
            MugPattern.STRIPES -> {
                var y = top + h * 0.06f
                while (y < bottom) { drawRect(mug.detail, Offset(left, y), Size(bodyW, h * 0.035f)); y += h * 0.09f }
            }
            MugPattern.HEARTS -> scatter(left, top, bodyW, bottom - top) { c -> drawHeart(c, w * 0.04f, mug.detail) }
            MugPattern.PAWS -> scatter(left, top, bodyW, bottom - top) { c -> drawPaw(c, w * 0.05f, mug.detail) }
            MugPattern.STRAWBERRIES -> scatter(left, top, bodyW, bottom - top) { c ->
                drawHeart(c, w * 0.04f, mug.detail)
                drawCircle(Color(0xFF66BB6A), w * 0.015f, Offset(c.x, c.y - w * 0.03f))
            }
            MugPattern.FLOWERS -> scatter(left, top, bodyW, bottom - top) { c -> drawFlower(c, w * 0.035f, mug.detail) }
            MugPattern.NONE -> {}
        }
        if (mug.catEars) {
            val fy = top + (bottom - top) * 0.55f
            val fx = left + bodyW / 2f
            drawCircle(Ink, w * 0.018f, Offset(fx - bodyW * 0.15f, fy))
            drawCircle(Ink, w * 0.018f, Offset(fx + bodyW * 0.15f, fy))
            drawOval(Blush, Offset(fx - bodyW * 0.3f, fy + h * 0.02f), Size(bodyW * 0.1f, h * 0.035f))
            drawOval(Blush, Offset(fx + bodyW * 0.2f, fy + h * 0.02f), Size(bodyW * 0.1f, h * 0.035f))
            val m = Path().apply {
                moveTo(fx - bodyW * 0.06f, fy + h * 0.03f)
                quadraticBezierTo(fx - bodyW * 0.03f, fy + h * 0.05f, fx, fy + h * 0.03f)
                quadraticBezierTo(fx + bodyW * 0.03f, fy + h * 0.05f, fx + bodyW * 0.06f, fy + h * 0.03f)
            }
            drawPath(m, Ink, style = Stroke(w * 0.01f, cap = StrokeCap.Round))
        }
        // Ombrage cylindrique : lumière à gauche, ombre à droite
        drawRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.10f),
                0.22f to Color.White.copy(alpha = 0.30f),
                0.45f to Color.Transparent,
                0.8f to Color.Black.copy(alpha = 0.12f),
                1f to Color.Black.copy(alpha = 0.28f),
                startX = left, endX = right,
            ),
            Offset(left, top), Size(bodyW, bottom - top),
        )
        // Reflet brillant vertical
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.05f)), top, bottom),
            Offset(left + bodyW * 0.14f, top + h * 0.05f), Size(bodyW * 0.06f, (bottom - top) * 0.7f), CornerRadius(bodyW * 0.03f),
        )
        // Ombre sous le rebord
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent), top, top + h * 0.05f), Offset(left, top), Size(bodyW, h * 0.05f))
    }
    drawPath(bodyPath, Ink, style = Stroke(stroke))

    // Ouverture : rebord, paroi intérieure, liquide
    val rim = Rect(left, top - rimH / 2f, right, top + rimH / 2f)
    val inner = Rect(rim.left + stroke * 2.5f, rim.top + stroke * 2f, rim.right - stroke * 2.5f, rim.bottom - stroke * 1.2f)
    drawOval(Brush.horizontalGradient(listOf(mug.body.shade(0.3f), mug.body, mug.body.shade(-0.2f)), rim.left, rim.right), rim.topLeft, rim.size)
    val innerPath = Path().apply { addOval(inner) }
    drawOval(Brush.verticalGradient(listOf(mug.body.shade(-0.45f), mug.body.shade(-0.15f)), inner.top, inner.bottom), inner.topLeft, inner.size)

    val hasFoam = Ingredient.FOAM in toppings
    clipPath(innerPath) {
        if (liquids.isNotEmpty()) {
            var mix = liquids.first().color
            liquids.drop(1).forEach { mix = lerp(mix, it.color, 0.55f) }
            val drop = (1f - level) * inner.height * 0.9f
            val surf = Rect(inner.left - inner.width * 0.05f, inner.top + drop, inner.right + inner.width * 0.05f, inner.bottom + drop + inner.height * 0.1f)
            val surfaceColor = if (hasFoam) Color(0xFFFFF3E0) else mix
            drawOval(
                Brush.radialGradient(listOf(surfaceColor.shade(0.25f), surfaceColor, surfaceColor.shade(-0.2f)), Offset(surf.center.x - surf.width * 0.15f, surf.center.y - surf.height * 0.1f), surf.width * 0.6f),
                surf.topLeft, Size(surf.width, inner.bottom - surf.top + inner.height),
            )
            if (hasFoam) {
                // Crème du café sur le pourtour + latte art en cœur
                val darkBase = liquids.any { it == Ingredient.ESPRESSO || it == Ingredient.CHOCO || it == Ingredient.TEA || it == Ingredient.MATCHA }
                if (darkBase) {
                    drawOval(mix.shade(-0.1f), surf.topLeft, Size(surf.width, surf.height), style = Stroke(inner.height * 0.2f))
                    val hc = Offset(surf.center.x, surf.top + inner.height * 0.45f)
                    withTransform({ scale(1f, 0.55f, hc) }) {
                        drawHeart(hc, inner.width * 0.18f, mix.shade(0.1f))
                        drawHeart(hc + Offset(0f, inner.width * 0.02f), inner.width * 0.11f, Color(0xFFFFF8E1))
                    }
                }
                // Micro-bulles
                val rnd = Random(7)
                repeat(14) {
                    drawCircle(Color.White.copy(alpha = 0.7f), inner.width * rnd.nextFloat() * 0.015f + 1f,
                        Offset(surf.left + surf.width * rnd.nextFloat(), surf.top + inner.height * 0.7f * rnd.nextFloat()))
                }
            } else {
                // Reflet sur le liquide
                drawOval(Color.White.copy(alpha = 0.35f), Offset(surf.left + surf.width * 0.2f, surf.top + inner.height * 0.12f), Size(surf.width * 0.25f, inner.height * 0.12f))
            }
            if (Ingredient.ICE in liquids) {
                val n = liquids.count { it == Ingredient.ICE } * 2
                for (i in 0 until n) {
                    val x = inner.left + inner.width * (0.15f + 0.22f * (i % 4))
                    val y = surf.top + inner.height * (0.05f + 0.12f * (i / 4))
                    val cube = Size(inner.width * 0.17f, inner.height * 0.32f)
                    rotate(if (i % 2 == 0) -12f else 10f, Offset(x + cube.width / 2, y + cube.height / 2)) {
                        drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xAAB3E5FC)), Offset(x, y), Offset(x + cube.width, y + cube.height)),
                            Offset(x, y), cube, CornerRadius(cube.width * 0.2f))
                        drawRoundRect(Color.White, Offset(x, y), cube, CornerRadius(cube.width * 0.2f), style = Stroke(stroke * 0.5f))
                    }
                }
            }
        }
    }
    drawOval(Ink, rim.topLeft, rim.size, style = Stroke(stroke))
    drawOval(Ink.copy(alpha = 0.35f), inner.topLeft, inner.size, style = Stroke(stroke * 0.6f))

    // Garnitures au-dessus de l'ouverture
    var stack = 0f
    toppings.forEach { t ->
        val base = top - stack
        when (t) {
            Ingredient.FOAM -> {
                // Dôme de mousse onctueux : bulles en volume + petites bulles
                val domeH = rimH * 0.55f
                val puffs = listOf(0.2f, 0.4f, 0.6f, 0.8f, 0.3f, 0.7f, 0.5f)
                puffs.forEachIndexed { i, fx ->
                    val cx = inner.left + inner.width * fx
                    val cy = base - (if (i >= 4) domeH * 0.55f else domeH * 0.1f) - (if (i == 6) domeH * 0.35f else 0f)
                    val rr = inner.width * (if (i >= 4) 0.2f else 0.17f)
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White, Color(0xFFFFF8EE), Color(0xFFEBDCCB)), Offset(cx - rr * 0.35f, cy - rr * 0.4f), rr * 1.3f),
                        rr, Offset(cx, cy),
                    )
                }
                val rnd = Random(3)
                repeat(10) {
                    val bx = inner.left + inner.width * (0.15f + 0.7f * rnd.nextFloat())
                    val by = base - domeH * rnd.nextFloat() * 0.9f
                    drawCircle(Color(0xFFE0D2C0), inner.width * 0.018f, Offset(bx, by), style = Stroke(1.5f))
                }
                stack += domeH * 0.5f
            }
            Ingredient.CREAM -> {
                // Chantilly en spirale (3 étages + pointe)
                val levels = listOf(0.95f, 0.72f, 0.5f)
                levels.forEachIndexed { i, sc ->
                    val lw = inner.width * sc
                    val cy = base - i * rimH * 0.32f
                    val rect = Rect(inner.center.x - lw / 2, cy - rimH * 0.28f, inner.center.x + lw / 2, cy + rimH * 0.12f)
                    drawOval(Brush.radialGradient(listOf(Color.White, Color(0xFFFFFAF3), Color(0xFFE8DDD0)), Offset(rect.left + rect.width * 0.35f, rect.top), rect.width * 0.7f), rect.topLeft, rect.size)
                    drawArc(Color(0xFFDCCFC0), 20f, 140f, false, rect.topLeft, rect.size, style = Stroke(stroke * 0.8f))
                }
                val tipBase = base - rimH * 0.9f
                val tip = Path().apply {
                    moveTo(inner.center.x - inner.width * 0.12f, tipBase + rimH * 0.05f)
                    quadraticBezierTo(inner.center.x, tipBase - rimH * 0.2f, inner.center.x + inner.width * 0.08f, tipBase - rimH * 0.35f)
                    quadraticBezierTo(inner.center.x + inner.width * 0.06f, tipBase, inner.center.x + inner.width * 0.12f, tipBase + rimH * 0.05f)
                    close()
                }
                drawPath(tip, Brush.linearGradient(listOf(Color.White, Color(0xFFE8DDD0)), Offset(inner.center.x - inner.width * 0.1f, tipBase - rimH * 0.3f), Offset(inner.center.x + inner.width * 0.12f, tipBase)))
                stack += rimH * 0.9f
            }
            Ingredient.CARAMEL -> {
                val y0 = base - rimH * 0.1f
                val p = Path().apply {
                    moveTo(inner.left + inner.width * 0.1f, y0)
                    for (k in 1..6) {
                        val x = inner.left + inner.width * (0.1f + k * 0.13f)
                        quadraticBezierTo(x - inner.width * 0.065f, y0 + (if (k % 2 == 0) -1 else 1) * rimH * 0.18f, x, y0)
                    }
                }
                drawPath(p, t.color.shade(-0.2f), style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawPath(p, t.color.shade(0.25f), style = Stroke(stroke * 0.8f, cap = StrokeCap.Round))
                for (k in 0 until 3) {
                    val dx = rim.left + rim.width * (0.25f + k * 0.25f)
                    drawLine(t.color, Offset(dx, top), Offset(dx, top + h * (0.05f + 0.03f * k)), stroke * 1.4f, StrokeCap.Round)
                }
            }
            Ingredient.SAKURA -> for (k in 0 until 5) {
                val x = inner.left + inner.width * (0.12f + k * 0.19f)
                val y = base - rimH * (0.1f + 0.15f * (k % 2))
                rotate(k * 37f, Offset(x, y)) {
                    val petal = Path().apply {
                        moveTo(x, y + w * 0.025f)
                        quadraticBezierTo(x - w * 0.03f, y - w * 0.005f, x - w * 0.008f, y - w * 0.03f)
                        lineTo(x, y - w * 0.02f)
                        lineTo(x + w * 0.008f, y - w * 0.03f)
                        quadraticBezierTo(x + w * 0.03f, y - w * 0.005f, x, y + w * 0.025f)
                        close()
                    }
                    drawPath(petal, Brush.radialGradient(listOf(Color.White, t.color), Offset(x, y + w * 0.02f), w * 0.05f))
                }
            }
            Ingredient.MARSHMALLOW -> for (k in 0 until 3) {
                val x = inner.left + inner.width * (0.12f + k * 0.28f)
                val y = base - rimH * (0.35f + 0.12f * (k % 2))
                val mw = w * 0.1f
                val mh = h * 0.06f
                val col = if (k % 2 == 0) t.color else Color.White
                drawRoundRect(Brush.horizontalGradient(listOf(col.shade(0.3f), col, col.shade(-0.15f)), x, x + mw), Offset(x, y), Size(mw, mh), CornerRadius(mw * 0.2f))
                drawOval(col.shade(0.35f), Offset(x, y - mh * 0.2f), Size(mw, mh * 0.45f))
            }
            else -> {}
        }
    }

    // Vapeur animée (seulement pour les boissons chaudes)
    if (liquids.isNotEmpty() && Ingredient.ICE !in liquids) {
        for (k in 0 until 3) {
            val phase = (anim.steam + k / 3f) % 1f
            val x = rim.left + rim.width * (0.3f + k * 0.2f)
            val y0 = top - stack - rimH * 0.3f
            val rise = phase * h * 0.22f
            val wobble = sin((phase * 2f + k) * PI.toFloat()) * w * 0.025f
            val p = Path().apply {
                moveTo(x, y0 - rise)
                quadraticBezierTo(x - w * 0.03f + wobble, y0 - rise - h * 0.05f, x + wobble, y0 - rise - h * 0.1f)
                quadraticBezierTo(x + w * 0.03f + wobble, y0 - rise - h * 0.14f, x, y0 - rise - h * 0.18f)
            }
            drawPath(p, Color.White.copy(alpha = 0.7f * (1f - phase) * (if (anim.level < 0f) 0.6f else 1f)), style = Stroke(w * 0.022f, cap = StrokeCap.Round))
        }
    }

    // Filet qui coule quand on ajoute un ingrédient
    val pour = anim.pour
    if (pour != null && anim.pourT < 1f) {
        val t = anim.pourT
        val streamX = inner.center.x + inner.width * 0.1f
        val surfaceY = inner.top + (1f - level) * inner.height * 0.9f
        if (pour.kind == IngredientKind.LIQUID && pour != Ingredient.ICE) {
            val headY = minOf(surfaceY, t * 3f * surfaceY)
            val tailY = if (t > 0.7f) (t - 0.7f) / 0.3f * surfaceY else 0f
            val width = w * 0.035f * (1f - t * 0.4f)
            drawLine(pour.color.shade(-0.1f), Offset(streamX, tailY), Offset(streamX, headY), width, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.4f), Offset(streamX - width * 0.2f, tailY), Offset(streamX - width * 0.2f, headY), width * 0.25f, StrokeCap.Round)
            if (headY >= surfaceY) {
                val rnd = Random(11)
                repeat(6) {
                    val a = rnd.nextFloat() * PI.toFloat()
                    val d = (t * 3f % 1f) * w * 0.08f
                    drawCircle(pour.color, w * 0.01f, Offset(streamX + cos(a) * d * (if (it % 2 == 0) 1 else -1), surfaceY - sin(a) * d * 0.6f))
                }
            }
        } else {
            // Garnitures / glaçons : ils tombent du haut
            val fallY = t * (top - stack)
            val a = 1f - maxOf(0f, (t - 0.85f) / 0.15f)
            for (k in 0 until 3) {
                val x = streamX + (k - 1) * w * 0.06f
                val y = fallY - k * h * 0.04f
                when (pour) {
                    Ingredient.ICE -> drawRoundRect(Color(0xCCE1F5FE), Offset(x, y), Size(w * 0.07f, w * 0.07f), CornerRadius(w * 0.015f), alpha = a)
                    Ingredient.SAKURA -> drawFlower(Offset(x, y), w * 0.025f, pour.color)
                    else -> drawCircle(pour.color, w * 0.02f, Offset(x, y), alpha = a)
                }
            }
        }
    }
}
