package com.meowcha.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import com.meowcha.game.game.Accessory
import com.meowcha.game.game.CatCustomer
import com.meowcha.game.game.FurPattern
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.IngredientKind
import com.meowcha.game.game.Mood
import com.meowcha.game.game.Mug
import com.meowcha.game.game.MugPattern
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF4E342E)
private val Blush = Color(0x88FF80AB)
private val InnerEar = Color(0xFFF8BBD0)

/** Chat dessiné entièrement en vectoriel (tête + épaules + pattes posées sur le comptoir). */
@Composable
fun CatPortrait(cat: CatCustomer, mood: Mood, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawCat(cat, mood) }
}

fun DrawScope.drawCat(cat: CatCustomer, mood: Mood) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val r = minOf(w, h) * 0.30f
    val cy = h * 0.46f
    val outline = Stroke(width = r * 0.045f)

    // Corps
    val body = Rect(cx - r * 1.05f, cy + r * 0.55f, cx + r * 1.05f, cy + r * 2.2f)
    drawRoundRect(cat.fur, body.topLeft, body.size, CornerRadius(r * 0.9f))
    drawRoundRect(Ink, body.topLeft, body.size, CornerRadius(r * 0.9f), style = outline)
    if (cat.pattern == FurPattern.TUXEDO) {
        drawOval(cat.accent, Offset(cx - r * 0.45f, cy + r * 0.7f), Size(r * 0.9f, r * 1.4f))
    }

    // Oreilles
    fun ear(sign: Float) {
        val base1 = Offset(cx + sign * r * 0.25f, cy - r * 0.8f)
        val base2 = Offset(cx + sign * r * 0.95f, cy - r * 0.35f)
        val tip = Offset(cx + sign * r * 0.85f, cy - r * 1.3f)
        val p = Path().apply { moveTo(base1.x, base1.y); lineTo(tip.x, tip.y); lineTo(base2.x, base2.y); close() }
        val earColor = if (cat.pattern == FurPattern.POINTS) cat.accent else cat.fur
        drawPath(p, earColor)
        drawPath(p, Ink, style = outline)
        val inner = Path().apply {
            moveTo(base1.x + (tip.x - base1.x) * 0.2f + sign * r * 0.08f, base1.y + r * 0.02f)
            lineTo(tip.x - sign * r * 0.04f, tip.y + r * 0.2f)
            lineTo(base2.x - sign * r * 0.12f, base2.y - r * 0.12f)
            close()
        }
        drawPath(inner, InnerEar)
    }
    ear(-1f); ear(1f)

    // Tête
    drawOval(cat.fur, Offset(cx - r * 1.1f, cy - r), Size(r * 2.2f, r * 1.95f))

    // Motifs du pelage
    clipPath(Path().apply { addOval(Rect(cx - r * 1.1f, cy - r, cx + r * 1.1f, cy + r * 0.95f)) }) {
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
            FurPattern.POINTS -> drawOval(cat.accent.copy(alpha = 0.55f), Offset(cx - r * 0.5f, cy - r * 0.05f), Size(r * 1.0f, r * 0.85f))
            FurPattern.PLAIN -> {}
        }
    }
    drawOval(Ink, Offset(cx - r * 1.1f, cy - r), Size(r * 2.2f, r * 1.95f), style = outline)

    // Yeux
    val eyeY = cy - r * 0.05f
    val eyeDx = r * 0.45f
    for (s in listOf(-1f, 1f)) {
        val c = Offset(cx + s * eyeDx, eyeY)
        when (mood) {
            Mood.DELIGHTED, Mood.HAPPY -> {
                // yeux fermés en arc ^^
                val p = Path().apply {
                    moveTo(c.x - r * 0.18f, c.y + r * 0.05f)
                    quadraticBezierTo(c.x, c.y - r * 0.2f, c.x + r * 0.18f, c.y + r * 0.05f)
                }
                if (mood == Mood.DELIGHTED) {
                    drawPath(p, Ink, style = Stroke(r * 0.07f, cap = StrokeCap.Round))
                } else {
                    drawOval(cat.eyes, Offset(c.x - r * 0.16f, c.y - r * 0.2f), Size(r * 0.32f, r * 0.36f))
                    drawOval(Ink, Offset(c.x - r * 0.09f, c.y - r * 0.14f), Size(r * 0.18f, r * 0.26f))
                    drawCircle(Color.White, r * 0.06f, Offset(c.x + r * 0.03f, c.y - r * 0.08f))
                }
            }
            Mood.WAITING -> {
                drawOval(cat.eyes, Offset(c.x - r * 0.16f, c.y - r * 0.16f), Size(r * 0.32f, r * 0.3f))
                drawOval(Ink, Offset(c.x - r * 0.06f, c.y - r * 0.12f), Size(r * 0.12f, r * 0.24f))
                drawCircle(Color.White, r * 0.05f, Offset(c.x + r * 0.03f, c.y - r * 0.07f))
            }
            Mood.IMPATIENT -> {
                drawOval(cat.eyes, Offset(c.x - r * 0.16f, c.y - r * 0.08f), Size(r * 0.32f, r * 0.2f))
                drawOval(Ink, Offset(c.x - r * 0.04f, c.y - r * 0.06f), Size(r * 0.08f, r * 0.16f))
                drawLine(Ink, Offset(c.x - s * r * 0.2f, c.y - r * 0.24f), Offset(c.x + s * r * 0.18f, c.y - r * 0.14f), r * 0.06f, StrokeCap.Round)
            }
            Mood.SAD -> {
                val p = Path().apply {
                    moveTo(c.x - r * 0.18f, c.y - r * 0.05f)
                    quadraticBezierTo(c.x, c.y + r * 0.15f, c.x + r * 0.18f, c.y - r * 0.05f)
                }
                drawPath(p, Ink, style = Stroke(r * 0.07f, cap = StrokeCap.Round))
                drawOval(Color(0xFF81D4FA), Offset(c.x + s * r * 0.08f, c.y + r * 0.1f), Size(r * 0.1f, r * 0.18f))
            }
        }
    }

    // Joues
    drawOval(Blush, Offset(cx - r * 0.85f, cy + r * 0.2f), Size(r * 0.34f, r * 0.2f))
    drawOval(Blush, Offset(cx + r * 0.51f, cy + r * 0.2f), Size(r * 0.34f, r * 0.2f))

    // Nez + bouche "w"
    val nose = Path().apply {
        moveTo(cx - r * 0.09f, cy + r * 0.15f); lineTo(cx + r * 0.09f, cy + r * 0.15f); lineTo(cx, cy + r * 0.26f); close()
    }
    drawPath(nose, Color(0xFFFF80AB))
    val mouth = Path().apply {
        moveTo(cx - r * 0.2f, cy + r * 0.32f)
        quadraticBezierTo(cx - r * 0.1f, cy + r * 0.44f, cx, cy + r * 0.28f)
        quadraticBezierTo(cx + r * 0.1f, cy + r * 0.44f, cx + r * 0.2f, cy + r * 0.32f)
    }
    if (mood == Mood.SAD || mood == Mood.IMPATIENT) {
        val frown = Path().apply {
            moveTo(cx - r * 0.14f, cy + r * 0.42f)
            quadraticBezierTo(cx, cy + r * 0.3f, cx + r * 0.14f, cy + r * 0.42f)
        }
        drawPath(frown, Ink, style = Stroke(r * 0.05f, cap = StrokeCap.Round))
    } else {
        drawPath(mouth, Ink, style = Stroke(r * 0.05f, cap = StrokeCap.Round))
    }
    if (mood == Mood.DELIGHTED) {
        drawOval(Color(0xFFFF8A80), Offset(cx - r * 0.07f, cy + r * 0.36f), Size(r * 0.14f, r * 0.14f))
    }

    // Moustaches
    val whisker = if (cat.fur.luminance() < 0.3f) Color(0xFFEEEEEE) else Ink.copy(alpha = 0.7f)
    for (s in listOf(-1f, 1f)) for (k in -1..1) {
        drawLine(
            whisker,
            Offset(cx + s * r * 0.55f, cy + r * 0.3f + k * r * 0.06f),
            Offset(cx + s * r * 1.25f, cy + r * 0.22f + k * r * 0.14f),
            r * 0.025f, StrokeCap.Round,
        )
    }

    // Accessoires
    when (cat.accessory) {
        Accessory.BOW -> drawBow(Offset(cx + r * 0.7f, cy - r * 0.85f), r * 0.32f, cat.accessoryColor)
        Accessory.FLOWER -> drawFlower(Offset(cx - r * 0.7f, cy - r * 0.8f), r * 0.26f, cat.accessoryColor)
        Accessory.GLASSES -> for (s in listOf(-1f, 1f)) {
            drawCircle(cat.accessoryColor, r * 0.26f, Offset(cx + s * eyeDx, eyeY), style = Stroke(r * 0.06f))
            drawLine(cat.accessoryColor, Offset(cx - r * 0.19f, eyeY), Offset(cx + r * 0.19f, eyeY), r * 0.05f)
        }
        Accessory.BELL -> {
            drawLine(Color(0xFFE91E63), Offset(cx - r * 0.75f, cy + r * 0.9f), Offset(cx + r * 0.75f, cy + r * 0.9f), r * 0.12f, StrokeCap.Round)
            drawCircle(cat.accessoryColor, r * 0.16f, Offset(cx, cy + r * 1.05f))
            drawCircle(Ink, r * 0.16f, Offset(cx, cy + r * 1.05f), style = Stroke(r * 0.03f))
        }
        Accessory.CROWN -> {
            val base = cy - r * 0.95f
            val p = Path().apply {
                moveTo(cx - r * 0.4f, base); lineTo(cx - r * 0.45f, base - r * 0.4f); lineTo(cx - r * 0.2f, base - r * 0.2f)
                lineTo(cx, base - r * 0.5f); lineTo(cx + r * 0.2f, base - r * 0.2f); lineTo(cx + r * 0.45f, base - r * 0.4f)
                lineTo(cx + r * 0.4f, base); close()
            }
            drawPath(p, cat.accessoryColor)
            drawPath(p, Ink, style = Stroke(r * 0.03f))
            drawCircle(Color(0xFFFF4081), r * 0.07f, Offset(cx, base - r * 0.18f))
        }
        Accessory.NONE -> {}
    }

    // Pattes sur le comptoir
    for (s in listOf(-1f, 1f)) {
        val pc = Offset(cx + s * r * 0.55f, cy + r * 2.15f)
        drawOval(cat.fur, Offset(pc.x - r * 0.3f, pc.y - r * 0.2f), Size(r * 0.6f, r * 0.4f))
        drawOval(Ink, Offset(pc.x - r * 0.3f, pc.y - r * 0.2f), Size(r * 0.6f, r * 0.4f), style = Stroke(r * 0.035f))
        drawCircle(InnerEar, r * 0.06f, Offset(pc.x, pc.y + r * 0.03f))
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

fun DrawScope.drawBow(c: Offset, s: Float, color: Color) {
    val left = Path().apply { moveTo(c.x, c.y); lineTo(c.x - s, c.y - s * 0.6f); lineTo(c.x - s, c.y + s * 0.6f); close() }
    val right = Path().apply { moveTo(c.x, c.y); lineTo(c.x + s, c.y - s * 0.6f); lineTo(c.x + s, c.y + s * 0.6f); close() }
    drawPath(left, color); drawPath(right, color)
    drawPath(left, Ink, style = Stroke(s * 0.08f)); drawPath(right, Ink, style = Stroke(s * 0.08f))
    drawCircle(color, s * 0.25f, c)
    drawCircle(Ink, s * 0.25f, c, style = Stroke(s * 0.08f))
}

fun DrawScope.drawFlower(c: Offset, s: Float, color: Color) {
    for (i in 0 until 5) {
        val a = Math.toRadians(i * 72.0 - 90.0)
        drawCircle(color, s * 0.5f, Offset(c.x + cos(a).toFloat() * s * 0.6f, c.y + sin(a).toFloat() * s * 0.6f))
    }
    drawCircle(Color(0xFFFFEB3B), s * 0.35f, c)
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

/**
 * Mug dessiné avec son contenu : les liquides s'empilent en couches depuis le fond,
 * les garnitures (mousse, chantilly, sakura...) se posent sur le dessus.
 */
@Composable
fun MugView(mug: Mug, contents: List<Ingredient>, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawMug(mug, contents) }
}

fun DrawScope.drawMug(mug: Mug, contents: List<Ingredient>) {
    val w = size.width
    val h = size.height
    val bodyW = w * 0.68f
    val left = w * 0.08f
    val top = h * 0.2f
    val bottom = h * 0.95f
    val right = left + bodyW
    val stroke = w * 0.02f
    val inset = bodyW * 0.06f

    val bodyPath = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right - inset, bottom - h * 0.06f)
        quadraticBezierTo(right - inset, bottom, right - inset - h * 0.06f, bottom)
        lineTo(left + inset + h * 0.06f, bottom)
        quadraticBezierTo(left + inset, bottom, left + inset, bottom - h * 0.06f)
        close()
    }

    // Anse
    drawArc(
        mug.body, -80f, 160f, false,
        Offset(right - bodyW * 0.18f, top + h * 0.12f), Size(w * 0.34f, h * 0.42f),
        style = Stroke(w * 0.07f, cap = StrokeCap.Round),
    )
    drawArc(
        Ink, -80f, 160f, false,
        Offset(right - bodyW * 0.18f, top + h * 0.12f), Size(w * 0.34f, h * 0.42f),
        style = Stroke(stroke),
    )

    // Oreilles de chat sur le bord
    if (mug.catEars) {
        for (s in listOf(0.22f, 0.78f)) {
            val ex = left + bodyW * s
            val ear = Path().apply { moveTo(ex - bodyW * 0.13f, top + 2f); lineTo(ex, top - h * 0.13f); lineTo(ex + bodyW * 0.13f, top + 2f); close() }
            drawPath(ear, mug.body)
            drawPath(ear, Ink, style = Stroke(stroke))
            val inner = Path().apply { moveTo(ex - bodyW * 0.06f, top); lineTo(ex, top - h * 0.07f); lineTo(ex + bodyW * 0.06f, top); close() }
            drawPath(inner, mug.detail)
        }
    }

    drawPath(bodyPath, mug.body)

    // Motif
    clipPath(bodyPath) {
        when (mug.pattern) {
            MugPattern.DOTS -> {
                var y = top + h * 0.08f; var row = 0
                while (y < bottom) {
                    var x = left + (if (row % 2 == 0) bodyW * 0.1f else bodyW * 0.22f)
                    while (x < right) { drawCircle(mug.detail, w * 0.025f, Offset(x, y)); x += bodyW * 0.24f }
                    y += h * 0.1f; row++
                }
            }
            MugPattern.STRIPES -> {
                var y = top + h * 0.06f
                while (y < bottom) { drawRect(mug.detail, Offset(left, y), Size(bodyW, h * 0.04f)); y += h * 0.1f }
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
        // Visage mignon pour le mug à oreilles
        if (mug.catEars) {
            val fy = top + (bottom - top) * 0.55f
            val fx = left + bodyW / 2f - inset * 0.3f
            drawCircle(Ink, w * 0.018f, Offset(fx - bodyW * 0.15f, fy))
            drawCircle(Ink, w * 0.018f, Offset(fx + bodyW * 0.15f, fy))
            drawOval(Blush, Offset(fx - bodyW * 0.3f, fy + h * 0.02f), Size(bodyW * 0.1f, h * 0.035f))
            drawOval(Blush, Offset(fx + bodyW * 0.2f, fy + h * 0.02f), Size(bodyW * 0.1f, h * 0.035f))
        }
    }
    drawPath(bodyPath, Ink, style = Stroke(stroke))

    // Ouverture et contenu (vue en coupe sur le dessus)
    val rimH = h * 0.1f
    val rim = Rect(left + stroke, top - rimH / 2f, right - stroke, top + rimH / 2f)
    drawOval(Color(0xFFEFEBE9), rim.topLeft, rim.size)

    val liquids = contents.filter { it.kind == IngredientKind.LIQUID }
    val toppings = contents.filter { it.kind == IngredientKind.TOPPING }
    if (liquids.isNotEmpty()) {
        // Couleur de surface = mélange des liquides, pondéré vers le dernier ajouté
        var mix = liquids.first().color
        liquids.drop(1).forEach { mix = lerp(mix, it.color, 0.55f) }
        drawOval(mix, rim.topLeft, rim.size)
        if (Ingredient.ICE in liquids) {
            for (i in 0 until liquids.count { it == Ingredient.ICE } * 2) {
                val x = rim.left + rim.width * (0.2f + 0.25f * (i % 3))
                drawRoundRect(Color(0xCCFFFFFF), Offset(x, rim.top + rim.height * 0.2f), Size(rim.width * 0.14f, rim.height * 0.5f), CornerRadius(4f))
            }
        }
    }
    drawOval(Ink, rim.topLeft, rim.size, style = Stroke(stroke))

    // Jauge latérale montrant les couches (comme un verre transparent miniature)
    if (contents.isNotEmpty()) {
        val gx = right + w * 0.13f
        val gw = w * 0.07f
        val gh = (bottom - top) * 0.8f
        val gTop = bottom - gh
        val step = gh / 6f
        contents.forEachIndexed { i, ing ->
            drawRect(ing.color, Offset(gx, bottom - step * (i + 1)), Size(gw, step))
        }
        drawRoundRect(Ink, Offset(gx, gTop), Size(gw, gh), CornerRadius(gw / 3f), style = Stroke(stroke * 0.7f))
    }

    // Garnitures au-dessus
    toppings.forEachIndexed { i, t ->
        val lift = i * h * 0.05f
        val cy = top - lift
        when (t) {
            Ingredient.FOAM, Ingredient.CREAM -> {
                val puffs = if (t == Ingredient.CREAM) 4 else 5
                for (k in 0 until puffs) {
                    val x = rim.left + rim.width * (k + 0.5f) / puffs
                    val rr = if (t == Ingredient.CREAM) rim.width * 0.16f else rim.width * 0.12f
                    drawCircle(t.color, rr, Offset(x, cy - rr * 0.3f))
                    drawCircle(Ink.copy(alpha = 0.25f), rr, Offset(x, cy - rr * 0.3f), style = Stroke(stroke * 0.5f))
                }
                if (t == Ingredient.CREAM) drawCircle(t.color, rim.width * 0.14f, Offset(rim.center.x, cy - rim.width * 0.2f))
            }
            Ingredient.CARAMEL -> {
                for (k in 0 until 3) {
                    drawLine(t.color, Offset(rim.left + rim.width * 0.15f, cy - k * h * 0.02f), Offset(rim.right - rim.width * 0.15f, cy - k * h * 0.02f + h * 0.02f), stroke * 1.4f, StrokeCap.Round)
                }
            }
            Ingredient.SAKURA -> for (k in 0 until 4) {
                val x = rim.left + rim.width * (0.15f + k * 0.23f)
                rotate(k * 40f, Offset(x, cy)) { drawFlower(Offset(x, cy - h * 0.02f), w * 0.03f, t.color) }
            }
            Ingredient.MARSHMALLOW -> for (k in 0 until 3) {
                val x = rim.left + rim.width * (0.2f + k * 0.28f)
                drawRoundRect(if (k % 2 == 0) t.color else Color.White, Offset(x, cy - h * 0.06f), Size(w * 0.09f, h * 0.07f), CornerRadius(8f))
                drawRoundRect(Ink.copy(alpha = 0.3f), Offset(x, cy - h * 0.06f), Size(w * 0.09f, h * 0.07f), CornerRadius(8f), style = Stroke(stroke * 0.5f))
            }
            else -> {}
        }
    }

    // Vapeur
    if (liquids.isNotEmpty() && Ingredient.ICE !in liquids) {
        for (k in 0 until 3) {
            val x = rim.left + rim.width * (0.25f + k * 0.25f)
            val p = Path().apply {
                moveTo(x, top - h * 0.1f)
                quadraticBezierTo(x - w * 0.03f, top - h * 0.14f, x, top - h * 0.18f)
                quadraticBezierTo(x + w * 0.03f, top - h * 0.21f, x, top - h * 0.25f)
            }
            drawPath(p, Color(0x66BDBDBD), style = Stroke(stroke, cap = StrokeCap.Round))
        }
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
