/*
 * Copyright 2020 Uttran Ishtalingam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dreamuth

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.*
import kotlin.random.Random

enum class UserType{
    PRACTICE,
    ADMIN,
    GUEST
}

data class UserInfo(
    val userSession: UserSession,
    val socketSession: WebSocketSession,
    val roomName: String,
    val userType: UserType,
    val guestPasscode: String? = null,
    val adminPasscode: String? = null)

data class QuestionState(
    var selectedTopic: Topic,
    var thirukkurals: List<Thirukkural>,
    var athikaramState: AthikaramState,
    var thirukkuralState: ThirukkuralState,
    var firstWordState: FirstWordState,
    var lastWordState: LastWordState)

data class AthikaramState(
    override var index: Int,
    override var history: MutableList<Int>,
    val athikarams: List<String>
) : HistoryState {
    constructor(thirukkurals: List<Thirukkural>) : this(
        nextIndex(0, getAthikarams(thirukkurals).size),
        mutableListOf(),
        getAthikarams(thirukkurals))
    fun getCurrent(): String = athikarams[index]
    fun goNext() = goNext(athikarams.size)
    fun goPrevious() = goPrevious(athikarams.size)
    fun getReset(): AthikaramState = this.copy(index = getReset(athikarams.size))
}

private fun getAthikarams(thirukkurals: List<Thirukkural>) = thirukkurals.map { it.athikaram }.distinct()

data class ThirukkuralState(
    override var index: Int,
    override var history: MutableList<Int>,
    val kurals: List<Thirukkural>
) : HistoryState {
    constructor(thirukkurals: List<Thirukkural>): this(nextIndex(0, thirukkurals.size), mutableListOf(), thirukkurals)
    fun getCurrent(): Thirukkural = kurals[index]
    fun goNext() = goNext(kurals.size)
    fun goPrevious() = goPrevious(kurals.size)
    fun getReset(): ThirukkuralState = this.copy(index = getReset(kurals.size))
}

data class FirstWordState(
    override var index: Int,
    override var history: MutableList<Int>,
    val words: List<String>
) : HistoryState {
    constructor(thirukkurals: List<Thirukkural>): this(
        nextIndex(0, getFirstWords(thirukkurals).size),
        mutableListOf(),
        getFirstWords(thirukkurals))
    fun getCurrent(): String = words[index]
    fun goNext() = goNext(words.size)
    fun goPrevious() = goPrevious(words.size)
}

data class LastWordState(
    override var index: Int,
    override var history: MutableList<Int>,
    val words: List<String>
) : HistoryState {
    constructor(thirukkurals: List<Thirukkural>): this(
        nextIndex(0, getLastWords(thirukkurals).size),
        mutableListOf(),
        getLastWords(thirukkurals))
    fun getCurrent(): String = words[index]
    fun goNext() = goNext(words.size)
    fun goPrevious() = goPrevious(words.size)
}

private fun getFirstWords(thirukkurals: List<Thirukkural>) = thirukkurals.map { it.words.first() }.distinct()
private fun getLastWords(thirukkurals: List<Thirukkural>) = thirukkurals.map { it.words.last() }.distinct()

interface HistoryState {
    var index: Int
    var history: MutableList<Int>
    fun goNext(maxIndex: Int) {
        if (history.isEmpty()) {
            history = generateRandomList(maxIndex)
            history.remove(index)
            history.add(index)
        }
        val nextIndex = history.removeFirst()
        history.add(nextIndex)
        println("${this::class} Current: $index to New: $nextIndex of Total: $maxIndex")
        index = nextIndex
    }
    fun goPrevious(maxIndex: Int) {
        if (history.isEmpty()) {
            history = generateRandomList(maxIndex)
            history.remove(index)
            history.add(index)
        }
        var nextIndex = history.removeLast()
        history.add(0, nextIndex)
        nextIndex = history.last()
        println("${this::class} Current: $index to New: $nextIndex of Total: $maxIndex")
        index = nextIndex
    }
    fun getReset(maxIndex: Int): Int {
        if (history.isEmpty()) {
            history = generateRandomList(maxIndex)
            history.remove(index)
            history.add(index)
        }
        val nextIndex = history.last()
        println("${this::class} Current: $index to New: $nextIndex of Total: $maxIndex")
        return nextIndex
    }
}

fun generateRandomList(maxIndex: Int): MutableList<Int> {
    var count = maxIndex - 1
    val randomList = generateSequence { (count--).takeIf { it >= 0 } }.toMutableList()
    randomList.shuffle()
    return randomList
}

fun nextIndex(currentIndex: Int, maxIndex: Int): Int {
    var newIndex: Int
    do {
        newIndex = Random.nextInt(maxIndex)
    } while (newIndex == currentIndex && maxIndex != 1)
    println("Current: $currentIndex to New: $newIndex of Total: $maxIndex")
    return newIndex
}

fun fetchSource(): List<Thirukkural> {
    val sourceData = Thirukkural::class.java.classLoader.getResource("kurals.txt")!!.readText()
//    val sourceUrl = "https://raw.githubusercontent.com/dreamuth/dreamuth.github.io/master/kurals.txt"
//    val client = HttpClient(CIO)
//    val sourceData = client.get<String>(sourceUrl)
    val thirukkurals = readSource(sourceData)
    println("version: 2020-12-19.1")
//    println("Source: $sourceUrl loaded")
    return thirukkurals
}

fun readSource(sourceTxt: String): MutableList<Thirukkural> {
    val thirukurals = mutableListOf<Thirukkural>()
    val lines = sourceTxt.lines()
    val partNames = mapOf(
        "புனிதா" to "பகுதி 1",
        "தவப்ரியா" to "பகுதி 2",
        "அழகு" to "பகுதி 3",
        "ஆர்த்தி" to "பகுதி 4",
        "மைதிலி" to "பகுதி 5")
    var i = 0
    if (!lines[i++].startsWith("----------")) {
        println("First line should start with --------")
    }
    do {
        val person = partNames[lines[i++].trim()]!!
        val athikaram = lines[i++].split(" - ")
        val (athikaramNo, athikaramName) = athikaram[0].split(".")
        i++ // Empty line
        val kural1No = lines[i++].trim().toInt()
        val kural1FirstLine = lines[i++]
        val kural1SecondLine = lines[i++]
        val kural1Only = KuralOnly(kural1FirstLine, kural1SecondLine)
        val kural1Words = getWords("$kural1FirstLine $kural1SecondLine")
        if (kural1Words.size != 7) println("No: $kural1No, Words: $kural1Words")
        i++ // Empty line
        var kural1Porul = lines[i++]
        while (lines[i].isNotBlank()) {
            kural1Porul += "\n"
            kural1Porul += lines[i++]
        }
        i++ // Empty line
        val kural2No = lines[i++].trim().toInt()
        val kural2FirstLine = lines[i++]
        val kural2SecondLine = lines[i++]
        val kural2Only = KuralOnly(kural2FirstLine, kural2SecondLine)
        val kural2Words = getWords("$kural2FirstLine $kural2SecondLine")
        if (kural2Words.size != 7) println("No: $kural2No, Words: $kural2Words")
        i++ // Empty line
        var kural2Porul = lines[i++]
        while (!lines[i].startsWith("----")) {
            kural2Porul += "\n"
            kural2Porul += lines[i++]
        }
        i++ // Empty line
        val thirukural1 = Thirukkural(athikaramNo.toInt(), athikaramName, athikaram[1], kural1No, kural1Only, kural1Porul, kural1Words)
        val thirukural2 = Thirukkural(athikaramNo.toInt(), athikaramName, athikaram[1], kural2No, kural2Only, kural2Porul, kural2Words)
        thirukurals.add(thirukural1)
        thirukurals.add(thirukural2)
    } while (i < lines.size && i + 5 < lines.size)
    return thirukurals
}

fun getWords(line: String): List<String> {
    val list = mutableListOf<String>()
    var currentWord = ""
    val charArray = line.toCharArray()
    for (char in charArray) {
        when (char.toInt()) {
            // Space, Dot, Zero-width non-joiner
            32, 39, 44, 46, 58, 8204 -> {
                if (currentWord.isNotBlank()) {
                    list.add(currentWord)
                }
                currentWord = ""
            }
            else -> {
                currentWord += char
            }
        }
    }
    if (currentWord.isNotBlank()) list.add(currentWord)
    return list
}
