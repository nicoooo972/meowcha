package com.meowcha.game.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.meowcha.game.data.AccountEntity
import com.meowcha.game.game.Audio
import com.meowcha.game.game.Cats
import com.meowcha.game.game.DayState
import com.meowcha.game.game.Decor
import com.meowcha.game.game.Decors
import com.meowcha.game.game.GameViewModel
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.Mood
import com.meowcha.game.game.Mug
import com.meowcha.game.game.Mugs
import com.meowcha.game.game.Recipes
import com.meowcha.game.game.SessionState
import com.meowcha.game.game.SessionViewModel

enum class Screen { HOME, GAME, SHOP, ALBUM, RECIPES }

@Composable
fun MeowchaApp(session: SessionViewModel = viewModel()) {
    val state by session.state.collectAsState()
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pink.Light, Pink.Bg, Pink.Cream)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
            label = "session",
        ) { s ->
            when (s) {
                is SessionState.Loading -> LoadingScreen(s, onSkip = session::skipDownload)
                is SessionState.LoggedOut -> AuthScreen(session, s.profiles)
                is SessionState.LoggedIn -> CafeApp(s.account, onLogout = session::logout)
            }
        }
    }
}

@Composable
private fun CafeApp(account: AccountEntity, onLogout: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: GameViewModel = viewModel(
        key = "game-${account.username}",
        factory = viewModelFactory { initializer { GameViewModel(app, account.username) } },
    )
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var confirmQuit by remember { mutableStateOf(false) }
    val day by vm.day.collectAsState()

    val back: () -> Unit = {
        if (screen == Screen.GAME && day?.finished == false) confirmQuit = true
        else { vm.quitDay(); screen = Screen.HOME }
    }
    BackHandler(enabled = screen != Screen.HOME) { back() }
    LaunchedEffect(screen) { Audio.playMusic(if (screen == Screen.GAME) "cafe" else "home") }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("Fermer le café ? 🙀") },
            text = { Text("Les gains de la journée en cours seront perdus.") },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; vm.quitDay(); screen = Screen.HOME }) { Text("Fermer", color = Pink.Deep) }
            },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Continuer à servir") } },
            containerColor = Pink.Cream,
        )
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.HOME -> HomeScreen(vm, account, onLogout) { t ->
                if (t == Screen.GAME) vm.startDay()
                screen = t
            }
            Screen.GAME -> GameScreen(vm, onExit = back)
            Screen.SHOP -> ShopScreen(vm, onBack = back)
            Screen.ALBUM -> AlbumScreen(vm, onBack = back)
            Screen.RECIPES -> RecipesScreen(vm, onBack = back)
        }
    }
}

/* ---------------------------------- Accueil ---------------------------------- */

