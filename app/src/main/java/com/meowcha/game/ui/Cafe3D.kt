package com.meowcha.game.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext

/**
 * Inclinaison du téléphone, lissée, entre -1 et 1 sur chaque axe (0 = position de départ).
 * Sert à l'effet de parallaxe : les plans du décor bougent à des vitesses différentes.
 */
@Composable
fun rememberTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var baseline: Offset? = null
        var smooth = Offset.Zero
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val raw = Offset(-e.values[0] / 9.81f, e.values[1] / 9.81f)
                val b = baseline ?: raw.also { baseline = it }
                val target = Offset(((raw.x - b.x) * 2.2f).coerceIn(-1f, 1f), ((raw.y - b.y) * 2.2f).coerceIn(-1f, 1f))
                smooth = Offset(smooth.x + (target.x - smooth.x) * 0.12f, smooth.y + (target.y - smooth.y) * 0.12f)
                tilt.value = smooth
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}

/**
 * Salle du café en perspective : plafond, murs, sol en damier qui fuit vers le point de fuite,
 * fenêtre avec rayon de lumière, suspensions qui brillent, étagères, et les décorations achetées.
 */
@Composable
fun CafeBackdrop(decor: Set<String>, modifier: Modifier, tilt: Offset = Offset.Zero) {
    val anim = rememberInfiniteTransition(label = "cafe")
    val twinkle by anim.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "tw")
    val sway by anim.animateFloat(-1f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "sway")
    Canvas(modifier) { drawCafeRoom(decor, tilt, twinkle, sway) }
}

private val WallTop = Color(0xFFFFE3EC)
private val WallBottom = Color(0xFFFFC9DA)
private val Wainscot = Color(0xFFF7A8C2)
private val Wood = Color(0xFFD9A08A)

