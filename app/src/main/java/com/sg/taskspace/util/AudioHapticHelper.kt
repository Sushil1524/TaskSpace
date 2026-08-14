package com.sg.taskspace.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object AudioHapticHelper {

    private var toneGenerator: ToneGenerator? = null

    init {
        runCatching {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        }
    }

    fun playTaskCompleteSoundAndHaptic(context: Context, hapticFeedback: HapticFeedback? = null, soundEnabled: Boolean = true) {
        // Compose Haptic Feedback
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)

        // Device Vibrator
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(60)
                    }
                }
            }
        }

        // Sound effect (Plays via STREAM_MUSIC for maximum reliability)
        if (soundEnabled) {
            runCatching {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        }
    }
}
