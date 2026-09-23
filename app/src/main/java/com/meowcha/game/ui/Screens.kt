package com.meowcha.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meowcha.game.game.Cats
import com.meowcha.game.game.DayState
import com.meowcha.game.game.GameViewModel
import com.meowcha.game.game.Ingredient
import com.meowcha.game.game.Mood
import com.meowcha.game.game.Mug
import com.meowcha.game.game.Mugs
import com.meowcha.game.game.Recipes

enum class Screen { HOME, GAME, SHOP, ALBUM, RECIPES }

@Composable
fun MeowchaApp(vm: GameViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val back = { if (screen == Screen.GAME) vm.quitDay(); screen = Screen.HOME }
    BackHandler(enabled = screen != Screen.HOME) { back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pink.Light, Pink.Bg, Pink.Cream)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (screen) {
            Screen.HOME -> HomeScreen(vm) { target ->
                if (target == Screen.GAME) vm.startDay()
                screen = target
            }
            Screen.GAME -> GameScreen(vm, onExit = back)
            Screen.SHOP -> ShopScreen(vm, onBack = back)
            Screen.ALBUM -> AlbumScreen(vm, onBack = back)
            Screen.RECIPES -> RecipesScreen(vm, onBack = back)
        }
    }
}

@Composable
private fun Title(text: String, size: Int = 34) {
    Text(
        text,
        fontSize = size.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Cursive,
        color = Pink.Deep,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CuteButton(text: String, modifier: Modifier = Modifier, color: Color = Pink.Main, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(4.dp),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Coins(amount: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text("$amount", fontWeight = FontWeight.Bold, color = Pink.Text)
    }
}

@Composable
private fun TopBar(title: String, coins: Int, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Color.White).clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) { Text("◀", color = Pink.Deep, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { Title(title, 28) }
        Coins(coins)
    }
}

/* ---------------------------------- Accueil ---------------------------------- */

@Composable
private fun HomeScreen(vm: GameViewModel, go: (Screen) -> Unit) {
    val player by vm.player.collectAsState()
    val bob = rememberInfiniteTransition(label = "bob")
    val dy by bob.animateFloat(0f, -12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dy")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Title("Meowcha Café", 46)
            Text("☕ le café des chats tout doux 🐾", color = Pink.Text, fontSize = 15.sp)
        }
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            HeartsBackground(Modifier.fillMaxSize())
            CatPortrait(Cats.byId("mochi"), Mood.HAPPY, Modifier.size(170.dp).padding(bottom = 60.dp).offsetY(dy))
            MugView(Mugs.byId(player.equippedMug), listOf(Ingredient.ESPRESSO, Ingredient.STRAWBERRY, Ingredient.MILK, Ingredient.FOAM),
                Modifier.size(110.dp).align(Alignment.BottomCenter))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Coins(player.coins)
            Chip("📅 Jour ${player.day}")
            Chip("⭐ ${player.perfectServed}")
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CuteButton("🐾  Ouvrir le café — Jour ${player.day}", Modifier.fillMaxWidth()) { go(Screen.GAME) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CuteButton("🛍️ Mugs", Modifier.weight(1f), Pink.Deep.copy(alpha = 0.8f)) { go(Screen.SHOP) }
                CuteButton("📸 Album", Modifier.weight(1f), Color(0xFFBA68C8)) { go(Screen.ALBUM) }
            }
            CuteButton("📖 Carnet de recettes", Modifier.fillMaxWidth(), Color(0xFFF48FB1)) { go(Screen.RECIPES) }
        }
    }
}

private fun Modifier.offsetY(dy: Float) = graphicsLayer { translationY = dy * density }

@Composable
private fun Chip(text: String) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 6.dp),
        color = Pink.Text, fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun HeartsBackground(modifier: Modifier) {
    Canvas(modifier) {
        val spots = listOf(0.1f to 0.2f, 0.85f to 0.15f, 0.2f to 0.75f, 0.9f to 0.65f, 0.5f to 0.05f)
        spots.forEachIndexed { i, (x, y) ->
            drawHeart(Offset(size.width * x, size.height * y), size.width * (0.04f + 0.01f * i), Color.White.copy(alpha = 0.8f))
        }
        drawCircle(Color.White.copy(alpha = 0.45f), size.minDimension * 0.42f)
    }
}

/* ----------------------------------- Jeu ------------------------------------ */

