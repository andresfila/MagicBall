
package com.andresfila.magicball

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class MagicBallHistoryEntry(
    val question: String,
    val answer: String
)

class MagicBallHistory(context: Context) {

    private val preferences = context.getSharedPreferences(
        "magicball_history",
        Context.MODE_PRIVATE
    )

    private val historyKey = "recent_questions"
    private val maxEntries = 10

    private fun normalize(question: String): String =
        question.trim()
            .replace(Regex("\\s+"), " ")
            .trimEnd('?', '!', '.')
            .trim()
            .lowercase(Locale.ROOT)

    fun getHistory(): List<MagicBallHistoryEntry> {
        return try {
            val stored = preferences.getString(historyKey, "[]") ?: "[]"
            val array = JSONArray(stored)

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)

                    add(
                        MagicBallHistoryEntry(
                            question = item.getString("question"),
                            answer = item.getString("answer")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun findAnswer(question: String): String? {
        val key = normalize(question)

        return getHistory()
            .firstOrNull { normalize(it.question) == key }
            ?.answer
    }

    fun saveAnswer(question: String, answer: String) {
        val key = normalize(question)
        if (key.isEmpty()) return

        val updated = getHistory()
            .filterNot { normalize(it.question) == key }
            .toMutableList()

        updated.add(
            0,
            MagicBallHistoryEntry(
                question = question.trim(),
                answer = answer
            )
        )

        val array = JSONArray()

        updated.take(maxEntries).forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("question", entry.question)
                    put("answer", entry.answer)
                }
            )
        }

        preferences.edit()
            .putString(historyKey, array.toString())
            .apply()
    }

    fun getOrCreateAnswer(
        question: String,
        generate: () -> String
    ): String {
        val previous = findAnswer(question)

        if (previous != null) {
            // Move repeated question to the top of history
            saveAnswer(question, previous)
            return previous
        }

        val newAnswer = generate()
        saveAnswer(question, newAnswer)

        return newAnswer
    }

    // Delete one question and its stored answer
    fun deleteQuestion(question: String) {
        val key = normalize(question)

        val updated = getHistory()
            .filterNot { normalize(it.question) == key }

        val array = JSONArray()

        updated.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("question", entry.question)
                    put("answer", entry.answer)
                }
            )
        }

        preferences.edit()
            .putString(historyKey, array.toString())
            .apply()
    }

    // Delete all stored questions and answers
    fun clearHistory() {
        preferences.edit()
            .remove(historyKey)
            .apply()
    }
}