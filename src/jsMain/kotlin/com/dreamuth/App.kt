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
import com.dreamuth.room.guestQuestion
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
    CREATE,
    JOIN,
    ADMIN_ROOM,
    GUEST_ROOM
}

data class ActiveGame(
    var gameState: GameState = GameState.NONE,
    var topic: Topic = Topic.Athikaram,
    var question: String = "Loading...",
    var question2: String? = null,
    var kurals: List<Thirukkural> = listOf(),
    var roomName: String? = null,
    var roomNames: List<String> = listOf(),
    var isAdminRoom: Boolean = false,
    var adminPasscode: String? = null,
    var guestPasscode: String? = null
)

val app = functionalComponent<RProps> {
    var activeGame by useState(ActiveGame())

    useEffect(listOf()) {
        scope.launch {
            wsClient.initConnection()
            wsClient.receive { message ->
                println(message)
                when {
                    message.startsWith(ClientCommand.ACTIVE_ROOMS.name) -> {
                        val data = message.removePrefix(ClientCommand.ACTIVE_ROOMS.name)
                        val roomNamesData = Json.decodeFromString<RoomNamesData>(data)
                        activeGame = activeGame.copy(
                            roomNames = roomNamesData.roomNames,
                            roomName = if (roomNamesData.roomNames.contains(activeGame.roomName)) activeGame.roomName
                                else roomNamesData.roomNames.firstOrNull()
                        )
                    }
                    message.startsWith(ClientCommand.ADMIN_ROOM_RESPONSE.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_ROOM_RESPONSE.name)
                        val adminRoomResponse = Json.decodeFromString<AdminRoomResponse>(data)
                        activeGame = activeGame.copy(
                            isAdminRoom = true,
                            adminPasscode = adminRoomResponse.adminPasscode,
                            guestPasscode = adminRoomResponse.guestPasscode
                        )
                    }
                    message.startsWith(ClientCommand.ADMIN_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_QUESTION.name)
                        val adminQuestion = Json.decodeFromString<AdminQuestion>(data)
                        activeGame = activeGame.copy(
                            topic = adminQuestion.topic,
                            question = adminQuestion.question,
                            question2 = adminQuestion.question2,
                            kurals = adminQuestion.thirukkurals,
                            gameState = GameState.ADMIN_ROOM
                        )
                    }
                    message.startsWith(ClientCommand.GUEST_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.GUEST_QUESTION.name)
                        val guestQuestion = Json.decodeFromString<GuestQuestion>(data)
                        activeGame = activeGame.copy(
                            topic = guestQuestion.topic,
                            question = guestQuestion.question,
                            question2 = guestQuestion.question2,
                            kurals = listOf(),
                            gameState = GameState.GUEST_ROOM
                        )
                    }
                    message.startsWith(ClientCommand.SIGN_OUT.name) -> {
                        activeGame = ActiveGame()
                    }
                }
            }
        }
    }

    styledDiv {
        css {
            height = 100.pct
            backgroundColor = if (activeGame.gameState == GameState.NONE) Color("#f5f5f5") else Color.white
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
            if (activeGame.gameState != GameState.NONE) {
                signOut {
                    onSignOutBtnClick = {
                        scope.launch {
                            wsClient.trySend(ServerCommand.SIGN_OUT)
                        }
                    }
                }
            }
            when (activeGame.gameState) {
                GameState.NONE -> {
                    gameMode {
                        roomNames = activeGame.roomNames
                        roomName = activeGame.roomName
                        onRoomNameChange = {
                            roomName = it
                        }
                        onCreateBtnClick = { name ->
                            println("sending ${ServerCommand.CREATE_ROOM}...")
                            scope.launch {
                                wsClient.trySend(ServerCommand.CREATE_ROOM.name + Json.encodeToString(CreateRoom(name)))
                            }
                        }
                        onAdminJoinBtnClick = { name, passcode ->
                            println("sending ${ServerCommand.ADMIN_JOIN_ROOM}...")
                            scope.launch {
                                wsClient.trySend(ServerCommand.ADMIN_JOIN_ROOM.name + Json.encodeToString(AdminJoinRoom(name, passcode)))
                            }
                        }
                        onGuestJoinBtnClick = { name, passcode ->
                            println("sending ${ServerCommand.GUEST_JOIN_ROOM}...")
                            scope.launch {
                                wsClient.trySend(ServerCommand.GUEST_JOIN_ROOM.name + Json.encodeToString(GuestJoinRoom(name, passcode)))
                            }
                        }
                    }
                }
                GameState.ADMIN_ROOM -> {
                    adminRoom {
                        topic = activeGame.topic
                        question = activeGame.question
                        question2 = activeGame.question2
                        thirukkurals = activeGame.kurals
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
                        onNextClick = {
                            scope.launch {
                                wsClient.trySend(ServerCommand.NEXT)
                            }
                        }
                    }
                }
                GameState.GUEST_ROOM -> {
                    guestQuestion {
                        topic = activeGame.topic
                        question = activeGame.question
                        question2 = activeGame.question2
                    }
                }
                else -> println("Error state...")
            }
        }
    }
}