@Composable
private fun GameScreen(vm: GameViewModel, onExit: () -> Unit) {
    val state by vm.day.collectAsState()
    val player by vm.player.collectAsState()
    val s = state ?: return
    val mug = Mugs.byId(player.equippedMug)

    if (s.finished) {
        DaySummary(s, onExit)
        return
    }
    val order = s.current ?: return
    var showSteps by remember(order) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onExit() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Pink.Deep) }
            Spacer(Modifier.width(10.dp))
            Chip("📅 Jour ${s.day}")
            Spacer(Modifier.width(8.dp))
            Chip("🐱 ${s.served + 1}/${s.totalCustomers}")
            Spacer(Modifier.weight(1f))
            Coins(player.coins + s.coinsToday)
        }

        // Scène : chat + bulle
        Box(Modifier.fillMaxWidth().weight(1f)) {
            CafeBackdrop(Modifier.fillMaxSize())
            CatPortrait(order.cat, s.mood, Modifier.size(210.dp).align(Alignment.BottomStart).padding(start = 8.dp))
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp).width(190.dp)
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
                    Text("En attente : " + "🐱".repeat(minOf(s.queue.size, 5)), fontSize = 11.sp, color = Pink.Text)
                }
            }
            AnimatedVisibility(
                visible = s.lastResult != null,
                enter = scaleIn() + fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                val r = s.lastResult
                if (r != null) ResultBadge(r.stars, r.coins + r.tip, r.photo)
            }
        }

        // Comptoir : mug
        Box(
            Modifier.fillMaxWidth().height(150.dp).background(Pink.Wood),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().height(10.dp).align(Alignment.TopCenter).background(Color(0xFFD7A08C)))
            MugView(mug, s.cup, Modifier.size(140.dp).padding(top = 12.dp))
            if (s.cup.isNotEmpty()) {
                Text(
                    s.cup.joinToString(" ") { it.emoji },
                    Modifier.align(Alignment.BottomStart).padding(8.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.8f)).padding(4.dp),
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
                        .clickable { vm.add(ing) }
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CuteButton("🗑️ Vider", Modifier.weight(1f), Color(0xFFBDBDBD), enabled = s.cup.isNotEmpty() && !s.reacting) { vm.trash() }
            CuteButton("💝 Servir", Modifier.weight(2f), Pink.Deep, enabled = s.cup.isNotEmpty() && !s.reacting) { vm.serve() }
        }
    }
}

@Composable
private fun CafeBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        // Fenêtre
        drawRoundRect(Color(0xFFB3E5FC), Offset(size.width * 0.05f, size.height * 0.08f),
            androidx.compose.ui.geometry.Size(size.width * 0.35f, size.height * 0.35f),
            androidx.compose.ui.geometry.CornerRadius(24f))
        drawLine(Color.White, Offset(size.width * 0.225f, size.height * 0.08f), Offset(size.width * 0.225f, size.height * 0.43f), 6f)
        drawLine(Color.White, Offset(size.width * 0.05f, size.height * 0.255f), Offset(size.width * 0.4f, size.height * 0.255f), 6f)
        // Guirlande de cœurs
        for (i in 0 until 8) {
            drawHeart(Offset(size.width * (0.06f + i * 0.125f), size.height * 0.03f + (i % 2) * 8f), 14f, if (i % 2 == 0) Pink.Main else Color.White)
        }
        // Étagère
        drawRect(Color(0xFFD7A08C), Offset(size.width * 0.45f, size.height * 0.62f), androidx.compose.ui.geometry.Size(size.width * 0.5f, 10f))
    }
}

@Composable
private fun ResultBadge(stars: Int, coins: Int, photo: Boolean) {
    Column(
        Modifier.shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text((1..3).joinToString("") { if (it <= stars) "⭐" else "☆" }, fontSize = 30.sp)
        Text("+$coins 🪙", fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 20.sp)
        if (photo) Text("📸 Photo souvenir !", color = Pink.Text, fontSize = 13.sp)
    }
}

@Composable
private fun DaySummary(s: DayState, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Title("Fin du jour ${s.day} !", 38)
        Spacer(Modifier.height(16.dp))
        CatPortrait(Cats.all[s.day % Cats.all.size], Mood.DELIGHTED, Modifier.size(180.dp))
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryLine("🐱 Chats servis", "${s.served}/${s.totalCustomers}")
            SummaryLine("⭐ Boissons parfaites", "${s.perfect}")
            SummaryLine("🪙 Gains du jour", "+${s.coinsToday}")
        }
        Spacer(Modifier.height(24.dp))
        CuteButton("🏠 Retour au café", Modifier.fillMaxWidth(), onClick = onExit)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = Pink.Text, fontSize = 17.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Pink.Deep, fontSize = 17.sp)
    }
}

/* --------------------------------- Boutique --------------------------------- */

@Composable
private fun ShopScreen(vm: GameViewModel, onBack: () -> Unit) {
    val player by vm.player.collectAsState()
    val owned by vm.ownedMugs.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Boutique de mugs", player.coins, onBack)
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
                        val fav = Recipes.all.first { it.id == cat.favorite }
                        Text(if (info.hearts >= 3) "Adore : ${fav.name}" else "Préférence : ???", fontSize = 11.sp, color = Pink.Text, textAlign = TextAlign.Center)
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
