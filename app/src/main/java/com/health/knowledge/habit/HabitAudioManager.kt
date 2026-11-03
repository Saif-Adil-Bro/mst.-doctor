package com.health.knowledge.habit

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.health.knowledge.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.sin

/**
 * Audio & Text-to-Speech (TTS) Subsystem for Smart Habit & Routine Engine.
 *
 * Fallback sequence for TTS_VOICE:
 *   Primary device TTS (bn-BD locale) -> Secondary (en-US locale) -> Fallback preset chime.
 *
 * Bundled preset sound keys:
 *   - "water_drop"       -> R.raw.water_drop
 *   - "soft_bell"        -> R.raw.soft_bell
 *   - "medication_chime" -> R.raw.medication_chime
 */
@Singleton
class HabitAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var activeMediaPlayer: MediaPlayer? = null

    companion object {
        const val SOUND_WATER_DROP = "water_drop"
        const val SOUND_SOFT_BELL = "soft_bell"
        const val SOUND_MEDICATION_CHIME = "medication_chime"
    }

    /**
     * Executes the configured alert payload (TTS Voice + Preset Chime, Preset Tone only, or System Default).
     */
    fun playReminderAlert(
        audioMode: String,
        presetSoundKey: String?,
        spokenText: String
    ) {
        val safePresetKey = presetSoundKey?.takeIf { it.isNotBlank() } ?: SOUND_SOFT_BELL

        when (audioMode) {
            HabitScheduleCalculator.AUDIO_TTS_VOICE -> {
                // Play short intro chime first, then speak Bangla/English TTS prompt
                playPresetSound(safePresetKey) {
                    speakWithFallback(
                        text = spokenText,
                        fallbackSoundKey = safePresetKey
                    )
                }
            }

            HabitScheduleCalculator.AUDIO_PRESET_TONE -> {
                playPresetSound(safePresetKey, onComplete = null)
            }

            HabitScheduleCalculator.AUDIO_SYSTEM_DEFAULT -> {
                playSystemDefaultNotificationSound(fallbackSoundKey = safePresetKey)
            }

            else -> {
                playPresetSound(safePresetKey, onComplete = null)
            }
        }
    }

    /**
     * Speaks text using primary locale `bn-BD` (Bengali), falling back to `en-US`,
     * and if TTS is unavailable on the device, plays the bundled preset tone.
     */
    fun speakWithFallback(
        text: String,
        fallbackSoundKey: String = SOUND_SOFT_BELL
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            playPresetSound(fallbackSoundKey, onComplete = null)
            return
        }

        scope.launch {
            try {
                if (tts != null && isTtsReady) {
                    configureLocaleAndSpeak(tts!!, cleanText, fallbackSoundKey)
                } else {
                    tts?.shutdown()
                    var engineRef: TextToSpeech? = null
                    engineRef = TextToSpeech(context) { status ->
                        if (status == TextToSpeech.SUCCESS && engineRef != null) {
                            isTtsReady = true
                            configureLocaleAndSpeak(engineRef!!, cleanText, fallbackSoundKey)
                        } else {
                            isTtsReady = false
                            playPresetSound(fallbackSoundKey, onComplete = null)
                        }
                    }
                    tts = engineRef
                }
            } catch (_: Exception) {
                playPresetSound(fallbackSoundKey, onComplete = null)
            }
        }
    }

    private fun configureLocaleAndSpeak(
        engine: TextToSpeech,
        text: String,
        fallbackSoundKey: String
    ) {
        try {
            val bnBdLocale = Locale("bn", "BD")
            val bnInLocale = Locale("bn", "IN")
            val enUsLocale = Locale.US

            val bnResult = engine.setLanguage(bnBdLocale)
            val languageSetOk = if (bnResult == TextToSpeech.LANG_MISSING_DATA || bnResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val bnInResult = engine.setLanguage(bnInLocale)
                if (bnInResult == TextToSpeech.LANG_MISSING_DATA || bnInResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val enResult = engine.setLanguage(enUsLocale)
                    enResult != TextToSpeech.LANG_MISSING_DATA && enResult != TextToSpeech.LANG_NOT_SUPPORTED
                } else {
                    true
                }
            } else {
                true
            }

            if (!languageSetOk) {
                playPresetSound(fallbackSoundKey, onComplete = null)
                return
            }

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            engine.setAudioAttributes(attrs)
            engine.setSpeechRate(0.95f)
            engine.setPitch(1.0f)

            val utteranceId = "habit_tts_${UUID.randomUUID()}"
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    playPresetSound(fallbackSoundKey, onComplete = null)
                }
            })

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            }
            val speakResult = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (speakResult == TextToSpeech.ERROR) {
                playPresetSound(fallbackSoundKey, onComplete = null)
            }
        } catch (_: Exception) {
            playPresetSound(fallbackSoundKey, onComplete = null)
        }
    }

    /**
     * Plays bundled raw audio file (`water_drop.ogg`, `soft_bell.ogg`, `medication_chime.ogg`).
     */
    fun playPresetSound(soundKey: String?, onComplete: (() -> Unit)? = null) {
        val resId = when (soundKey) {
            SOUND_WATER_DROP -> R.raw.water_drop
            SOUND_MEDICATION_CHIME -> R.raw.medication_chime
            else -> R.raw.soft_bell
        }

        scope.launch(Dispatchers.Main) {
            try {
                activeMediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) player.stop()
                    } catch (_: Exception) {}
                    player.release()
                }
                activeMediaPlayer = null

                val afd = context.resources.openRawResourceFd(resId)
                if (afd == null) {
                    playSynthesizedFallbackTone(soundKey, onComplete)
                    return@launch
                }

                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                mp.setOnCompletionListener { player ->
                    player.release()
                    if (activeMediaPlayer === player) {
                        activeMediaPlayer = null
                    }
                    onComplete?.invoke()
                }
                mp.setOnErrorListener { player, _, _ ->
                    player.release()
                    if (activeMediaPlayer === player) {
                        activeMediaPlayer = null
                    }
                    playSynthesizedFallbackTone(soundKey, onComplete)
                    true
                }
                mp.prepare()
                mp.start()
                activeMediaPlayer = mp
            } catch (_: Exception) {
                playSynthesizedFallbackTone(soundKey, onComplete)
            }
        }
    }

    private fun playSystemDefaultNotificationSound(fallbackSoundKey: String) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.play()
            } else {
                playPresetSound(fallbackSoundKey, onComplete = null)
            }
        } catch (_: Exception) {
            playPresetSound(fallbackSoundKey, onComplete = null)
        }
    }

    /**
     * Hardware-safe PCM synthesizer fallback in case MediaPlayer fails on a headless or restricted device.
     */
    private fun playSynthesizedFallbackTone(soundKey: String?, onComplete: (() -> Unit)?) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * 0.45).toInt()
                val pcm = ShortArray(numSamples)
                val baseFreq = when (soundKey) {
                    SOUND_WATER_DROP -> 740.0
                    SOUND_MEDICATION_CHIME -> 880.0
                    else -> 528.0
                }
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 5.0)
                    val sample = env * sin(2.0 * Math.PI * baseFreq * t)
                    pcm[i] = (sample * 24000).toInt().coerceIn(-32767, 32767).toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                kotlinx.coroutines.delay(500L)
                track.release()
            } catch (_: Exception) {
                // Ignore audio error
            } finally {
                scope.launch { onComplete?.invoke() }
            }
        }
    }
}
