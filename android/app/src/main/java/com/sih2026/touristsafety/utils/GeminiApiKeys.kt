package com.sih2026.touristsafety.utils

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import java.util.concurrent.atomic.AtomicInteger

/**
 * Robust Gemini API Key Manager with automatic shuffling, round-robin distribution,
 * and failover retry handling.
 *
 * Keys are loaded from BuildConfig.GEMINI_API_KEYS and BuildConfig.GEMINI_API_KEY
 * (configured via android/local.properties). Multiple keys can be separated by commas,
 * semicolons, or newlines.
 */
object GeminiApiKeys {
    private const val TAG = "GeminiApiKeys"

    // Master list of available keys
    private val keyList: MutableList<String> = mutableListOf()
    private val index = AtomicInteger(0)
    private val lock = Any()

    init {
        loadKeys()
    }

    private fun loadKeys() {
        val rawKeys = listOf(
            com.sih2026.touristsafety.BuildConfig.GEMINI_API_KEYS,
            com.sih2026.touristsafety.BuildConfig.GEMINI_API_KEY
        )

        val parsed = rawKeys
            .flatMap { it.split(",", ";", "\n") }
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
            .filter { it.isNotBlank() }
            .distinct()

        synchronized(lock) {
            keyList.clear()
            keyList.addAll(parsed)
            if (keyList.isNotEmpty()) {
                keyList.shuffle() // Randomize order across app launches
            }
        }
        Log.d(TAG, "Loaded ${keyList.size} Gemini API key(s) with shuffling enabled.")
    }

    /**
     * Manually shuffle the keys order to balance traffic.
     */
    fun shuffle() {
        synchronized(lock) {
            if (keyList.isNotEmpty()) {
                keyList.shuffle()
                index.set(0)
            }
        }
    }

    /**
     * Get the next key using round-robin distribution.
     */
    fun getNextKey(): String {
        synchronized(lock) {
            if (keyList.isEmpty()) {
                throw IllegalStateException(
                    "No Gemini API keys found. Set GEMINI_API_KEYS in android/local.properties"
                )
            }
            val currentIndex = Math.floorMod(index.getAndIncrement(), keyList.size)
            return keyList[currentIndex]
        }
    }

    /**
     * Mark a key as failed (e.g. 401, 403, 429 quota exhaustion)
     * and move it to the back of the queue so healthy keys are tried first.
     */
    fun markKeyFailed(failedKey: String, reason: String? = null) {
        synchronized(lock) {
            val idx = keyList.indexOf(failedKey)
            if (idx != -1 && keyList.size > 1) {
                val key = keyList.removeAt(idx)
                keyList.add(key) // Send to back of line
                Log.w(TAG, "Demoted key ${failedKey.take(12)}... to end of pool. Reason: $reason")
            }
        }
    }

    /**
     * Returns total count of available keys.
     */
    fun getKeyCount(): Int {
        synchronized(lock) {
            return keyList.size
        }
    }

    /**
     * Returns a snapshot of masked keys for diagnostics.
     */
    fun getMaskedKeys(): List<String> {
        synchronized(lock) {
            return keyList.map {
                if (it.length > 10) "${it.take(6)}...${it.takeLast(4)}" else "***"
            }
        }
    }

    /**
     * Creates a new GenerativeModel with the next available key.
     */
    fun createModel(modelName: String = "gemini-3.6-flash"): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = getNextKey()
        )
    }

    /**
     * Executes an operation with automatic key rotation and failover.
     * If the operation throws an exception, it marks the key as failed,
     * advances to the next key, and retries until all keys have been attempted.
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = getKeyCount().coerceAtLeast(1),
        block: suspend (apiKey: String) -> T
    ): T {
        val attempts = maxAttempts.coerceAtLeast(1)
        var lastException: Throwable? = null

        for (i in 0 until attempts) {
            val key = getNextKey()
            try {
                val result = block(key)
                return result
            } catch (e: Throwable) {
                lastException = e
                val errorMsg = e.message ?: "Unknown error"
                Log.w(TAG, "Attempt ${i + 1}/$attempts failed using key ${key.take(10)}...: $errorMsg")
                markKeyFailed(key, errorMsg)

                // If this is the last attempt, don't continue loop
                if (i == attempts - 1) {
                    break
                }
            }
        }

        throw lastException ?: IllegalStateException("All Gemini API keys failed during execution.")
    }
}
