package com.meowcha.game.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File

/** Musique et bruitages, fournis par les packs de contenu téléchargés. */
object Audio {
    private var music: Map<String, File> = emptyMap()
    private var sounds: Map<String, Int> = emptyMap()
    private var pool: SoundPool? = null
    private var player: MediaPlayer? = null
    private var currentTrack: String? = null
    private var paused = false

    var musicOn = true
        private set
    var sfxOn = true
        private set
    val hasContent get() = music.isNotEmpty() || sounds.isNotEmpty()

    fun init(context: Context, musicFiles: Map<String, File>, sfxFiles: Map<String, File>) {
        val prefs = context.getSharedPreferences("meowcha_settings", Context.MODE_PRIVATE)
        musicOn = prefs.getBoolean("music", true)
        sfxOn = prefs.getBoolean("sfx", true)
        music = musicFiles
        pool?.release()
        val p = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        sounds = sfxFiles.mapValues { (_, f) -> p.load(f.path, 1) }
        pool = p
    }

    fun setMusic(context: Context, on: Boolean) {
        musicOn = on
        context.getSharedPreferences("meowcha_settings", Context.MODE_PRIVATE).edit().putBoolean("music", on).apply()
        if (on) currentTrack?.let { t -> currentTrack = null; playMusic(t) } else stopPlayer()
    }

    fun setSfx(context: Context, on: Boolean) {
        sfxOn = on
        context.getSharedPreferences("meowcha_settings", Context.MODE_PRIVATE).edit().putBoolean("sfx", on).apply()
    }

    /** Joue la musique [track] ("home", "cafe"...) en boucle, sans relancer si c'est déjà elle. */
    fun playMusic(track: String) {
        if (currentTrack == track && player != null) return
        currentTrack = track
        stopPlayer()
        if (!musicOn || paused) return
        val file = music[track] ?: return
        player = runCatching {
            MediaPlayer().apply {
                setDataSource(file.path)
                isLooping = true
                setVolume(0.45f, 0.45f)
                prepare()
                start()
            }
        }.getOrNull()
    }

    fun play(sound: String) {
        if (!sfxOn) return
        val id = sounds[sound] ?: return
        pool?.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }

    /** Bruitage propre à chaque ingrédient (versement, mousse, glaçons...). */
    fun playIngredient(ing: Ingredient) {
        val key = when (ing) {
            Ingredient.ESPRESSO -> "espresso"
            Ingredient.WATER, Ingredient.TEA -> "pour_water"
            Ingredient.MILK -> "pour_milk"
            Ingredient.FOAM -> "steam"
            Ingredient.STRAWBERRY, Ingredient.VANILLA, Ingredient.CHOCO, Ingredient.CARAMEL -> "syrup"
            Ingredient.MATCHA -> "whisk"
            Ingredient.ICE -> "ice"
            Ingredient.CREAM -> "cream"
            Ingredient.MARSHMALLOW, Ingredient.SAKURA -> "sprinkle"
        }
        if (key in sounds) play(key) else play("add")
    }

    fun onPause() {
        paused = true
        runCatching { player?.pause() }
    }

    fun onResume() {
        paused = false
        val p = player
        if (p != null) runCatching { if (musicOn) p.start() }
        else currentTrack?.let { t -> currentTrack = null; playMusic(t) }
    }

    private fun stopPlayer() {
        runCatching { player?.stop(); player?.release() }
        player = null
    }
}
