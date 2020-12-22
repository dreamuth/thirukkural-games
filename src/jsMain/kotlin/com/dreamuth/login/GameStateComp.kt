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

package com.dreamuth.login

import com.dreamuth.AdminJoinRoom
import com.dreamuth.GameState
import com.dreamuth.GuestJoinRoom
import com.dreamuth.Room
import com.dreamuth.ServerCommand
import com.dreamuth.Thirukkural
import com.dreamuth.Topic
import com.dreamuth.room.adminRoom
import com.dreamuth.room.guestQuestion
import com.dreamuth.room.roomInfo
import com.dreamuth.scope
import com.dreamuth.wsClient
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv

external interface GameStateCompProps: RProps {
    var gameState: GameState
    var roomNames: List<String>
    var roomName: String?
    var topic: Topic
    var question: String
    var question2: String?
    var thirukkurals: List<Thirukkural>
    var adminPasscode: String?
    var guestPasscode: String?
    var createRoomErrorMsg: String?
    var joinRoomErrorMsg: String?
}

private var gameStateComp = functionalComponent<GameStateCompProps> { props ->
    when (props.gameState) {
        GameState.NONE -> {
            gameMode {
                roomNames = props.roomNames
                roomName = props.roomName
                createRoomErrorMsg = props.createRoomErrorMsg
                joinRoomErrorMsg = props.joinRoomErrorMsg
                onRoomNameChange = {
                    roomName = it
                }
                onCreateBtnClick = { name ->
                    println("sending ${ServerCommand.CREATE_ROOM}...")
                    scope.launch {
                        wsClient.trySend(
                            ServerCommand.CREATE_ROOM.name + Json.encodeToString(
                                Room(name)
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
            styledDiv {
                css {
                    classes = mutableListOf("row")
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-9 pr-0")
                    }
                    adminRoom {
                        topic = props.topic
                        question = props.question
                        question2 = props.question2
                        thirukkurals = props.thirukkurals
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
                styledDiv {
                    css {
                        classes = mutableListOf("col-3 pl-0")
                    }
                    if (props.roomName != null
                        && props.adminPasscode != null
                        && props.guestPasscode != null) {
                        roomInfo {
                            roomName = props.roomName!!
                            adminPasscode = props.adminPasscode!!
                            guestPasscode = props.guestPasscode!!
                        }
                    }
                }
            }
        }
        GameState.GUEST_ROOM -> {
            guestQuestion {
                topic = props.topic
                question = props.question
                question2 = props.question2
            }
        }
        else -> println("Error state...")
    }
}

fun RBuilder.gameStateComp(handler: GameStateCompProps.() -> Unit) = child(gameStateComp) {
    attrs {
        handler()
    }
}
