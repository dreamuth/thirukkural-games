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
import com.dreamuth.room.roomInfo
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
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import react.functionalComponent
import react.setState
import react.useEffect
import react.useState
import styled.css
import styled.styledDiv
import styled.styledP

val scope = MainScope()
val wsClient = WsClient(HttpClient { install(WebSockets) })

enum class GameState {
    NONE,
    CREATE,
    JOIN,
    ADMIN_ROOM,
    GUEST_ROOM
}

external interface AppState: RState {
    var isLoaded: Boolean
    var gameState: GameState
    var topic: Topic
    var question: String
    var question2: String?
    var kurals: List<Thirukkural>
    var roomName: String?
    var roomNames: List<String>
    var isAdminRoom: Boolean
    var adminPasscode: String?
    var guestPasscode: String?
}

class App : RComponent<RProps, AppState> () {
    override fun AppState.init() {
        scope.launch {
            setState {
                gameState = GameState.NONE
                topic = Topic.Athikaram
                question = "Loading..."
                kurals = listOf()
                roomNames = listOf()
                isAdminRoom = false
                isLoaded = true
            }
            wsClient.initConnection()
            wsClient.receive { message ->
                println(message)
                when {
                    message.startsWith(ClientCommand.ACTIVE_ROOMS.name) -> {
                        val data = message.removePrefix(ClientCommand.ACTIVE_ROOMS.name)
                        val roomNamesData = Json.decodeFromString<RoomNamesData>(data)
                        setState {
                            roomNames = roomNamesData.roomNames
                            roomName = if (roomNamesData.roomNames.contains(roomName)) roomName
                            else roomNamesData.roomNames.firstOrNull()
                        }
                    }
                    message.startsWith(ClientCommand.ADMIN_ROOM_RESPONSE.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_ROOM_RESPONSE.name)
                        val adminRoomResponse = Json.decodeFromString<AdminRoomResponse>(data)
                        setState {
                            isAdminRoom = true
                            adminPasscode = adminRoomResponse.adminPasscode
                            guestPasscode = adminRoomResponse.guestPasscode
                        }
                    }
                    message.startsWith(ClientCommand.ADMIN_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_QUESTION.name)
                        val adminQuestion = Json.decodeFromString<AdminQuestion>(data)
                        setState {
                            topic = adminQuestion.topic
                            question = adminQuestion.question
                            question2 = adminQuestion.question2
                            kurals = adminQuestion.thirukkurals
                            gameState = GameState.ADMIN_ROOM
                        }
                    }
                    message.startsWith(ClientCommand.GUEST_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.GUEST_QUESTION.name)
                        val guestQuestion = Json.decodeFromString<GuestQuestion>(data)
                        setState {
                            topic = guestQuestion.topic
                            question = guestQuestion.question
                            question2 = guestQuestion.question2
                            kurals = listOf()
                            gameState = GameState.GUEST_ROOM
                        }
                    }
                    message.startsWith(ClientCommand.SIGN_OUT.name) -> {
                        setState {
                            gameState = GameState.NONE
                            topic = Topic.Athikaram
                            question = "Loading..."
                            kurals = listOf()
                            isAdminRoom = false
                        }
                    }
                }
            }
        }
    }

    override fun RBuilder.render() {
        if (state.isLoaded) {
            styledDiv {
                css {
                    height = 100.pct
                    backgroundColor = if (state.gameState == GameState.NONE) Color("#f5f5f5") else Color.white
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
                    if (state.gameState != GameState.NONE) {
                        signOut {
                            onSignOutBtnClick = {
                                scope.launch {
                                    wsClient.trySend(ServerCommand.SIGN_OUT)
                                }
                            }
                        }
                    }
                    when (state.gameState) {
                        GameState.NONE -> {
                            gameMode {
                                roomNames = state.roomNames
                                roomName = state.roomName
                                onRoomNameChange = {
                                    roomName = it
                                }
                                onCreateBtnClick = { name ->
                                    println("sending ${ServerCommand.CREATE_ROOM}...")
                                    scope.launch {
                                        wsClient.trySend(
                                            ServerCommand.CREATE_ROOM.name + Json.encodeToString(CreateRoom(name)
                                            )
                                        )
                                    }
                                }
                                onAdminJoinBtnClick = { name, passcode ->
                                    println("sending ${ServerCommand.ADMIN_JOIN_ROOM}...")
                                    scope.launch {
                                        wsClient.trySend(
                                            ServerCommand.ADMIN_JOIN_ROOM.name + Json.encodeToString(
                                                AdminJoinRoom(name, passcode)
                                            )
                                        )
                                    }
                                }
                                onGuestJoinBtnClick = { name, passcode ->
                                    println("sending ${ServerCommand.GUEST_JOIN_ROOM}...")
                                    scope.launch {
                                        wsClient.trySend(
                                            ServerCommand.GUEST_JOIN_ROOM.name + Json.encodeToString(
                                                GuestJoinRoom(name, passcode)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        GameState.ADMIN_ROOM -> {
                            adminRoom {
                                topic = state.topic
                                question = state.question
                                question2 = state.question2
                                thirukkurals = state.kurals
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
                                topic = state.topic
                                question = state.question
                                question2 = state.question2
                            }
                        }
                        else -> println("Error state...")
                    }
                    if (state.isAdminRoom) {
                        roomInfo {
                            roomName = state.roomName!!
                            adminPasscode = state.adminPasscode!!
                            guestPasscode = state.guestPasscode!!
                        }
                    }
                }
            }
        }
    }
}
