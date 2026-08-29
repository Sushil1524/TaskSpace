package com.sg.taskspace.util

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object AudioHapticHelper {

    private var cachedRingtone: Ringtone? = null

    fun ensureInitialized(context: Context) {
        if (cachedRingtone != null) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            cachedRingtone = RingtoneManager.getRingtone(context.applicationContext, uri)
        } catch (_: Exception) { }
    }

    fun playTaskCompleteSoundAndHaptic(
        context: Context,
        hapticFeedback: HapticFeedback? = null,
        soundEnabled: Boolean = true,
        hapticEnabled: Boolean = true
    ) {
        if (!soundEnabled && !hapticEnabled) return

        // 1. Compose haptic feedback (visual + tactile layer)
        if (hapticEnabled) {
            runCatching {
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        // 2. Hardware vibrator — works even in silent / vibrate-only mode
        if (hapticEnabled) {
            runCatching {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                        ?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Pleasant two-pulse "done" pattern
                            v.vibrate(
                                VibrationEffect.createWaveform(
                                    longArrayOf(0L, 40L, 60L, 80L),
                                    intArrayOf(0, 160, 0, 100),
                                    -1
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(70L)
                        }
                    }
                }
            }
        }

        // 3. System notification chime — only in NORMAL or VIBRATE mode (not SILENT)
        if (soundEnabled) {
            runCatching {
                val audioManager =
                    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
                if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    ensureInitialized(context)
                    val ringtone = cachedRingtone ?: run {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context.applicationContext, uri)
                            ?.also { cachedRingtone = it }
                    }
                    ringtone?.play()
                }
            }
        }
    }
}
