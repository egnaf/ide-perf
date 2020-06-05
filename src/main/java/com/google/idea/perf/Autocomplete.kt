/*
 * Copyright 2020 Google LLC
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

package com.google.idea.perf

data class Suggestion(val name: String, val detail: String?)

class Autocomplete {
    private var classNames: List<String> = emptyList()

    fun setClasses(classes: Collection<Class<*>>) {
        classNames = classes.mapNotNull { it.canonicalName }
    }

    fun predict(input: String, index: Int, cancellationCheck: () -> Unit): List<Suggestion> {
        val tokens = input.trimStart().split(' ')
        val normalizedInput = tokens.joinToString(" ")
        val command = parseTracerCommand(normalizedInput)
        val tokenIndex = getTokenIndex(normalizedInput, index)
        val token = tokens.getOrElse(tokenIndex) { "" }

        return when (tokenIndex) {
            0 -> predictOptionToken(
                listOf("clear", "reset", "trace", "untrace"), token, cancellationCheck
            )
            1 -> when (command) {
                is TracerCommand.Trace -> predictOptionToken(
                    listOf("all", "count", "wall-time"), token, cancellationCheck
                )
                else -> emptyList()
            }
            2 -> when (command) {
                is TracerCommand.Trace -> predictClassToken(classNames, token, cancellationCheck)
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun predictOptionToken(
        choices: Collection<String>,
        token: String,
        cancellationCheck: () -> Unit
    ): List<Suggestion> {
        return predictToken(choices, token, { it.formattedSource }, { null }, cancellationCheck)
    }

    private fun predictClassToken(
        choices: Collection<String>,
        token: String,
        cancellationCheck: () -> Unit
    ): List<Suggestion> {
        return predictToken(choices, token, { it.formattedSource }, { null }, cancellationCheck)
    }

    private fun predictToken(
        choices: Collection<String>,
        token: String,
        nameFunc: (MatchResult) -> String,
        detailFunc: (MatchResult) -> String?,
        cancellationCheck: () -> Unit
    ): List<Suggestion> {
        return fuzzySearch(choices, token) { cancellationCheck() }
            .map { Suggestion(nameFunc(it), detailFunc(it)) }
    }

    private fun getTokenIndex(input: String, index: Int): Int {
        return input.subSequence(0, index).count(Char::isWhitespace)
    }
}