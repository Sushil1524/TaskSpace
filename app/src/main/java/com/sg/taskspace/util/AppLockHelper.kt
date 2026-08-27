package com.sg.taskspace.util

import java.security.MessageDigest

object AppLockHelper {

    private const val PIN_SALT = "TaskSpace_AppLock_Salt_2026"

    fun hashPin(pin: String): String {
        if (pin.isBlank()) return ""
        val bytes = (pin + PIN_SALT).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (inputPin.isBlank() || storedHash.isBlank()) return false
        return hashPin(inputPin) == storedHash
    }

    fun isTimeoutExceeded(lastBackgroundMs: Long, timeoutOption: String): Boolean {
        if (lastBackgroundMs <= 0L) return true
        val elapsedMs = System.currentTimeMillis() - lastBackgroundMs
        return when (timeoutOption) {
            "Immediately" -> true
            "1 minute" -> elapsedMs >= 60_000L
            "5 minutes" -> elapsedMs >= 300_000L
            "15 minutes" -> elapsedMs >= 900_000L
            "30 minutes" -> elapsedMs >= 1_800_000L
            else -> true
        }
    }
}
