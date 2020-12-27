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

import com.dreamuth.ActiveStudents
import com.dreamuth.ActiveUsers
import com.dreamuth.AdminQuestion
import com.dreamuth.GameState
import com.dreamuth.GuestQuestion
import com.dreamuth.StudentScore
import com.dreamuth.TimerState
import com.dreamuth.TopicState
import com.dreamuth.adminRoom.adminRoom
import com.dreamuth.adminRoom.roomInfo
import com.dreamuth.adminRoom.scoreInfo
import com.dreamuth.guestRoom.guestQuestionComp
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv

external interface GameStateCompProps: RProps {
    var gameState: GameState
    var topicState: TopicState
    var roomNames: List<String>
    var roomName: String?
    var activeStudents: ActiveStudents
    var timerState: TimerState
    var adminQuestion: AdminQuestion
    var guestQuestion: GuestQuestion
    var studentScore: StudentScore
    var adminPasscode: String?
    var guestPasscode: String?
    var activeUsers: ActiveUsers
    var createRoomErrorMsg: String?
    var joinRoomErrorMsg: String?
    var onNoClickHandler: () -> Unit
}

private var gameStateComp = functionalComponent<GameStateCompProps> { props ->
    when (props.gameState) {
        GameState.NONE -> {
            gameMode {
                roomNames = props.roomNames
                activeStudents = props.activeStudents
                createRoomErrorMsg = props.createRoomErrorMsg
                joinRoomErrorMsg = props.joinRoomErrorMsg
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
                        timerState = props.timerState
                        topicState = props.topicState
                        adminQuestion = props.adminQuestion
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
                            activeUsers = props.activeUsers
                        }
                    }
                    scoreInfo {
                        studentScore = props.studentScore
                    }
                }
            }
        }
        GameState.GUEST_ROOM -> {
            guestQuestionComp {
                guestQuestion = props.guestQuestion
                timerState = props.timerState
            }
        }
        GameState.SIGN_OUT_CONFIRM -> {
            signOutConfirm {
                isAdmin = props.adminPasscode != null
                onNoClickHandler = props.onNoClickHandler
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
