package com.meowcha.game.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meowcha.game.data.AccountEntity
import com.meowcha.game.data.AccountRepository
import com.meowcha.game.data.AuthResult
import com.meowcha.game.data.ContentPacks
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

class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AccountRepository(app)

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading(0f, TIPS.random()))
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                    is SyncResult.Updated -> show(0.95f, "Nouveautés installées ✨", result.names.joinToString())
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
            show(1f, "C'est prêt !")
            delay(250)
            _state.value = session?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut(profiles)
        }
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
