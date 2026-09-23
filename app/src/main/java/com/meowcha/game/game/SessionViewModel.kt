package com.meowcha.game.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meowcha.game.data.AccountEntity
import com.meowcha.game.data.AccountRepository
import com.meowcha.game.data.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SessionState {
    /** Écran de chargement ; [progress] entre 0 et 1. */
    data class Loading(val progress: Float, val tip: String) : SessionState
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

    init {
        viewModelScope.launch {
            val tip = TIPS.random()
            // Petite animation de chargement pendant l'ouverture de la base locale
            var session: AccountEntity? = null
            var profiles: List<AccountEntity> = emptyList()
            val load = launch(Dispatchers.IO) {
                session = repo.currentSession()
                profiles = repo.accounts()
            }
            for (i in 1..20) {
                delay(90)
                _state.value = SessionState.Loading(i / 20f, tip)
            }
            load.join()
            _state.value = session?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut(profiles)
        }
    }

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
            "Astuce : l'ordre des ingrédients compte pour une boisson parfaite ⭐",
        )
    }
}
