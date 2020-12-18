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

import com.dreamuth.login.create
import com.dreamuth.login.createOrJoin
import com.dreamuth.login.gameMode
import com.dreamuth.login.join
import com.dreamuth.room.adminRoom
import com.dreamuth.room.practice
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.RProps
import react.functionalComponent
import react.useEffect
import react.useState
import styled.styledDiv

val scope = MainScope()
val wsClient = WsClient(HttpClient { install(WebSockets) })

enum class GameState {
    NONE,
    PRACTICE,
    CREATE_OR_JOIN,
    CREATE,
    JOIN,
    ADMIN_ROOM
}

val app = functionalComponent<RProps> {
    val text by useState("Loading...")
    var gameState by useState(GameState.NONE)
    var activeTopic by useState(Topic.Athikaram)
    var activeQuestion by useState("loading...")
    var activeKuralQuestion by useState(KuralOnly("Error..", "Error..."))
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
                }
            }
        }
    }

    styledDiv {
        header {
            activeState = gameState
            onSignOutBtnClick = {
                gameState = when (gameState) {
                    GameState.CREATE_OR_JOIN, GameState.PRACTICE, GameState.ADMIN_ROOM -> GameState.NONE
                    GameState.JOIN, GameState.CREATE -> GameState.CREATE_OR_JOIN
                    else -> GameState.NONE
                }
            }
        }
        when (gameState) {
            GameState.NONE -> {
                gameMode {
                    onGameBtnClick = {
                        gameState = GameState.CREATE_OR_JOIN
                    }
                    onPracticeBtnClick = {
                        gameState = GameState.PRACTICE
                        scope.launch {
                            wsClient.send(ServerCommand.PRACTICE)
                        }
                    }
                }
            }
            GameState.CREATE_OR_JOIN -> {
                createOrJoin {
                    onCreateBtnClick = {
                        gameState = GameState.CREATE
                    }
                    onJoinBtnClick = {
                        gameState = GameState.JOIN
                    }
                }
            }
            GameState.CREATE -> {
                create {
                    onCreateBtnClick = {
                        gameState = GameState.ADMIN_ROOM
                    }
                }
            }
            GameState.JOIN -> {
                join {
                    onJoinBtnClick = {
                        gameState = GameState.ADMIN_ROOM
                    }
                }
            }
            GameState.ADMIN_ROOM -> {
                adminRoom {  }
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
                            wsClient.send(ServerCommand.TOPIC_CHANGE.name + it.name)
                        }
                    }
                    onPreviousClick = {
                        scope.launch {
                            wsClient.send(ServerCommand.PREVIOUS)
                        }
                    }
                    onShowAnswerClick = {
                        activeShowAnswer = it
                    }
                    onNextClick = {
                        scope.launch {
                            wsClient.send(ServerCommand.NEXT)
                        }
                    }
                }
            }
            else -> {
                println("else...")
                styledDiv {
                    +text
                }
            }
        }
    }
}