fun DrawScope.drawCafeRoom(decor: Set<String>, tilt: Offset, twinkle: Float, sway: Float) {
    val w = size.width
    val h = size.height
    // Parallaxe : le fond bouge peu, les objets proches bougent plus
    val far = Offset(tilt.x * w * 0.025f, tilt.y * h * 0.02f)
    val mid = far * 2f
    val near = far * 3.2f

    // Mur du fond (rectangle) + murs latéraux / sol / plafond en trapèzes
    val back = Rect(w * 0.16f + far.x, h * 0.1f + far.y, w * 0.84f + far.x, h * 0.66f + far.y)

    fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }

    val ceiling = quad(Offset(0f, 0f), Offset(w, 0f), back.topRight, back.topLeft)
    val floor = quad(back.bottomLeft, back.bottomRight, Offset(w, h), Offset(0f, h))
    val leftWall = quad(Offset(0f, 0f), back.topLeft, back.bottomLeft, Offset(0f, h))
    val rightWall = quad(back.topRight, Offset(w, 0f), Offset(w, h), back.bottomRight)

    drawPath(ceiling, Brush.verticalGradient(listOf(Color(0xFFFFF5F8), Color(0xFFFFE6EE)), 0f, back.top))
    drawPath(leftWall, Brush.horizontalGradient(listOf(Color(0xFFF3B5C8), WallBottom), 0f, back.left))
    drawPath(rightWall, Brush.horizontalGradient(listOf(WallBottom, Color(0xFFEBA4BB)), back.right, w))

    // Sol en damier en perspective
    drawPath(floor, Color(0xFFFFF0F4))
    clipPath(floor) {
        val rows = 7
        val cols = 8
        fun floorPoint(u: Float, v: Float): Offset {
            // v = 0 au fond, 1 devant ; interpolation perspective simple (v^1.8 pour l'effet de profondeur)
            val vv = v * v * 0.6f + v * 0.4f
            val l = Offset(back.left + (0f - back.left) * vv, back.bottom + (h - back.bottom) * vv)
            val r = Offset(back.right + (w - back.right) * vv, back.bottom + (h - back.bottom) * vv)
            return Offset(l.x + (r.x - l.x) * u, l.y)
        }
        for (i in 0 until rows) for (j in 0 until cols) {
            if ((i + j) % 2 == 0) continue
            val a = floorPoint(j / cols.toFloat(), i / rows.toFloat())
            val b = floorPoint((j + 1) / cols.toFloat(), i / rows.toFloat())
            val c = floorPoint((j + 1) / cols.toFloat(), (i + 1) / rows.toFloat())
            val d = floorPoint(j / cols.toFloat(), (i + 1) / rows.toFloat())
            drawPath(quad(a, b, c, d), Color(0xFFF8BBD0))
        }
        // assombrir vers le fond
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent), back.bottom, h), Offset(0f, back.bottom), Size(w, h - back.bottom))
    }

    // Mur du fond : papier peint rayé + soubassement
    drawRect(Brush.verticalGradient(listOf(WallTop, WallBottom), back.top, back.bottom), back.topLeft, back.size)
    var x = back.left
    while (x < back.right) {
        drawRect(Color.White.copy(alpha = 0.28f), Offset(x, back.top), Size(back.width * 0.035f, back.height * 0.7f))
        x += back.width * 0.07f
    }
    val wainTop = back.top + back.height * 0.7f
    drawRect(Brush.verticalGradient(listOf(Wainscot, Wainscot.shade(-0.1f)), wainTop, back.bottom), Offset(back.left, wainTop), Size(back.width, back.bottom - wainTop))
    drawRect(Color.White.copy(alpha = 0.6f), Offset(back.left, wainTop), Size(back.width, h * 0.006f))
    // Arêtes de la pièce (ombres douces dans les coins)
    for (edge in listOf(back.left, back.right)) {
        drawLine(Color.Black.copy(alpha = 0.08f), Offset(edge, back.top), Offset(edge, back.bottom), w * 0.01f)
    }

    // Fenêtre avec ciel et nuages sur le mur du fond
    val win = Rect(back.left + back.width * 0.08f, back.top + back.height * 0.12f, back.left + back.width * 0.36f, back.top + back.height * 0.55f)
    drawRoundRect(Color.White, win.topLeft - Offset(w * 0.01f, w * 0.01f), Size(win.width + w * 0.02f, win.height + w * 0.02f), CornerRadius(w * 0.05f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF90CAF9), Color(0xFFFCE4EC)), win.top, win.bottom), win.topLeft, win.size, CornerRadius(w * 0.04f))
    val cloud = Offset(win.left + win.width * (0.35f + sway * 0.08f), win.top + win.height * 0.35f)
    for ((dx, rr) in listOf(-0.12f to 0.1f, 0f to 0.14f, 0.13f to 0.1f)) drawCircle(Color.White, win.width * rr, cloud + Offset(win.width * dx, 0f))
    drawLine(Color.White, Offset(win.center.x, win.top), Offset(win.center.x, win.bottom), w * 0.01f)
    drawLine(Color.White, Offset(win.left, win.center.y), Offset(win.right, win.center.y), w * 0.01f)
    // Rayon de lumière qui tombe sur le sol
    val ray = quad(win.topLeft, win.topRight, Offset(win.right + w * 0.18f + near.x, h), Offset(win.left + w * 0.02f + near.x, h))
    drawPath(ray, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), win.top, h))

    // Étagères avec bocaux et tasses (mur du fond, à droite)
    val shelfX = back.left + back.width * 0.55f
    for (k in 0 until 2) {
        val sy = back.top + back.height * (0.3f + k * 0.2f)
        drawRect(Brush.verticalGradient(listOf(Wood.shade(0.15f), Wood.shade(-0.25f)), sy, sy + h * 0.02f), Offset(shelfX, sy), Size(back.width * 0.38f, h * 0.018f))
        drawRect(Color.Black.copy(alpha = 0.1f), Offset(shelfX, sy + h * 0.018f), Size(back.width * 0.38f, h * 0.012f))
        val jars = listOf(Color(0xFFF48FB1), Color(0xFFFFE082), Color(0xFFA5D6A7), Color(0xFFCE93D8))
        for (j in 0 until 4) {
            val jx = shelfX + back.width * (0.03f + j * 0.09f)
            val jh = h * (0.05f + 0.01f * ((j + k) % 2))
            val jw = back.width * 0.06f
            drawRoundRect(Brush.horizontalGradient(listOf(jars[(j + k) % 4].shade(0.3f), jars[(j + k) % 4], jars[(j + k) % 4].shade(-0.2f)), jx, jx + jw),
                Offset(jx, sy - jh), Size(jw, jh), CornerRadius(jw * 0.25f))
            drawRect(Color.White.copy(alpha = 0.5f), Offset(jx + jw * 0.15f, sy - jh * 0.85f), Size(jw * 0.12f, jh * 0.6f))
        }
    }

    // Décorations sur le mur du fond
    if ("painting" in decor) {
        val pr = Rect(back.left + back.width * 0.42f, back.top + back.height * 0.08f, back.left + back.width * 0.54f, back.top + back.height * 0.3f)
        drawRect(Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFC8A200)), pr.topLeft, pr.bottomRight), pr.topLeft, pr.size)
        val inner = Rect(pr.left + pr.width * 0.12f, pr.top + pr.height * 0.1f, pr.right - pr.width * 0.12f, pr.bottom - pr.height * 0.1f)
        drawRect(Color(0xFFFCE4EC), inner.topLeft, inner.size)
        drawCircle(Color(0xFF9E9E9E), inner.width * 0.3f, Offset(inner.center.x, inner.center.y + inner.height * 0.1f))
        drawPath(quad(Offset(inner.center.x - inner.width * 0.3f, inner.center.y), Offset(inner.center.x - inner.width * 0.25f, inner.top + inner.height * 0.15f), Offset(inner.center.x - inner.width * 0.05f, inner.center.y - inner.height * 0.1f), Offset(inner.center.x - inner.width * 0.2f, inner.center.y)), Color(0xFF9E9E9E))
        drawPath(quad(Offset(inner.center.x + inner.width * 0.3f, inner.center.y), Offset(inner.center.x + inner.width * 0.25f, inner.top + inner.height * 0.15f), Offset(inner.center.x + inner.width * 0.05f, inner.center.y - inner.height * 0.1f), Offset(inner.center.x + inner.width * 0.2f, inner.center.y)), Color(0xFF9E9E9E))
    }
    if ("garland" in decor) {
        for (i in 0 until 12) {
            val u = i / 11f
            val gx = back.left + back.width * u
            val gy = back.top + back.height * 0.05f + kotlin.math.sin(u * Math.PI.toFloat() * 3f) * h * 0.012f
            val col = listOf(Color(0xFFFFF176), Pink.Main, Color(0xFF81D4FA))[i % 3]
            val a = if (i % 2 == 0) twinkle else 1.5f - twinkle
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.6f * a), Color.Transparent), Offset(gx, gy), w * 0.03f), w * 0.03f, Offset(gx, gy))
            drawCircle(col, w * 0.009f, Offset(gx, gy))
        }
    }

    // Tapis au sol (ellipse en perspective)
    if ("rug" in decor) {
        val rc = Offset(w * 0.4f + mid.x, back.bottom + (h - back.bottom) * 0.55f + mid.y)
        drawOval(Brush.radialGradient(listOf(Color(0xFFF8BBD0), Color(0xFFF06292)), rc, w * 0.3f), Offset(rc.x - w * 0.3f, rc.y - h * 0.06f), Size(w * 0.6f, h * 0.12f))
        drawOval(Color.White.copy(alpha = 0.5f), Offset(rc.x - w * 0.24f, rc.y - h * 0.045f), Size(w * 0.48f, h * 0.09f), style = Stroke(w * 0.006f))
    }

    // Piano au fond à droite
    if ("piano" in decor) {
        val px = back.right - back.width * 0.3f + mid.x
        val py = back.bottom - h * 0.16f + mid.y
        val pw = back.width * 0.26f
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF48FB1), Color(0xFFD81B60)), py, py + h * 0.16f), Offset(px, py), Size(pw, h * 0.16f), CornerRadius(w * 0.015f))
        drawRect(Color.White, Offset(px + pw * 0.05f, py + h * 0.06f), Size(pw * 0.9f, h * 0.025f))
        for (k in 0 until 9) drawRect(Color(0xFF3E2723), Offset(px + pw * (0.08f + k * 0.1f), py + h * 0.06f), Size(pw * 0.04f, h * 0.015f))
        drawRect(Color.White.copy(alpha = 0.35f), Offset(px + pw * 0.05f, py + h * 0.01f), Size(pw * 0.9f, h * 0.012f))
    }

    // Arbre à chat sur le mur de gauche (plan intermédiaire)
    if ("cattree" in decor) {
        val tx = w * 0.07f + mid.x
        drawRect(Brush.horizontalGradient(listOf(Color(0xFFD7CCC8), Color(0xFFA1887F)), tx, tx + w * 0.035f), Offset(tx, h * 0.35f + mid.y), Size(w * 0.035f, h * 0.65f))
        for (k in 0 until 2) {
            val py = h * (0.35f + k * 0.28f) + mid.y
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE1BEE7), Color(0xFFBA68C8)), py, py + h * 0.05f), Offset(tx - w * 0.06f, py), Size(w * 0.15f, h * 0.05f), CornerRadius(w * 0.02f))
        }
    }

    // Plante au premier plan à droite
    if ("plant" in decor) drawPlant3D(Offset(w * 0.9f + near.x, h * 0.98f + near.y), w * 0.07f, sway)

    // Suspensions lumineuses (du plafond)
    val lamps = if ("lamp" in decor) listOf(0.3f, 0.5f, 0.7f) else listOf(0.5f)
    for (lx in lamps) {
        val cx = w * lx + mid.x + sway * w * 0.005f
        val cy = back.top + h * 0.02f + mid.y
        drawLine(Color(0xFF8D6E63), Offset(w * lx + far.x, 0f), Offset(cx, cy - h * 0.03f), w * 0.004f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF9C4).copy(alpha = 0.55f * twinkle), Color.Transparent), Offset(cx, cy + h * 0.02f), w * 0.14f), w * 0.14f, Offset(cx, cy + h * 0.02f))
        val shade = Path().apply {
            moveTo(cx - w * 0.015f, cy - h * 0.03f); lineTo(cx + w * 0.015f, cy - h * 0.03f)
            lineTo(cx + w * 0.045f, cy + h * 0.01f); lineTo(cx - w * 0.045f, cy + h * 0.01f); close()
        }
        drawPath(shade, Brush.horizontalGradient(listOf(Pink.Main.shade(0.2f), Pink.Deep), cx - w * 0.045f, cx + w * 0.045f))
        drawOval(Color(0xFFFFFDE7), Offset(cx - w * 0.02f, cy + h * 0.004f), Size(w * 0.04f, h * 0.014f))
    }
}

