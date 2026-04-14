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

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unscramble.R
import com.example.unscramble.data.GameDatabase
import com.example.unscramble.data.GamePreferences
import com.example.unscramble.data.GameRepository
import com.example.unscramble.ui.theme.UnscrambleTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GameScreen() {
    val context = LocalContext.current
    val db = remember { GameDatabase.getDatabase(context) }
    val repository = remember { GameRepository(db.correctWordDao()) }
    val prefs = remember { GamePreferences(context) }
    val gameViewModel: GameViewModel = viewModel(
        factory = GameViewModel.factory(repository, prefs)
    )

    val gameUiState by gameViewModel.uiState.collectAsState()
    val correctWordsHistory by gameViewModel.correctWordsHistory.collectAsState(initial = emptyList())
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    // State untuk tampilkan dialog history
    var showHistory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(mediumPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = typography.titleLarge,
        )
        GameLayout(
            onUserGuessChanged = { gameViewModel.updateUserGuess(it) },
            wordCount = gameUiState.currentWordCount,
            userGuess = gameViewModel.userGuess,
            onKeyboardDone = { gameViewModel.checkUserGuess() },
            currentScrambledWord = gameUiState.currentScrambledWord,
            isGuessWrong = gameUiState.isGuessedWordWrong,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(mediumPadding)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(mediumPadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { gameViewModel.checkUserGuess() }
            ) {
                Text(text = stringResource(R.string.submit), fontSize = 16.sp)
            }

            // Row: Skip + History berdampingan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(mediumPadding)
            ) {
                OutlinedButton(
                    onClick = { gameViewModel.skipWord() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.skip), fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = { showHistory = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "History", fontSize = 16.sp)
                }
            }
        }

        GameStatus(score = gameUiState.score, modifier = Modifier.padding(20.dp))

        if (gameUiState.isGameOver) {
            FinalScoreDialog(
                score = gameUiState.score,
                onPlayAgain = { gameViewModel.resetGame() }
            )
        }
    }

    // Dialog History
    if (showHistory) {
        HistoryDialog(
            words = correctWordsHistory,
            onDismiss = { showHistory = false }
        )
    }
}

@Composable
fun HistoryDialog(
    words: List<com.example.unscramble.data.CorrectWordEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "History Jawaban Benar") },
        text = {
            if (words.isEmpty()) {
                Text(
                    text = "Belum ada kata yang berhasil ditebak.",
                    style = typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(words) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = item.word,
                                    style = typography.titleMedium,
                                    color = colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    style = typography.bodySmall,
                                    color = colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Tutup")
            }
        }
    )
}

@Composable
fun GameStatus(score: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Text(
            text = stringResource(R.string.score, score),
            style = typography.headlineMedium,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun GameLayout(
    currentScrambledWord: String,
    wordCount: Int,
    isGuessWrong: Boolean,
    userGuess: String,
    onUserGuessChanged: (String) -> Unit,
    onKeyboardDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(mediumPadding)
        ) {
            Text(
                modifier = Modifier
                    .clip(shapes.medium)
                    .background(colorScheme.surfaceTint)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(alignment = Alignment.End),
                text = stringResource(R.string.word_count, wordCount),
                style = typography.titleMedium,
                color = colorScheme.onPrimary
            )
            Text(text = currentScrambledWord, style = typography.displayMedium)
            Text(
                text = stringResource(R.string.instructions),
                textAlign = TextAlign.Center,
                style = typography.titleMedium
            )
            OutlinedTextField(
                value = userGuess,
                singleLine = true,
                shape = shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    disabledContainerColor = colorScheme.surface,
                ),
                onValueChange = onUserGuessChanged,
                label = {
                    if (isGuessWrong) Text(stringResource(R.string.wrong_guess))
                    else Text(stringResource(R.string.enter_your_word))
                },
                isError = isGuessWrong,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onKeyboardDone() })
            )
        }
    }
}

@Composable
private fun FinalScoreDialog(score: Int, onPlayAgain: () -> Unit, modifier: Modifier = Modifier) {
    val activity = (LocalContext.current as Activity)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.congratulations)) },
        text = { Text(text = stringResource(R.string.you_scored, score)) },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = { activity.finish() }) {
                Text(text = stringResource(R.string.exit))
            }
        },
        confirmButton = {
            TextButton(onClick = onPlayAgain) {
                Text(text = stringResource(R.string.play_again))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    UnscrambleTheme { GameScreen() }
}