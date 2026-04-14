package com.example.unscramble.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: CorrectWordDao) {
    fun getAllCorrectWords(): Flow<List<CorrectWordEntity>> = dao.getAllWords()
    suspend fun insertCorrectWord(word: String) = dao.insertWord(CorrectWordEntity(word = word))
    suspend fun clearAll() = dao.deleteAll()
}