private fun DrawScope.drawPlant3D(base: Offset, s: Float, sway: Float) {
    for (k in -2..2) {
        val leafTop = Offset(base.x + k * s * 0.45f + sway * s * 0.1f, base.y - s * 2.6f + kotlin.math.abs(k) * s * 0.5f)
        val p = Path().apply {
            moveTo(base.x, base.y - s * 1.1f)
            quadraticBezierTo(leafTop.x - s * 0.4f, (base.y - s * 1.1f + leafTop.y) / 2f, leafTop.x, leafTop.y)
            quadraticBezierTo(leafTop.x + s * 0.4f, (base.y - s * 1.1f + leafTop.y) / 2f, base.x, base.y - s * 1.1f)
        }
        drawPath(p, Brush.linearGradient(listOf(Color(0xFFA5D6A7), Color(0xFF388E3C)), leafTop, Offset(base.x, base.y - s)))
    }
    val pot = Path().apply {
        moveTo(base.x - s, base.y - s * 1.2f); lineTo(base.x + s, base.y - s * 1.2f)
        lineTo(base.x + s * 0.7f, base.y); lineTo(base.x - s * 0.7f, base.y); close()
    }
    drawPath(pot, Brush.horizontalGradient(listOf(Color(0xFFFCE4EC), Color(0xFFF48FB1), Color(0xFFC2185B)), base.x - s, base.x + s))
    drawOval(Color(0xFFF8BBD0), Offset(base.x - s, base.y - s * 1.3f), Size(s * 2f, s * 0.25f))
    drawOval(Color(0xFF6D4C41), Offset(base.x - s * 0.85f, base.y - s * 1.27f), Size(s * 1.7f, s * 0.17f))
}

