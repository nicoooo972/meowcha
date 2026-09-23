package com.meowcha.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowcha.game.data.AccountEntity
import com.meowcha.game.game.Cats
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.Mood
import com.meowcha.game.game.Mugs
import com.meowcha.game.game.SessionState
import com.meowcha.game.game.SessionViewModel

/* ------------------------------ Écran de chargement ------------------------------ */

@Composable
fun LoadingScreen(state: SessionState.Loading, onSkip: () -> Unit) {
    val progress = state.progress
    val anim = rememberInfiniteTransition(label = "loading")
    val bounce by anim.animateFloat(0f, -18f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "bounce")
    val tilt by anim.animateFloat(-6f, 6f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "tilt")
    val shown by animateFloatAsState(progress, tween(150), label = "progress")
    // La tasse se remplit au fil du chargement
    val fill = listOf(Ingredient.MATCHA, Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.CREAM, Ingredient.SAKURA)
        .take((progress * 5).toInt().coerceIn(0, 5))

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            FloatingHearts(Modifier.fillMaxSize())
            MugView(
                Mugs.byId("kitty"), fill,
                Modifier.size(160.dp).graphicsLayer { translationY = bounce * density; rotationZ = tilt },
            )
        }
        Spacer(Modifier.height(20.dp))
        Title("Meowcha Café", 44)
        Spacer(Modifier.height(20.dp))
        Text(state.title, color = Pink.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        PawProgress(shown, Modifier.fillMaxWidth().height(36.dp))
        Spacer(Modifier.height(8.dp))
        Text("${(shown * 100).toInt()} %", color = Pink.Deep, fontWeight = FontWeight.Bold)
        state.detail?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = Pink.Text, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (state.canSkip) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Plus tard ›",
                Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).clickable { onSkip() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Pink.Deep, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(state.tip, color = Pink.Text, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Barre de progression faite d'empreintes de pattes qui se colorent. */
@Composable
private fun PawProgress(progress: Float, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.7f))) {
        val n = 8
        val step = size.width / n
        for (i in 0 until n) {
            val lit = (i + 1) / n.toFloat() <= progress + 0.001f
            val y = size.height * (if (i % 2 == 0) 0.45f else 0.6f)
            drawPaw(Offset(step * (i + 0.5f), y), size.height * 0.36f, if (lit) Pink.Main else Pink.Light.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun FloatingHearts(modifier: Modifier) {
    val anim = rememberInfiniteTransition(label = "hearts")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(3000)), label = "t")
    Canvas(modifier) {
        for (i in 0 until 6) {
            val phase = (t + i / 6f) % 1f
            val x = size.width * (0.12f + 0.15f * i)
            val y = size.height * (1f - phase)
            drawHeart(Offset(x, y), size.width * 0.035f, Pink.Main.copy(alpha = (1f - phase) * 0.7f))
        }
    }
}

/* --------------------------- Connexion / création de compte --------------------------- */

@Composable
fun AuthScreen(session: SessionViewModel, profiles: List<AccountEntity>) {
    val error by session.error.collectAsState()
    val busy by session.busy.collectAsState()
    var register by rememberSaveable { mutableStateOf(profiles.isEmpty()) }
    var name by rememberSaveable { mutableStateOf(profiles.firstOrNull()?.displayName ?: "") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf("mochi") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        CatPortrait(Cats.byId(if (register) avatar else profiles.firstOrNull { it.displayName.equals(name.trim(), true) }?.avatarCat ?: "mochi"),
            if (error != null) Mood.SAD else Mood.HAPPY, Modifier.size(140.dp))
        Title("Meowcha Café", 40)
        Text(if (register) "Crée ton compte de barista 🎀" else "Bon retour parmi nous ! 💕", color = Pink.Text)
        Spacer(Modifier.height(18.dp))

        // Onglets
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.7f)).padding(4.dp),
        ) {
            listOf(false to "Se connecter", true to "Créer un compte").forEach { (isRegister, label) ->
                val selected = register == isRegister
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                        .background(if (selected) Pink.Main else Color.Transparent)
                        .clickable { register = isRegister; session.clearError() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (selected) Color.White else Pink.Text, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!register && profiles.isNotEmpty()) {
                Text("Profils sur ce téléphone", color = Pink.Text, fontSize = 13.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    profiles.forEach { p ->
                        val selected = p.displayName.equals(name.trim(), true)
                        Column(
                            Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (selected) Pink.Light else Pink.Bg)
                                .clickable { name = p.displayName; password = ""; session.clearError() }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CatPortrait(Cats.byId(p.avatarCat), Mood.HAPPY, Modifier.size(56.dp))
                            Text(p.displayName, fontSize = 12.sp, color = Pink.Text, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            CuteField(name, { name = it }, "Pseudo", "🐱")
            CuteField(
                password, { password = it }, "Mot de passe", "🔒", password = !showPassword,
                trailing = { Text(if (showPassword) "🙈" else "👁️", Modifier.clickable { showPassword = !showPassword }.padding(8.dp)) },
            )
            if (register) {
                CuteField(confirm, { confirm = it }, "Confirmer le mot de passe", "🔒", password = !showPassword)
                Text("Choisis ton avatar", color = Pink.Text, fontSize = 13.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cats.all.forEach { cat ->
                        Box(
                            Modifier.size(60.dp).clip(CircleShape)
                                .background(if (avatar == cat.id) Pink.Light else Pink.Bg)
                                .border(if (avatar == cat.id) 3.dp else 0.dp, if (avatar == cat.id) Pink.Deep else Color.Transparent, CircleShape)
                                .clickable { avatar = cat.id },
                        ) { CatPortrait(cat, Mood.HAPPY, Modifier.fillMaxSize()) }
                    }
                }
            }
            AnimatedContent(error, label = "error") { e ->
                if (e != null) {
                    Text("⚠️ $e", color = Color(0xFFD32F2F), fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEBEE)).padding(10.dp))
                }
            }
            CuteButton(
                when {
                    busy -> "⏳ Un instant..."
                    register -> "🎀 Créer mon café"
                    else -> "🐾 Entrer dans le café"
                },
                Modifier.fillMaxWidth(), enabled = !busy,
            ) {
                if (register) session.register(name, password, confirm, avatar) else session.login(name, password)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Ton compte et ta progression sont enregistrés uniquement sur ce téléphone.",
            fontSize = 11.sp, color = Pink.Text.copy(alpha = 0.7f), textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CuteField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    emoji: String,
    password: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Text(emoji) },
        trailingIcon = trailing,
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (label.startsWith("Mot") || label.startsWith("Confirmer")) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Pink.Main,
            unfocusedBorderColor = Pink.Light,
            focusedLabelColor = Pink.Deep,
            cursorColor = Pink.Deep,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