@Composable
private fun HomeScreen(vm: GameViewModel, account: AccountEntity, onLogout: () -> Unit, go: (Screen) -> Unit) {
    val player by vm.player.collectAsState()
    val decor by vm.ownedDecor.collectAsState()
    val bob = rememberInfiniteTransition(label = "bob")
    val dy by bob.animateFloat(0f, -10f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dy")
    val objectives = remember(player.day) { vm.upcomingObjectives(player.day) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Profil
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.8f)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Pink.Light)) {
                CatPortrait(Cats.byId(account.avatarCat), Mood.HAPPY, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(account.displayName, fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 17.sp)
                Text("Barista • jour ${player.day}", color = Pink.Text, fontSize = 12.sp)
            }
            Coins(player.coins)
            Spacer(Modifier.width(6.dp))
            val ctx = LocalContext.current
            var musicOn by remember { mutableStateOf(Audio.musicOn) }
            if (Audio.hasContent) {
                RoundIconButton(if (musicOn) "🔊" else "🔇") {
                    musicOn = !musicOn
                    Audio.setMusic(ctx, musicOn)
                    Audio.setSfx(ctx, musicOn)
                }
                Spacer(Modifier.width(6.dp))
            }
            RoundIconButton("⎋", onLogout)
        }

        Title("Meowcha Café", 44)

        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp))) {
            CafeBackdrop(decor, Modifier.fillMaxSize())
            CatPortrait(Cats.byId(account.avatarCat), Mood.HAPPY,
                Modifier.size(160.dp).align(Alignment.BottomStart).padding(start = 12.dp).graphicsLayer { translationY = dy * density })
            MugView(Mugs.byId(player.equippedMug), listOf(Ingredient.ESPRESSO, Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.FOAM),
                Modifier.size(110.dp).align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatTile("⭐", "${player.perfectServed}", "parfaites")
            StatTile("🔥", "x${player.bestCombo}", "meilleur combo")
            StatTile("🐱", "${player.totalServed}", "chats servis")
        }

        // Objectifs du jour
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("🎯 Objectifs du jour ${player.day}", fontWeight = FontWeight.Bold, color = Pink.Deep)
            objectives.forEach { o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(o.emoji)
                    Spacer(Modifier.width(8.dp))
                    Text(o.label, Modifier.weight(1f), color = Pink.Text, fontSize = 14.sp)
                    Text("+${o.reward} 🪙", color = Pink.Deep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        CuteButton("🐾  Ouvrir le café", Modifier.fillMaxWidth().height(60.dp), Pink.Deep) { go(Screen.GAME) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CuteButton("🛍️ Boutique", Modifier.weight(1f), Pink.Main) { go(Screen.SHOP) }
            CuteButton("📸 Album", Modifier.weight(1f), Color(0xFFBA68C8)) { go(Screen.ALBUM) }
        }
        CuteButton("📖 Carnet de recettes", Modifier.fillMaxWidth(), Color(0xFFF48FB1)) { go(Screen.RECIPES) }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String) {
    Column(
        Modifier.width(100.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.8f)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.ExtraBold, color = Pink.Deep, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, color = Pink.Text, textAlign = TextAlign.Center)
    }
}

/* ----------------------------------- Jeu ------------------------------------ */

@Composable
private fun GameScreen(vm: GameViewModel, onExit: () -> Unit) {
    val state by vm.day.collectAsState()
    val player by vm.player.collectAsState()
    val decor by vm.ownedDecor.collectAsState()
    val haptic = LocalHapticFeedback.current
    val s = state ?: return
    val mug = Mugs.byId(player.equippedMug)

    if (s.finished) {
        DaySummary(s, onExit)
        return
    }
    val order = s.current ?: return
    var showSteps by remember(order.id) { mutableStateOf(false) }

    // Arrivée du chat qui glisse depuis la gauche
    val enter = remember(order.id) { Animatable(-1.2f) }
    LaunchedEffect(order.id) { enter.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
    // Petit "squish" + cœurs quand on caresse
    val petAnim = remember { Animatable(1f) }
    LaunchedEffect(s.petPulse) {
        if (s.petPulse > 0) {
            petAnim.snapTo(0f)
            petAnim.animateTo(1f, tween(900))
        }
    }
    // Pop du badge de résultat
    val pop = remember(order.id) { Animatable(0f) }
    LaunchedEffect(s.lastResult) {
        s.lastResult?.let { r ->
            Audio.play(if (r.stars == 3) "perfect" else if (r.stars >= 2) "coin" else "sad")
        }
        if (s.lastResult != null) pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton("✕", onExit)
            Spacer(Modifier.width(8.dp))
            Chip("📅 ${s.day}")
            Spacer(Modifier.width(6.dp))
            Chip("🐱 ${minOf(s.served + s.left + 1, s.totalCustomers)}/${s.totalCustomers}")
            Spacer(Modifier.width(6.dp))
            if (s.combo >= 2) Chip("🔥 x${s.combo}", Color(0xFFFFE0B2), Color(0xFFE65100))
            Spacer(Modifier.weight(1f))
            Coins(player.coins + s.coinsToday)
        }
        // Objectifs en direct
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.objectives.forEach { o ->
                val done = s.done(o)
                Text(
                    if (done) "${o.emoji} ✓" else "${o.emoji} ${minOf(s.progress(o), o.target)}/${o.target}",
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (done) Color(0xFFC8E6C9) else Color.White.copy(alpha = 0.7f)).padding(vertical = 4.dp),
                    fontSize = 12.sp, textAlign = TextAlign.Center, color = Pink.Text, fontWeight = FontWeight.Bold,
                )
            }
        }

        // Scène : chat + bulle
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val sceneWidth = constraints.maxWidth.toFloat()
            CafeBackdrop(decor, Modifier.fillMaxSize())
            Box(
                Modifier.size(220.dp).align(Alignment.BottomStart).padding(start = 4.dp)
                    .graphicsLayer {
                        translationX = enter.value * sceneWidth
                        val squish = if (petAnim.value < 1f) 1f + 0.06f * kotlin.math.sin(petAnim.value * Math.PI.toFloat() * 3f) else 1f
                        scaleX = squish; scaleY = 2f - squish
                    }
                    .clickable(enabled = !s.reacting) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!s.petted) Audio.play("purr")
                        vm.pet()
                    },
            ) {
                if (order.vip) VipGlow(Modifier.fillMaxSize())
                CatPortrait(order.cat, if (petAnim.value < 1f && s.lastResult == null) Mood.DELIGHTED else s.mood, Modifier.fillMaxSize())
                if (petAnim.value < 1f) PetHearts(petAnim.value, Modifier.fillMaxSize())
                if (order.vip) {
                    Box(Modifier.align(Alignment.TopCenter).padding(top = 4.dp)) { Chip("👑 VIP ×2", Color(0xFFFFF8E1), Color(0xFFF57F17)) }
                }
            }
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp).width(190.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { showSteps = !showSteps }
                    .padding(12.dp),
            ) {
                Text(order.cat.name, fontWeight = FontWeight.Bold, color = Pink.Deep)
                val result = s.lastResult
                if (result != null) {
                    Text(result.message, color = Pink.Text, fontSize = 14.sp)
                } else {
                    Text("« ${order.recipe.name} »", fontWeight = FontWeight.ExtraBold, color = Pink.Text, fontSize = 16.sp)
                    if (showSteps) {
                        Text(order.recipe.steps.joinToString(" → ") { it.emoji }, fontSize = 18.sp)
                    } else {
                        Text("(touche pour voir la recette)", fontSize = 11.sp, color = Color.Gray)
                    }
                    if (!s.petted) Text("🐾 caresse-moi !", fontSize = 11.sp, color = Pink.Main)
                }
            }
            val ratio = (s.patienceLeft / order.patienceMax).coerceIn(0f, 1f)
            Column(Modifier.align(Alignment.BottomEnd).padding(12.dp).width(150.dp)) {
                Text(if (ratio > 0.3f) "Patience 💗" else "Patience 💔", fontSize = 12.sp, color = Pink.Text)
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = if (ratio > 0.3f) Pink.Main else Color(0xFFE53935),
                    trackColor = Color.White,
                )
                if (s.queue.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("En attente : " + s.queue.take(5).joinToString("") { if (it.vip) "👑" else "🐱" }, fontSize = 11.sp, color = Pink.Text)
                }
            }
            val r = s.lastResult
            if (r != null) {
                Box(Modifier.align(Alignment.Center).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    ResultBadge(r.stars, r.coins + r.tip, r.photo, r.combo)
                }
            }
        }

        // Comptoir : mug
        Box(
            Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color(0xFFD7A08C), Pink.Wood))),
            contentAlignment = Alignment.Center,
        ) {
            MugView(mug, s.cup, Modifier.size(140.dp).padding(top = 12.dp))
            if (s.cup.isNotEmpty()) {
                Text(
                    s.cup.joinToString(" ") { it.emoji },
                    Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.85f)).padding(4.dp),
                    fontSize = 13.sp,
                )
            }
        }

        // Ingrédients
        val available = Ingredient.entries.filter { it.unlockDay <= s.day }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height(180.dp).background(Pink.Cream),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(available) { ing ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(2.dp, ing.color.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                        .clickable(enabled = !s.reacting && s.cup.size < 6) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            Audio.play("add")
                            vm.add(ing)
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(ing.emoji, fontSize = 22.sp)
                    Text(ing.label, fontSize = 10.sp, color = Pink.Text, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Pink.Cream).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val canEdit = s.cup.isNotEmpty() && !s.reacting
            CuteButton("↩️", Modifier.weight(0.8f), Color(0xFFCE93D8), enabled = canEdit) { vm.undo() }
            CuteButton("🗑️", Modifier.weight(0.8f), Color(0xFFBDBDBD), enabled = canEdit) { vm.trash() }
            CuteButton("💝 Servir", Modifier.weight(2f), Pink.Deep, enabled = canEdit) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.serve()
            }
        }
    }
}

