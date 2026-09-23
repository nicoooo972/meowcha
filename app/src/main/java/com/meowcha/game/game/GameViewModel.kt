package com.meowcha.game.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meowcha.game.data.CatMetEntity
import com.meowcha.game.data.DayHistoryEntity
import com.meowcha.game.data.MeowchaDb
import com.meowcha.game.data.OwnedDecorEntity
import com.meowcha.game.data.OwnedMugEntity
import com.meowcha.game.data.PlayerEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Mood { HAPPY, WAITING, IMPATIENT, SAD, DELIGHTED }

data class Order(val id: Int, val cat: CatCustomer, val recipe: Recipe, val patienceMax: Float, val vip: Boolean)

data class ServeResult(
    val stars: Int,
    val coins: Int,
    val tip: Int,
    val message: String,
    val photo: Boolean,
    val combo: Int,
)

data class DayState(
    val day: Int,
    val queue: List<Order>,
    val current: Order?,
    val patienceLeft: Float,
    val cup: List<Ingredient> = emptyList(),
    val mood: Mood = Mood.HAPPY,
    val lastResult: ServeResult? = null,
    val coinsToday: Int = 0,
    val served: Int = 0,
    val perfect: Int = 0,
    val left: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val vipPerfect: Int = 0,
    val pets: Int = 0,
    /** Le chat actuel a déjà été caressé (une fois par client). */
    val petted: Boolean = false,
    /** Compteur incrémenté à chaque caresse pour déclencher l'animation. */
    val petPulse: Int = 0,
    val objectives: List<Objective>,
    val finished: Boolean = false,
    val objectiveRewards: Int = 0,
    val totalCustomers: Int,
    /** Gel pendant la réaction du chat après le service. */
    val reacting: Boolean = false,
) {
    fun progress(o: Objective): Int = when (o.type) {
        ObjectiveType.PERFECT -> perfect
        ObjectiveType.COMBO -> bestCombo
        ObjectiveType.VIP -> vipPerfect
        ObjectiveType.NO_LEAVE -> if (finished && left == 0) 1 else 0
        ObjectiveType.COINS -> coinsToday
        ObjectiveType.PETS -> pets
    }

    fun done(o: Objective) = progress(o) >= o.target
}

class GameViewModel(app: Application, val username: String) : AndroidViewModel(app) {
    private val dao = MeowchaDb.get(app, username).dao()

    val player: StateFlow<PlayerEntity> = dao.player()
        .map { it ?: PlayerEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerEntity())
    val ownedMugs: StateFlow<Set<String>> = dao.ownedMugs()
        .map { list -> list.map { it.mugId }.toSet() + "classic" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("classic"))
    val ownedDecor: StateFlow<Set<String>> = dao.ownedDecor()
        .map { list -> list.map { it.decorId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val catsMet: StateFlow<Map<String, CatMetEntity>> = dao.catsMet()
        .map { list -> list.associateBy { it.catId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val history: StateFlow<List<DayHistoryEntity>> = dao.history()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _day = MutableStateFlow<DayState?>(null)
    val day: StateFlow<DayState?> = _day.asStateFlow()

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            if (dao.playerNow() == null) dao.savePlayer(PlayerEntity())
        }
    }

    /** Objectifs prévus pour la prochaine journée (affichés sur l'accueil). */
    fun upcomingObjectives(day: Int) = Objective.forDay(day, customersFor(day))

    private fun customersFor(day: Int) = (4 + day).coerceAtMost(12)

    fun startDay() {
        val p = player.value
        val decor = ownedDecor.value
        val recipes = Recipes.available(p.day)
        val count = customersFor(p.day)
        val objectives = upcomingObjectives(p.day)
        val patienceBonus = 1f + Decors.bonus(decor, DecorBonus.PATIENCE) / 100f
        val basePatience = (40f - p.day * 2f).coerceAtLeast(20f) * patienceBonus
        val vipChance = 0.1f + Decors.bonus(decor, DecorBonus.VIP) / 100f
        val forceVip = if (objectives.any { it.type == ObjectiveType.VIP }) Random.nextInt(1, count) else -1

        val orders = List(count) { i ->
            val cat = Cats.all.random()
            val fav = recipes.firstOrNull { it.id == cat.favorite }
            // Les chats commandent souvent leur boisson préférée si elle est débloquée
            val recipe = if (fav != null && Random.nextFloat() < 0.4f) fav else recipes.random()
            val vip = i > 0 && (i == forceVip || Random.nextFloat() < vipChance)
            val patience = (basePatience + recipe.steps.size * 3f) * if (vip) 0.75f else 1f
            Order(i, cat, recipe, patience, vip)
        }
        _day.value = DayState(
            day = p.day,
            queue = orders.drop(1),
            current = orders.first(),
            patienceLeft = orders.first().patienceMax,
            totalCustomers = count,
            objectives = objectives,
        )
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(100)
                val s = _day.value ?: break
                if (s.finished) break
                val current = s.current
                if (s.reacting || current == null) continue
                val left = s.patienceLeft - 0.1f
                if (left <= 0f) {
                    _day.update {
                        it?.copy(
                            patienceLeft = 0f,
                            mood = Mood.SAD,
                            reacting = true,
                            combo = 0,
                            left = it.left + 1,
                            lastResult = ServeResult(0, 0, 0, "${current.cat.name} est partie fâchée... 😿", false, 0),
                        )
                    }
                    delay(1600)
                    nextCustomer()
                } else {
                    val ratio = left / current.patienceMax
                    val mood = when {
                        ratio > 0.6f -> Mood.HAPPY
                        ratio > 0.3f -> Mood.WAITING
                        else -> Mood.IMPATIENT
                    }
                    _day.update { it?.copy(patienceLeft = left, mood = mood) }
                }
            }
        }
    }

