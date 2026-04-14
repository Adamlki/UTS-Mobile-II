package com.example.unscramble.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrectWordDao {
    @Insert
    suspend fun insertWord(word: CorrectWordEntity)

    @Query("SELECT * FROM correct_words ORDER BY timestamp DESC")
    fun getAllWords(): Flow<List<CorrectWordEntity>>

    @Query("DELETE FROM correct_words")
    suspend fun deleteAll()
}