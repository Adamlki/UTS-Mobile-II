/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unscramble.data.CorrectWordEntity
import com.example.unscramble.data.GamePreferences
import com.example.unscramble.data.GameRepository
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: GameRepository,
    private val prefs: GamePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var userGuess by mutableStateOf("")
        private set

    // History kata benar dari DB (Flow)
    val correctWordsHistory = repository.getAllCorrectWords()

    private var usedWords: MutableSet<String> = mutableSetOf()
    private lateinit var currentWord: String

    init {
        // Restore game state dari DataStore saat init
        viewModelScope.launch {
            val savedScore = prefs.score.first()
            val savedWordCount = prefs.wordCount.first()
            val savedUsedWordsStr = prefs.usedWords.first()
            val savedCurrentWord = prefs.currentWord.first()
            val savedIsGameOver = prefs.isGameOver.first()

            if (savedUsedWordsStr.isNotEmpty()) {
                // Ada saved state, restore
                usedWords = savedUsedWordsStr.split(",").toMutableSet()
                currentWord = savedCurrentWord.ifEmpty {
                    pickRandomWordAndShuffle()
                }
                _uiState.value = GameUiState(
                    currentScrambledWord = shuffleCurrentWord(currentWord),
                    currentWordCount = savedWordCount,
                    score = savedScore,
                    isGameOver = savedIsGameOver
                )
            } else {
                // Fresh start
                resetGame()
            }
        }
    }

    fun resetGame() {
        usedWords.clear()
        val scrambled = pickRandomWordAndShuffle()
        _uiState.value = GameUiState(currentScrambledWord = scrambled)
        viewModelScope.launch {
            prefs.saveGameState(0, 1, usedWords, currentWord, false)
        }
    }

    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            // Simpan kata benar ke database
            viewModelScope.launch {
                repository.insertCorrectWord(currentWord)
            }
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            _uiState.update { it.copy(isGuessedWordWrong = true) }
        }
        updateUserGuess("")
    }

    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            _uiState.update { it.copy(isGuessedWordWrong = false, score = updatedScore, isGameOver = true) }
            viewModelScope.launch {
                prefs.saveGameState(updatedScore, _uiState.value.currentWordCount, usedWords, currentWord, true)
            }
        } else {
            val newScrambled = pickRandomWordAndShuffle()
            _uiState.update {
                it.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = newScrambled,
                    currentWordCount = it.currentWordCount.inc(),
                    score = updatedScore
                )
            }
            viewModelScope.launch {
                prefs.saveGameState(updatedScore, _uiState.value.currentWordCount, usedWords, currentWord, false)
            }
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word) tempWord.shuffle()
        return String(tempWord)
    }

    private fun pickRandomWordAndShuffle(): String {
        currentWord = allWords.random()
        return if (usedWords.contains(currentWord)) {
            pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            shuffleCurrentWord(currentWord)
        }
    }

    // Factory untuk inject dependency
    companion object {
        fun factory(repository: GameRepository, prefs: GamePreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(repository, prefs) as T
            }
    }
}