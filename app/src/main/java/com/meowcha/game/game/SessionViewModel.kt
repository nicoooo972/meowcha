package com.meowcha.game.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meowcha.game.data.AccountEntity
import com.meowcha.game.data.AccountRepository
import com.meowcha.game.data.AuthResult
import com.meowcha.game.BuildConfig
import com.meowcha.game.data.ContentPacks
import com.meowcha.game.data.PackInfo
import com.meowcha.game.data.SyncEvent
import com.meowcha.game.data.SyncResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SessionState {
    /**
     * Écran de chargement ; [progress] entre 0 et 1.
     * [detail] décrit l'étape (ex. "12,4 Mo / 18,7 Mo • 2,1 Mo/s"), [canSkip] affiche le bouton "Plus tard".
     */
    data class Loading(
        val progress: Float,
        val tip: String,
        val title: String = "Ouverture du café...",
        val detail: String? = null,
        val canSkip: Boolean = false,
    ) : SessionState
    data class LoggedOut(val profiles: List<AccountEntity>) : SessionState
    data class LoggedIn(val account: AccountEntity) : SessionState
}

/** Contenu de la fenêtre « Quoi de neuf ? ». */
data class News(
    val title: String,
    val lines: List<Pair<String, String>>,
    val cats: List<CatCustomer>,
)

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AccountRepository(app)

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading(0f, TIPS.random()))
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _news = MutableStateFlow<News?>(null)
    val news: StateFlow<News?> = _news.asStateFlow()

    fun dismissNews() { _news.value = null }

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val content = ContentPacks(app)
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            val tip = TIPS.random()
            fun show(progress: Float, title: String, detail: String? = null, canSkip: Boolean = false) {
                _state.value = SessionState.Loading(progress, tip, title, detail, canSkip)
            }
            show(0.05f, "Recherche de nouveautés...")

            var updatedIds = emptyList<String>()
            // 1. Téléchargement des packs de contenu (annulable avec "Plus tard")
            val job = launch {
                val result = content.sync { e ->
                    when (e) {
                        SyncEvent.Checking -> show(0.08f, "Recherche de nouveautés...")
                        is SyncEvent.Downloading -> {
                            val pct = e.bytes.toFloat() / maxOf(1L, e.total)
                            val pack = if (e.count > 1) " (${e.index + 1}/${e.count})" else ""
                            show(
                                0.1f + 0.8f * (e.index + pct) / e.count,
                                "Téléchargement : ${e.pack.name}$pack",
                                "${mb(e.bytes)} / ${mb(e.total)} • ${mb(e.bytesPerSecond)}/s",
                                canSkip = true,
                            )
                        }
                        is SyncEvent.Installing -> show(0.92f, "Installation : ${e.pack.name}...")
                    }
                }
                when (result) {
                    is SyncResult.Updated -> {
                        updatedIds = result.ids
                        show(0.95f, "Nouveautés installées ✨", result.names.joinToString())
                    }
                    is SyncResult.Offline -> show(0.95f, "Ouverture du café...", result.reason)
                    is SyncResult.UpToDate -> show(0.95f, "Tout est à jour 💖")
                }
                delay(if (result is SyncResult.Updated) 900 else 300)
            }
            syncJob = job
            job.join()

            // 2. Chargement du contenu installé + session
            show(0.98f, "Préparation du café...")
            val installed = withContext(Dispatchers.IO) { content.loadInstalled() }
            Audio.init(app, installed.music, installed.sfx)
            val session = withContext(Dispatchers.IO) { repo.currentSession() }
            val profiles = withContext(Dispatchers.IO) { repo.accounts() }
            _news.value = buildNews(app, installed.packs.filter { it.id in updatedIds })
            show(1f, "C'est prêt !")
            delay(250)
            _state.value = session?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut(profiles)
        }
    }

    /** Nouveautés = packs tout juste téléchargés + notes de version si l'app vient d'être mise à jour. */
    private fun buildNews(app: Application, packs: List<PackInfo>): News? {
        val prefs = app.getSharedPreferences("meowcha_settings", android.content.Context.MODE_PRIVATE)
        val seen = prefs.getString("seen_version", null)
        val current = BuildConfig.VERSION_NAME
        prefs.edit().putString("seen_version", current).apply()
        val appNotes = if (seen != current) RELEASE_NOTES[current].orEmpty() else emptyList()

        val lines = mutableListOf<Pair<String, String>>()
        lines += appNotes
        packs.forEach { p ->
            lines += "📦" to "${p.name} : ${p.description}"
            if (p.cats.isNotEmpty()) lines += "🐱" to "Nouvelles clientes : ${p.cats.joinToString { it.name }}"
            if (p.recipes.isNotEmpty()) lines += "☕" to "Nouvelles recettes : ${p.recipes.joinToString { "${it.name} (jour ${it.unlockDay})" }}"
            if (p.hasMusic) lines += "🎵" to "Musique d'ambiance et bruitages"
        }
        if (lines.isEmpty()) return null
        return News(
            title = if (appNotes.isNotEmpty()) "Quoi de neuf dans la v$current ?" else "Nouveautés téléchargées !",
            lines = lines,
            cats = packs.flatMap { it.cats },
        )
    }

    /** Bouton "Plus tard" pendant un téléchargement : on continue sans les nouveautés. */
    fun skipDownload() {
        syncJob?.cancel()
    }

    private fun mb(bytes: Long) = String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / 1_000_000f)

    fun clearError() { _error.value = null }

    fun register(name: String, password: String, confirm: String, avatar: String) =
        auth { repo.register(name, password, confirm, avatar) }

    fun login(name: String, password: String) = auth { repo.login(name, password) }

    private fun auth(block: suspend () -> AuthResult) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            when (val r = withContext(Dispatchers.Default) { block() }) {
                is AuthResult.Success -> _state.value = SessionState.LoggedIn(r.account)
                is AuthResult.Error -> _error.value = r.message
            }
            _busy.value = false
        }
    }

    fun logout() {
        repo.logout()
        viewModelScope.launch {
            _state.value = SessionState.LoggedOut(withContext(Dispatchers.IO) { repo.accounts() })
        }
    }

    companion object {
        /** Points forts affichés au premier lancement de chaque version. */
        val RELEASE_NOTES = mapOf(
            "1.1.0" to listOf(
                "👤" to "Comptes : plusieurs profils sur le même téléphone",
                "🔥" to "Combos : enchaîne les boissons parfaites pour plus de pourboires",
                "🐾" to "Caresse les chats pour leur redonner de la patience",
                "👑" to "Chats VIP : ils paient double mais sont pressés",
                "🎯" to "3 objectifs par jour avec des récompenses",
                "🪴" to "Décorations pour ton café, avec des bonus",
                "📦" to "Contenu téléchargeable : de nouveaux chats sans mettre à jour l'app",
                "✨" to "Café en relief : incline ton téléphone pour voir la salle bouger",
                "☕" to "Nouvelles tasses en céramique, mousse onctueuse et latte art",
                "🔊" to "Bruitages : miaous, versements, mousse, glaçons...",
            ),
        )

        val TIPS = listOf(
            "Astuce : caresse un chat pour lui redonner de la patience 🐾",
            "Astuce : enchaîne les boissons parfaites pour faire grimper ton combo 🔥",
            "Astuce : les chats VIP 👑 paient double mais sont pressés !",
            "Astuce : les décorations rendent les chats plus patients 🪴",
            "Astuce : gagne les cœurs d'un chat pour découvrir sa boisson préférée 💗",
            "Astuce : de nouveaux chats arrivent avec les packs de contenu 🌸",
            "Astuce : l'ordre des ingrédients compte pour une boisson parfaite ⭐",
        )
    }
}