@Composable
private fun VipGlow(modifier: Modifier) {
    val anim = rememberInfiniteTransition(label = "vip")
    val a by anim.animateFloat(0.25f, 0.6f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Canvas(modifier) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD54F).copy(alpha = a), Color.Transparent)), size.minDimension * 0.5f)
    }
}

@Composable
private fun PetHearts(t: Float, modifier: Modifier) {
    Canvas(modifier) {
        for (i in 0 until 5) {
            val x = size.width * (0.3f + 0.1f * i)
            val y = size.height * (0.35f - 0.3f * t) - (i % 2) * 20f
            drawHeart(Offset(x, y), size.width * 0.04f, Pink.Main.copy(alpha = 1f - t))
        }
    }
}

@Composable
private fun ResultBadge(stars: Int, coins: Int, photo: Boolean, combo: Int) {
    Column(
        Modifier.shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text((1..3).joinToString("") { if (it <= stars) "⭐" else "☆" }, fontSize = 30.sp)
        Text("+$coins 🪙", fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 20.sp)
        if (combo >= 2) Text("Combo x$combo 🔥", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
        if (photo) Text("📸 Photo souvenir !", color = Pink.Text, fontSize = 13.sp)
    }
}

/** Décor du café : fenêtre, guirlande, et les décorations achetées. */
@Composable
fun CafeBackdrop(decor: Set<String>, modifier: Modifier) {
    val anim = rememberInfiniteTransition(label = "twinkle")
    val twinkle by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "tw")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        // Mur à rayures
        var x = 0f
        while (x < w) { drawRect(Color.White.copy(alpha = 0.25f), Offset(x, 0f), Size(w * 0.05f, h)); x += w * 0.1f }
        if ("rug" in decor) drawOval(Color(0xFFF48FB1), Offset(w * 0.05f, h * 0.86f), Size(w * 0.5f, h * 0.16f))
        // Fenêtre
        drawRoundRect(Color(0xFFB3E5FC), Offset(w * 0.05f, h * 0.1f), Size(w * 0.3f, h * 0.32f), CornerRadius(24f))
        drawCircle(Color.White, w * 0.03f, Offset(w * 0.13f, h * 0.2f))
        drawCircle(Color.White, w * 0.04f, Offset(w * 0.17f, h * 0.2f))
        drawLine(Color.White, Offset(w * 0.2f, h * 0.1f), Offset(w * 0.2f, h * 0.42f), 6f)
        drawLine(Color.White, Offset(w * 0.05f, h * 0.26f), Offset(w * 0.35f, h * 0.26f), 6f)
        // Guirlande de cœurs ou lumineuse
        for (i in 0 until 9) {
            val c = Offset(w * (0.05f + i * 0.11f), h * 0.04f + (i % 2) * 8f)
            if ("garland" in decor) drawCircle(listOf(Color(0xFFFFF176), Pink.Main, Color(0xFF81D4FA))[i % 3].copy(alpha = if (i % 2 == 0) twinkle else 1.4f - twinkle), 9f, c)
            else drawHeart(c, 14f, if (i % 2 == 0) Pink.Main else Color.White)
        }
        // Étagère
        drawRect(Color(0xFFD7A08C), Offset(w * 0.45f, h * 0.55f), Size(w * 0.5f, 10f))
        if ("painting" in decor) {
            drawRect(Color(0xFFFFD54F), Offset(w * 0.42f, h * 0.12f), Size(w * 0.16f, h * 0.2f))
            drawRect(Pink.Light, Offset(w * 0.435f, h * 0.135f), Size(w * 0.13f, h * 0.17f))
            drawCircle(Color.White, w * 0.035f, Offset(w * 0.5f, h * 0.23f))
        }
        if ("plant" in decor) drawPlant(Offset(w * 0.52f, h * 0.55f), w * 0.05f)
        if ("lamp" in decor) {
            drawLine(Color(0xFF8D6E63), Offset(w * 0.9f, 0f), Offset(w * 0.9f, h * 0.12f), 3f)
            drawCircle(Color(0xFFFFF9C4).copy(alpha = twinkle * 0.5f), w * 0.09f, Offset(w * 0.9f, h * 0.16f))
            drawCircle(Color.White, w * 0.04f, Offset(w * 0.87f, h * 0.16f))
            drawCircle(Color.White, w * 0.05f, Offset(w * 0.92f, h * 0.15f))
        }
        if ("piano" in decor) {
            drawRoundRect(Color(0xFFF06292), Offset(w * 0.66f, h * 0.36f), Size(w * 0.2f, h * 0.18f), CornerRadius(8f))
            for (k in 0 until 6) drawRect(Color.White, Offset(w * 0.67f + k * w * 0.03f, h * 0.44f), Size(w * 0.025f, h * 0.06f))
        }
        if ("cattree" in decor) {
            drawRect(Color(0xFFBCAAA4), Offset(w * 0.9f, h * 0.35f), Size(w * 0.03f, h * 0.65f))
            drawRoundRect(Color(0xFFE1BEE7), Offset(w * 0.84f, h * 0.35f), Size(w * 0.15f, h * 0.05f), CornerRadius(10f))
            drawRoundRect(Color(0xFFE1BEE7), Offset(w * 0.84f, h * 0.65f), Size(w * 0.15f, h * 0.05f), CornerRadius(10f))
        }
    }
}