    fun add(ingredient: Ingredient) {
        _day.update { s ->
            if (s == null || s.reacting || s.cup.size >= 6) s else s.copy(cup = s.cup + ingredient)
        }
    }

    fun undo() {
        _day.update { if (it == null || it.reacting) it else it.copy(cup = it.cup.dropLast(1)) }
    }

    fun trash() {
        _day.update { if (it == null || it.reacting) it else it.copy(cup = emptyList()) }
    }

    /** Caresser le chat : il ronronne et gagne un peu de patience (une fois par client). */
    fun pet() {
        _day.update { s ->
            val order = s?.current
            if (s == null || order == null || s.reacting) return@update s
            if (s.petted) return@update s.copy(petPulse = s.petPulse + 1)
            s.copy(
                petted = true,
                pets = s.pets + 1,
                petPulse = s.petPulse + 1,
                patienceLeft = (s.patienceLeft + order.patienceMax * 0.2f).coerceAtMost(order.patienceMax),
            )
        }
    }

    fun serve() {
        val s = _day.value ?: return
        val order = s.current ?: return
        if (s.reacting || s.cup.isEmpty()) return

        val stars = grade(order.recipe.steps, s.cup)
        val mug = Mugs.byId(player.value.equippedMug)
        val price = order.recipe.price * if (order.vip) 2 else 1
        val base = when (stars) {
            3 -> price
            2 -> (price * 0.7f).roundToInt()
            1 -> (price * 0.3f).roundToInt()
            else -> 0
        }
        val combo = if (stars == 3) s.combo + 1 else 0
        val ratio = s.patienceLeft / order.patienceMax
        val rawTip = if (stars >= 2) (ratio * 4f).roundToInt() + 1 else 0
        val tipBonus = mug.tipBonus + Decors.bonus(ownedDecor.value, DecorBonus.TIPS)
        val comboMult = 1f + 0.25f * (combo - 1).coerceIn(0, 4)
        val tip = (rawTip * (1f + tipBonus / 100f) * comboMult).roundToInt()
        val photo = stars == 3 && ratio > 0.5f
        val msg = when (stars) {
            3 -> if (order.vip) "Digne d'une reine ! 👑" else listOf("Purrrfait ! 💖", "Miaou-gnifique ! ✨", "C'est exactement ça ! 😻").random()
            2 -> "Presque parfait, merci ! 😺"
            1 -> "Hmm... c'est bizarre. 🙀"
            else -> "Ce n'est pas du tout ça ! 😾"
        }
        _day.update {
            it?.copy(
                reacting = true,
                mood = if (stars >= 2) Mood.DELIGHTED else Mood.SAD,
                lastResult = ServeResult(stars, base, tip, msg, photo, combo),
                coinsToday = it.coinsToday + base + tip,
                served = it.served + 1,
                perfect = it.perfect + if (stars == 3) 1 else 0,
                vipPerfect = it.vipPerfect + if (stars == 3 && order.vip) 1 else 0,
                combo = combo,
                bestCombo = maxOf(it.bestCombo, combo),
            )
        }
        viewModelScope.launch {
            val cat = dao.catNow(order.cat.id) ?: CatMetEntity(order.cat.id)
            val heartGain = when {
                stars == 3 && order.recipe.id == order.cat.favorite -> 2
                stars >= 2 -> 1
                else -> 0
            }
            dao.saveCat(cat.copy(timesServed = cat.timesServed + 1, hearts = (cat.hearts + heartGain).coerceAtMost(10)))
            delay(1800)
            nextCustomer()
        }
    }

