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

import com.dreamuth.login.gameStateComp
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.setState
import styled.css
import styled.styledDiv

val scope = MainScope()
val wsClient = WsClient(HttpClient { install(WebSockets) })

enum class GameState {
    NONE,
    CREATE,
    JOIN,
    CATEGORY_SELECTION,
    ADMIN_ROOM,
    GUEST_ROOM
}

external interface AppState: RState {
    var isLoaded: Boolean
    var gameState: GameState
    var timerState: TimerState
    var topicState: TopicState
    var adminQuestion: AdminQuestion
    var guestQuestion: GuestQuestion
    var roomName: String?
    var roomNames: List<String>
    var isAdminRoom: Boolean
    var adminPasscode: String?
    var guestPasscode: String?
    var createRoomErrorMsg: String?
    var joinRoomErrorMsg: String?
}

class App : RComponent<RProps, AppState> () {
    override fun AppState.init() {
        scope.launch {
            setState {
                gameState = GameState.NONE
                topicState = TopicState()
                timerState = TimerState()
                adminQuestion = AdminQuestion()
                guestQuestion = GuestQuestion()
                roomNames = listOf()
                isAdminRoom = false
                isLoaded = true
            }
            wsClient.initConnection()
            wsClient.receive { message ->
                println("Received: $message")
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
                    message.startsWith(ClientCommand.ADMIN_CREATED_ROOM.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_CREATED_ROOM.name)
                        val adminRoomResponse = Json.decodeFromString<AdminRoomResponse>(data)
                        setState {
                            createRoomErrorMsg = null
                            joinRoomErrorMsg = null
                            isAdminRoom = true
                            adminPasscode = adminRoomResponse.adminPasscode
                            guestPasscode = adminRoomResponse.guestPasscode
                            gameState = GameState.ADMIN_ROOM
                        }
                    }
                    message.startsWith(ClientCommand.ADMIN_JOINED_ROOM.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_JOINED_ROOM.name)
                        val adminRoomResponse = Json.decodeFromString<AdminRoomResponse>(data)
                        setState {
                            createRoomErrorMsg = null
                            joinRoomErrorMsg = null
                            isAdminRoom = true
                            adminPasscode = adminRoomResponse.adminPasscode
                            guestPasscode = adminRoomResponse.guestPasscode
                            gameState = GameState.ADMIN_ROOM
                        }
                    }
                    message.startsWith(ClientCommand.ADMIN_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.ADMIN_QUESTION.name)
                        val receivedAdminQuestion = Json.decodeFromString<AdminQuestion>(data)
                        setState {
                            adminQuestion = receivedAdminQuestion
                            // TODO: Do we need to reset the timer?
                        }
                    }
                    message.startsWith(ClientCommand.GUEST_JOINED_ROOM.name) -> {
                        setState {
                            gameState = GameState.GUEST_ROOM
                        }
                    }
                    message.startsWith(ClientCommand.GUEST_QUESTION.name) -> {
                        val data = message.removePrefix(ClientCommand.GUEST_QUESTION.name)
                        val receivedGuestQuestion = Json.decodeFromString<GuestQuestion>(data)
                        setState {
                            guestQuestion = receivedGuestQuestion
                        }
                    }
                    message.startsWith(ClientCommand.TIME_UPDATE.name) -> {
                        val data = message.removePrefix(ClientCommand.TIME_UPDATE.name)
                        val receivedTimerState = Json.decodeFromString<TimerState>(data)
                        setState {
                            timerState = receivedTimerState
                        }
                    }
                    message.startsWith(ClientCommand.TOPIC_STATE.name) -> {
                        val data = message.removePrefix(ClientCommand.TOPIC_STATE.name)
                        val receivedTopicState = Json.decodeFromString<TopicState>(data)
                        setState {
                            topicState = receivedTopicState
                        }
                    }
                    message.startsWith(ClientCommand.SIGN_OUT.name) -> {
                        setState {
                            gameState = GameState.NONE
                            timerState = TimerState()
                            topicState = TopicState()
                            adminQuestion = AdminQuestion()
                            guestQuestion = GuestQuestion()
                            isAdminRoom = false
                        }
                    }
                    message.startsWith(ClientCommand.GUEST_JOINED_ROOM.name) -> {
                        setState {
                            createRoomErrorMsg = null
                            joinRoomErrorMsg = null
                        }
                    }
                    message.startsWith(ClientCommand.ERROR_ROOM_EXISTS.name) -> {
                        val data = message.removePrefix(ClientCommand.ERROR_ROOM_EXISTS.name)
                        val room = Json.decodeFromString<Room>(data)
                        setState {
                            createRoomErrorMsg = "Room [${room.name}] already exists. Please use different name"
                        }
                    }
                    message.startsWith(ClientCommand.ERROR_ROOM_NOT_EXISTS.name) -> {
                        val data = message.removePrefix(ClientCommand.ERROR_ROOM_EXISTS.name)
                        val room = Json.decodeFromString<Room>(data)
                        setState {
                            joinRoomErrorMsg = "Select room [${room.name}] no longer exists"
                        }
                    }
                    message.startsWith(ClientCommand.ERROR_INVALID_PASSCODE.name) -> {
                        setState {
                            joinRoomErrorMsg = "Invalid passcode"
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
                if (state.gameState != GameState.NONE) {
                    signOut { }
                }
                styledDiv {
                    css {
                        classes = mutableListOf("container-fluid")
                    }
                    gameStateComp {
                        gameState = state.gameState
                        roomName = state.roomName
                        roomNames = state.roomNames
                        adminQuestion = state.adminQuestion
                        guestQuestion = state.guestQuestion
                        timerState = state.timerState
                        topicState = state.topicState
                        adminPasscode = state.adminPasscode
                        guestPasscode = state.guestPasscode
                        createRoomErrorMsg = state.createRoomErrorMsg
                        joinRoomErrorMsg = state.joinRoomErrorMsg
                    }
                }
            }
        }
    }
}
