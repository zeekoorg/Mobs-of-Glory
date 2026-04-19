package com.zeekoorg.mobsofglory

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object SoundManager {
    private var soundPool: SoundPool? = null
    private var bgmPlayer: MediaPlayer? = null
    
    private var sfxClick = 0
    private var sfxWindowOpen = 0
    private var sfxWheat = 0
    private var sfxIron = 0
    private var sfxGold = 0
    private var sfxMarch = 0
    private var sfxTrain = 0
    private var sfxClash = 0
    private var sfxBlacksmith = 0

    var isMusicEnabled = true
    var isSfxEnabled = true

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("MobsOfGlorySettings", Context.MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean("MUSIC", true)
        isSfxEnabled = prefs.getBoolean("SFX", true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10) // تشغيل 10 أصوات في نفس اللحظة بدون تقطيع
            .setAudioAttributes(audioAttributes)
            .build()

        // تحميل المؤثرات الصوتية للذاكرة
        sfxClick = soundPool?.load(context, R.raw.sfx_click, 1) ?: 0
        sfxWindowOpen = soundPool?.load(context, R.raw.sfx_window, 1) ?: 0
        sfxWheat = soundPool?.load(context, R.raw.sfx_wheat, 1) ?: 0
        sfxIron = soundPool?.load(context, R.raw.sfx_iron, 1) ?: 0
        sfxGold = soundPool?.load(context, R.raw.sfx_gold, 1) ?: 0
        sfxMarch = soundPool?.load(context, R.raw.sfx_march, 1) ?: 0
        sfxTrain = soundPool?.load(context, R.raw.sfx_train, 1) ?: 0
        sfxClash = soundPool?.load(context, R.raw.sfx_clash, 1) ?: 0
        sfxBlacksmith = soundPool?.load(context, R.raw.sfx_blacksmith, 1) ?: 0
    }

    private fun playSfx(soundId: Int) {
        if (isSfxEnabled && soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    fun playClick() = playSfx(sfxClick)
    fun playWindowOpen() = playSfx(sfxWindowOpen)
    fun playWheat() = playSfx(sfxWheat)
    fun playIron() = playSfx(sfxIron)
    fun playGold() = playSfx(sfxGold)
    fun playMarch() = playSfx(sfxMarch)
    fun playTrain() = playSfx(sfxTrain)
    fun playClash() = playSfx(sfxClash)
    fun playBlacksmith() = playSfx(sfxBlacksmith)

    // إدارة الموسيقى الخلفية
    fun playBGM(context: Context, resId: Int) {
        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer = MediaPlayer.create(context, resId)
        bgmPlayer?.isLooping = true
        if (isMusicEnabled) {
            bgmPlayer?.start()
        }
    }

    fun pauseBGM() {
        if (bgmPlayer?.isPlaying == true) {
            bgmPlayer?.pause()
        }
    }

    fun resumeBGM() {
        if (isMusicEnabled && bgmPlayer?.isPlaying == false) {
            bgmPlayer?.start()
        }
    }

    fun updateSettings(music: Boolean, sfx: Boolean) {
        isMusicEnabled = music
        isSfxEnabled = sfx
        if (!isMusicEnabled) pauseBGM() else resumeBGM()
    }
    
    fun onDestroy() {
        soundPool?.release()
        soundPool = null
        bgmPlayer?.release()
        bgmPlayer = null
    }
}