    private fun nextCustomer() {
        val s = _day.value ?: return
        val next = s.queue.firstOrNull()
        if (next == null) {
            val ended = s.copy(current = null, finished = true, reacting = false, cup = emptyList())
            val rewards = ended.objectives.filter { ended.done(it) }.sumOf { it.reward }
            _day.value = ended.copy(objectiveRewards = rewards)
            finishDay(ended, rewards)
        } else {
            _day.value = s.copy(
                current = next,
                queue = s.queue.drop(1),
                patienceLeft = next.patienceMax,
                cup = emptyList(),
                mood = Mood.HAPPY,
                reacting = false,
                petted = false,
                lastResult = null,
            )
        }
    }

    private fun finishDay(s: DayState, rewards: Int) {
        viewModelScope.launch {
            val p = dao.playerNow() ?: PlayerEntity()
            dao.savePlayer(
                p.copy(
                    coins = p.coins + s.coinsToday + rewards,
                    day = p.day + 1,
                    totalServed = p.totalServed + s.served,
                    perfectServed = p.perfectServed + s.perfect,
                    bestDayCoins = maxOf(p.bestDayCoins, s.coinsToday),
                    bestCombo = maxOf(p.bestCombo, s.bestCombo),
                )
            )
            dao.addHistory(DayHistoryEntity(day = s.day, coins = s.coinsToday + rewards, served = s.served, perfect = s.perfect))
        }
    }

    fun quitDay() {
        ticker?.cancel()
        _day.value = null
    }

    fun buyMug(mug: Mug) {
        viewModelScope.launch {
            val p = dao.playerNow() ?: return@launch
            if (mug.id in ownedMugs.value || p.coins < mug.price) return@launch
            dao.addMug(OwnedMugEntity(mug.id))
            dao.savePlayer(p.copy(coins = p.coins - mug.price, equippedMug = mug.id))
        }
    }

    fun equipMug(mug: Mug) {
        viewModelScope.launch {
            val p = dao.playerNow() ?: return@launch
            if (mug.id in ownedMugs.value) dao.savePlayer(p.copy(equippedMug = mug.id))
        }
    }

    fun buyDecor(decor: Decor) {
        viewModelScope.launch {
            val p = dao.playerNow() ?: return@launch
            if (decor.id in ownedDecor.value || p.coins < decor.price) return@launch
            dao.addDecor(OwnedDecorEntity(decor.id))
            dao.savePlayer(p.copy(coins = p.coins - decor.price))
        }
    }

    companion object {
        /** 3 = recette exacte, 2 = bons ingrédients dans le désordre ou presque, 1 = partiel, 0 = raté. */
        fun grade(expected: List<Ingredient>, made: List<Ingredient>): Int {
            if (expected == made) return 3
            val exp = expected.groupingBy { it }.eachCount()
            val got = made.groupingBy { it }.eachCount()
            val matches = exp.entries.sumOf { (k, v) -> minOf(v, got[k] ?: 0) }
            val accuracy = matches.toFloat() / maxOf(expected.size, made.size)
            return when {
                accuracy >= 0.7f -> 2
                accuracy >= 0.4f -> 1
                else -> 0
            }
        }
    }
}
