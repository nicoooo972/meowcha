package com.meowcha.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.meowcha.game.ui.MeowchaApp
import com.meowcha.game.ui.MeowchaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeowchaTheme { MeowchaApp() } }
    }
}
