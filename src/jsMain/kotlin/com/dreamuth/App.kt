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

import com.dreamuth.login.gameMode
import com.dreamuth.room.adminRoom
import com.dreamuth.room.practice
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.RProps
import react.functionalComponent
import react.useEffect
import react.useState
import styled.css
import styled.styledDiv

val scope = MainScope()
val wsClient = WsClient(HttpClient { install(WebSockets) })

enum class GameState {
    NONE,
    PRACTICE,
    CREATE,
    JOIN,
    ADMIN_ROOM
}

val app = functionalComponent<RProps> {
    var gameState by useState(GameState.NONE)
    var activeTopic by useState(Topic.Athikaram)
    var activeQuestion by useState("loading...")
    var activeKuralQuestion by useState(KuralOnly("loading...", ""))
    var activeKurals by useState(listOf<Thirukkural>())
    var activeShowAnswer by useState(false)

    useEffect(listOf()) {
        scope.launch {
            wsClient.initConnection()
            wsClient.receive { message ->
                println(message)
                when {
                    message.startsWith(ClientCommand.PRACTICE_RESPONSE.name) -> {
                        val data = message.removePrefix(ClientCommand.PRACTICE_RESPONSE.name)
                        val practiceData = Json.decodeFromString<PracticeData>(data)
                        gameState = GameState.PRACTICE
                        activeTopic = practiceData.topic
                        activeQuestion = practiceData.question
                        activeKurals = practiceData.thirukkurals
                        activeShowAnswer = false
                    }
                    message.startsWith(ClientCommand.PRACTICE_KURAL_RESPONSE.name) -> {
                        val data = message.removePrefix(ClientCommand.PRACTICE_KURAL_RESPONSE.name)
                        val practiceData = Json.decodeFromString<PracticeKuralData>(data)
                        gameState = GameState.PRACTICE
                        activeTopic = practiceData.topic
                        activeKuralQuestion = practiceData.question
                        activeKurals = practiceData.thirukkurals
                        activeShowAnswer = false
                    }
                    message.startsWith(ClientCommand.SIGN_OUT.name) -> {
                        gameState = GameState.NONE
                        activeTopic = Topic.Athikaram
                        activeQuestion = "loading..."
                        activeKuralQuestion = KuralOnly("loading...", "")
                        activeKurals = listOf()
                        activeShowAnswer = false
                    }
                }
            }
        }
    }

    styledDiv {
        css {
            height = 100.pct
            backgroundColor = if (gameState == GameState.NONE) Color("#f5f5f5") else Color.white
        }
        styledDiv {
            css {
                classes = mutableListOf("alert alert-primary text-center mb-0 rounded-0")
            }
            +"திருக்குறள் விளையாட்டு"
        }
        styledDiv {
            css {
                classes = mutableListOf("container-lg")
            }
            if (gameState != GameState.NONE) {
                signOut {
                    onSignOutBtnClick = {
                        scope.launch {
                            wsClient.trySend(ServerCommand.SIGN_OUT)
                        }
                    }
                }
            }
            when (gameState) {
                GameState.NONE -> {
                    gameMode {
                        onCreateBtnClick = {
                            gameState = GameState.ADMIN_ROOM
                            println("sending ${ServerCommand.CREATE_ROOM}...")
                            scope.launch {
                                wsClient.trySend(ServerCommand.CREATE_ROOM.name + Json.encodeToString(CreateRoom(it)))
                            }
                        }
                        onJoinBtnClick = {
                            gameState = GameState.PRACTICE
                            println("sending Practice...")
                            scope.launch {
                                wsClient.trySend(ServerCommand.PRACTICE)
                            }
                        }
                    }
                }
                GameState.ADMIN_ROOM -> {
                    adminRoom {
                        topic = activeTopic
                        question = activeQuestion
                        kuralQuestion = activeKuralQuestion
                        thirukkurals = activeKurals
                        showAnswer = activeShowAnswer
                        onTopicClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.TOPIC_CHANGE.name + it.name)
                            }
                        }
                        onPreviousClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.PREVIOUS)
                            }
                        }
                        onShowAnswerClick = {
                            activeShowAnswer = it
                        }
                        onNextClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.NEXT)
                            }
                        }
                    }
                }
                GameState.PRACTICE -> {
                    practice {
                        topic = activeTopic
                        question = activeQuestion
                        kuralQuestion = activeKuralQuestion
                        thirukkurals = activeKurals
                        showAnswer = activeShowAnswer
                        onTopicClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.TOPIC_CHANGE.name + it.name)
                            }
                        }
                        onPreviousClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.PREVIOUS)
                            }
                        }
                        onShowAnswerClick = {
                            activeShowAnswer = it
                        }
                        onNextClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.NEXT)
                            }
                        }
                    }
                }
                else -> println("Error state...")
            }
        }
    }
}