/** Comptoir en bois vu de face, avec plateau brillant et tranche ombrée. */
@Composable
fun CounterTop(modifier: Modifier, tilt: Offset = Offset.Zero) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val topH = h * 0.2f
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF1C9B8), Color(0xFFE2A992)), 0f, topH), Offset.Zero, Size(w, topH))
        drawRect(Color.White.copy(alpha = 0.55f), Offset(0f, 0f), Size(w, h * 0.02f))
        // reflet qui glisse avec l'inclinaison
        val rx = w * (0.35f + tilt.x * 0.15f)
        drawOval(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), Offset(rx, topH * 0.5f), w * 0.3f), Offset(rx - w * 0.3f, 0f), Size(w * 0.6f, topH))
        drawRect(Brush.verticalGradient(listOf(Color(0xFFC98B74), Color(0xFFA86D58)), topH, h), Offset(0f, topH), Size(w, h - topH))
        drawRect(Color.Black.copy(alpha = 0.18f), Offset(0f, topH), Size(w, h * 0.03f))
        // lattes de bois
        var x = w * 0.12f
        while (x < w) {
            drawLine(Color.Black.copy(alpha = 0.08f), Offset(x, topH), Offset(x, h), w * 0.004f, StrokeCap.Butt)
            x += w * 0.16f
        }
        // festons roses décoratifs
        val scallopY = topH + h * 0.03f
        var sx = 0f
        while (sx < w) {
            drawArc(Pink.Main.copy(alpha = 0.7f), 0f, 180f, true, Offset(sx, scallopY - w * 0.03f), Size(w * 0.08f, w * 0.06f))
            sx += w * 0.08f
        }
    }
}
