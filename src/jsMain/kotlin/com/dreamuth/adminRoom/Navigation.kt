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

package com.dreamuth.adminRoom

import com.dreamuth.AdminQuestion
import com.dreamuth.ServerCommand
import com.dreamuth.TimerState
import com.dreamuth.scope
import com.dreamuth.wsClient
import kotlinx.coroutines.launch
import kotlinx.css.LinearDimension
import kotlinx.css.px
import kotlinx.css.width
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledButton
import styled.styledDiv

external interface NavigationProps: RProps {
    var buttonSize: LinearDimension
    var timerState: TimerState
    var adminQuestion: AdminQuestion
}

class Navigation : RComponent<NavigationProps, RState>() {
    override fun RBuilder.render() {
        styledButton {
            val activeStyle = if (props.timerState.isLive) "active" else ""
            css {
                classes = mutableListOf("btn btn-primary mr-2 $activeStyle")
                width = props.buttonSize
                attrs {
                    disabled = props.timerState.time <= 0
                }
            }
            if (props.timerState.isLive) +"${props.timerState.time / 60 % 60} : ${props.timerState.time % 60} " else +"தொடங்கு"
            attrs {
                onClickFunction = {
                    if (!props.timerState.isLive) {
                        scope.launch {
                            wsClient.trySend(ServerCommand.START_GAME)
                        }
                    }
                }
            }
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-success mr-2")
                width = props.buttonSize
                attrs {
                    disabled = props.timerState.time <= 0 || !props.timerState.isLive
                }
            }
            attrs {
                onClickFunction = {
                    scope.launch {
                        wsClient.trySend(ServerCommand.PREVIOUS)
                    }
                }
            }
            +"முன்பு"
        }
        styledDiv {
            css {
                classes = mutableListOf("btn-group")
                attrs {
                    role = "group"
                }
            }
            styledButton {
                css {
                    val selectedStyle = if (props.adminQuestion.answered) "" else "active"
                    classes = mutableListOf("btn btn-outline-success $selectedStyle")
                    width = 80.px
                    attrs {
                        disabled = !props.timerState.isLive && (props.timerState.time == 31L)
                    }
                }
                attrs {
                    onClickFunction = {
                        if (props.adminQuestion.thirukkurals.isNotEmpty()) {
                            var question = props.adminQuestion.question
                            props.adminQuestion.question2?.let {
                                question += it
                            }
                            scope.launch {
                                wsClient.trySend(ServerCommand.WRONG_ANSWER.name + question)
                            }
                        }
                    }
                }
                +"தவறு"
            }
            styledButton {
                css {
                    val selectedStyle = if (props.adminQuestion.answered) "active" else ""
                    classes = mutableListOf("btn btn-outline-success mr-2 $selectedStyle")
                    width = 80.px
                    attrs {
                        disabled = !props.timerState.isLive && (props.timerState.time == 31L)
                    }
                }
                attrs {
                    onClickFunction = {
                        if (props.adminQuestion.thirukkurals.isNotEmpty()) {
                            var question = props.adminQuestion.question
                            props.adminQuestion.question2?.let {
                                question += it
                            }
                            scope.launch {
                                wsClient.trySend(ServerCommand.RIGHT_ANSWER.name + question)
                            }
                        }
                    }
                }
                +"சரி"
            }
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-success")
                width = props.buttonSize
                attrs {
                    disabled = props.timerState.time <= 0 || !props.timerState.isLive
                }
            }
            attrs {
                onClickFunction = {
                    scope.launch {
                        wsClient.trySend(ServerCommand.NEXT)
                    }
                }
            }
            +"அடுத்து"
        }
    }
}

fun RBuilder.navigation(handler: NavigationProps.() -> Unit): ReactElement {
    return child(Navigation::class) {
        this.attrs(handler)
    }
}
