package com.example.unscramble.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "correct_words")
data class CorrectWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val timestamp: Long = System.currentTimeMillis()
)