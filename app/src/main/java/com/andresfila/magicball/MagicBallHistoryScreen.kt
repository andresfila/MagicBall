
package com.andresfila.magicball

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MagicBallHistoryScreen(
    historyManager: MagicBallHistory,
    onBack: () -> Unit
) {
    var entries by remember {
        mutableStateOf(historyManager.getHistory())
    }

    var questionToDelete by remember {
        mutableStateOf<String?>(null)
    }

    var confirmClearAll by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text("← Back to Settings", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Question History",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "${entries.size} of 10 saved questions",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Text("No questions saved yet.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = entries,
                    key = { it.question }
                ) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = entry.question,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Answer: ${entry.answer}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            TextButton(
                                onClick = {
                                    questionToDelete = entry.question
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { confirmClearAll = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear All History")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Confirmation for deleting one question
    questionToDelete?.let { question ->
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text("Delete question?") },
            text = {
                Text("This question and its saved answer will be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyManager.deleteQuestion(question)
                        entries = historyManager.getHistory()
                        questionToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { questionToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation for clearing all history
    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text("Clear all history?") },
            text = {
                Text("All saved questions and answers will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyManager.clearHistory()
                        entries = historyManager.getHistory()
                        confirmClearAll = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmClearAll = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