private fun DrawScope.drawPlant(base: Offset, s: Float) {
    for (k in -2..2) {
        drawOval(Color(0xFF81C784), Offset(base.x + k * s * 0.5f - s * 0.3f, base.y - s * 2.2f + kotlin.math.abs(k) * s * 0.4f), Size(s * 0.6f, s * 1.6f))
    }
    val pot = androidx.compose.ui.graphics.Path().apply {
        moveTo(base.x - s, base.y - s * 1.1f); lineTo(base.x + s, base.y - s * 1.1f)
        lineTo(base.x + s * 0.7f, base.y); lineTo(base.x - s * 0.7f, base.y); close()
    }
    drawPath(pot, Color(0xFFF8BBD0))
    drawPath(pot, Color(0xFF6A1B4D), style = Stroke(2f))
}

@Composable
private fun DaySummary(s: DayState, onExit: () -> Unit) {
    val rating = when {
        s.left == 0 && s.perfect >= s.totalCustomers * 0.8f -> 3
        s.perfect >= s.totalCustomers / 2 -> 2
        else -> 1
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Title("Fin du jour ${s.day} !", 38)
        Text("⭐".repeat(rating) + "☆".repeat(3 - rating), fontSize = 34.sp)
        CatPortrait(Cats.all[s.day % Cats.all.size], Mood.DELIGHTED, Modifier.size(150.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryLine("🐱 Chats servis", "${s.served}/${s.totalCustomers}")
            SummaryLine("⭐ Boissons parfaites", "${s.perfect}")
            SummaryLine("🔥 Meilleur combo", "x${s.bestCombo}")
            SummaryLine("🐾 Caresses", "${s.pets}")
            if (s.left > 0) SummaryLine("😿 Chats partis", "${s.left}")
            SummaryLine("🪙 Ventes et pourboires", "+${s.coinsToday}")
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🎯 Objectifs", fontWeight = FontWeight.Bold, color = Pink.Deep)
            s.objectives.forEach { o ->
                val done = s.done(o)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "✅" else "❌")
                    Spacer(Modifier.width(8.dp))
                    Text(o.label, Modifier.weight(1f), color = Pink.Text, fontSize = 14.sp)
                    Text(if (done) "+${o.reward}" else "—", color = Pink.Deep, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Total : +${s.coinsToday + s.objectiveRewards} 🪙", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Pink.Deep)
        CuteButton("🏠 Retour au café", Modifier.fillMaxWidth(), onClick = onExit)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = Pink.Text, fontSize = 16.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 16.sp)
    }
}

/* --------------------------------- Boutique --------------------------------- */

@Composable
private fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val player by vm.player.collectAsState()
    val owned by vm.ownedMugs.collectAsState()
    val decor by vm.ownedDecor.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Boutique", player.coins, onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.7f)).padding(4.dp),
        ) {
            listOf("☕ Mugs", "🪴 Décoration").forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                        .background(if (tab == i) Pink.Main else Color.Transparent)
                        .clickable { tab = i }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (tab == i) Color.White else Pink.Text, fontWeight = FontWeight.Bold) }
            }
        }
        if (tab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(Mugs.all) { mug ->
                    MugCard(mug, owned = mug.id in owned, equipped = player.equippedMug == mug.id, canBuy = player.coins >= mug.price,
                        onBuy = { vm.buyMug(mug) }, onEquip = { vm.equipMug(mug) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp))) {
                        CafeBackdrop(decor, Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.BottomCenter).padding(8.dp)) { Chip("Aperçu de ton café") }
                    }
                }
                items(Decors.all) { d -> DecorCard(d, d.id in decor, player.coins >= d.price) { vm.buyDecor(d) } }
            }
        }
    }
}

