package com.example.unscramble.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game_prefs")

class GamePreferences(private val context: Context) {

    companion object {
        val KEY_SCORE = intPreferencesKey("score")
        val KEY_WORD_COUNT = intPreferencesKey("word_count")
        val KEY_USED_WORDS = stringPreferencesKey("used_words")
        val KEY_CURRENT_WORD = stringPreferencesKey("current_word")
        val KEY_IS_GAME_OVER = intPreferencesKey("is_game_over")
    }

    val score: Flow<Int> = context.dataStore.data.map { it[KEY_SCORE] ?: 0 }
    val wordCount: Flow<Int> = context.dataStore.data.map { it[KEY_WORD_COUNT] ?: 1 }
    val usedWords: Flow<String> = context.dataStore.data.map { it[KEY_USED_WORDS] ?: "" }
    val currentWord: Flow<String> = context.dataStore.data.map { it[KEY_CURRENT_WORD] ?: "" }
    val isGameOver: Flow<Boolean> = context.dataStore.data.map { (it[KEY_IS_GAME_OVER] ?: 0) == 1 }

    suspend fun saveGameState(
        score: Int,
        wordCount: Int,
        usedWords: Set<String>,
        currentWord: String,
        isGameOver: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCORE] = score
            prefs[KEY_WORD_COUNT] = wordCount
            prefs[KEY_USED_WORDS] = usedWords.joinToString(",")
            prefs[KEY_CURRENT_WORD] = currentWord
            prefs[KEY_IS_GAME_OVER] = if (isGameOver) 1 else 0
        }
    }

    suspend fun clearGameState() {
        context.dataStore.edit { it.clear() }
    }
}