@Composable
private fun DecorCard(d: Decor, owned: Boolean, canBuy: Boolean, onBuy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(Pink.Bg), contentAlignment = Alignment.Center) { Text(d.emoji, fontSize = 28.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, fontWeight = FontWeight.Bold, color = Pink.Text)
            Text(d.description, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        if (owned) Text("💖 Installé", color = Pink.Deep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        else CuteButton("${d.price} 🪙", Modifier.width(110.dp), enabled = canBuy, onClick = onBuy)
    }
}

@Composable
private fun MugCard(mug: Mug, owned: Boolean, equipped: Boolean, canBuy: Boolean, onBuy: () -> Unit, onEquip: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(if (equipped) 3.dp else 0.dp, if (equipped) Pink.Deep else Color.Transparent, RoundedCornerShape(20.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MugView(mug, emptyList(), Modifier.size(110.dp).padding(top = 10.dp))
        Text(mug.name, fontWeight = FontWeight.Bold, color = Pink.Text, textAlign = TextAlign.Center)
        Text(if (mug.tipBonus > 0) "Pourboires +${mug.tipBonus}%" else "Le mug de base", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(6.dp))
        when {
            equipped -> Text("💖 Utilisé", color = Pink.Deep, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
            owned -> CuteButton("Utiliser", Modifier.fillMaxWidth(), Color(0xFFBA68C8), onClick = onEquip)
            else -> CuteButton("${mug.price} 🪙", Modifier.fillMaxWidth(), enabled = canBuy, onClick = onBuy)
        }
    }
}

/* ---------------------------------- Album ----------------------------------- */

@Composable
private fun AlbumScreen(vm: GameViewModel, onBack: () -> Unit) {
    val player by vm.player.collectAsState()
    val met by vm.catsMet.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Album photo", player.coins, onBack)
        Text(
            "${met.size}/${Cats.all.size} chats rencontrés",
            Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Pink.Text,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(Cats.all.withIndex().toList()) { (i, cat) ->
                val info = met[cat.id]
                // Carte façon polaroïd légèrement penchée
                Column(
                    Modifier
                        .rotate(if (i % 2 == 0) -3f else 3f)
                        .shadow(4.dp)
                        .background(Color.White)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).background(if (info != null) Pink.Light else Color(0xFFE0E0E0))) {
                        if (info != null) {
                            CatPortrait(cat, if (info.hearts >= 5) Mood.DELIGHTED else Mood.HAPPY, Modifier.fillMaxSize())
                        } else {
                            Text("?", Modifier.align(Alignment.Center), fontSize = 48.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (info != null) {
                        Text(cat.name, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Pink.Deep)
                        Text("💗".repeat((info.hearts + 1) / 2).ifEmpty { "🤍" }, fontSize = 12.sp)
                        Text("Servie ${info.timesServed}×", fontSize = 11.sp, color = Color.Gray)
                        val fav = Recipes.all.firstOrNull { it.id == cat.favorite }
                        Text(if (info.hearts >= 3 && fav != null) "Adore : ${fav.name}" else "Préférence : ???", fontSize = 11.sp, color = Pink.Text, textAlign = TextAlign.Center)
                        if (info.hearts >= 6) Text("« ${cat.quote} »", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    } else {
                        Text("???", fontFamily = FontFamily.Cursive, fontSize = 20.sp, color = Color.Gray)
                        Text("Pas encore venue", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

/* --------------------------------- Recettes --------------------------------- */

@Composable
private fun RecipesScreen(vm: GameViewModel, onBack: () -> Unit) {
    val player by vm.player.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Carnet de recettes", player.coins, onBack)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Recipes.all) { r ->
                val unlocked = r.unlockDay <= player.day
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (unlocked) Color.White else Color.White.copy(alpha = 0.5f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MugView(Mugs.byId(player.equippedMug), if (unlocked) r.steps else emptyList(), Modifier.size(70.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (unlocked) r.name else "🔒 ${r.name}", fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 17.sp)
                        if (unlocked) {
                            Text(r.steps.joinToString(" → ") { "${it.emoji} ${it.label}" }, fontSize = 12.sp, color = Pink.Text)
                        } else {
                            Text("Débloquée au jour ${r.unlockDay}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Text("${r.price} 🪙", fontWeight = FontWeight.Bold, color = Pink.Text)
                }
            }
        }
    }
}
