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

import com.dreamuth.GameState
import com.dreamuth.components.linkItem
import kotlinx.html.role
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import react.useState
import styled.css
import styled.styledDiv
import styled.styledImg
import styled.styledUl

external interface GameModeProps: RProps {
    var roomNames: List<String>
    var createRoomErrorMsg: String?
    var joinRoomErrorMsg: String?
}

private var gameMode = functionalComponent<GameModeProps> { props ->
    var loginState by useState(GameState.CREATE)

    styledDiv {
        css {
            classes = mutableListOf("container py-5")
        }
        styledDiv {
            css {
                classes = mutableListOf("row")
            }
            styledDiv {
                css {
                    classes = mutableListOf("col-sm-10 col-md-8 col-lg-7 col-xl-6 mx-auto")
                }
                styledDiv {
                    css {
                        classes = mutableListOf("bg-white rounded-lg shadow-sm p-5")
                    }
                    styledDiv {
                        css {
                            classes = mutableListOf("d-flex justify-content-center align-items-center pb-5")
                        }
                        styledImg {
                            attrs.src = "img/sangam_logo.png"
                        }
                    }
                    styledUl {
                        css {
                            classes = mutableListOf("nav bg-light nav-pills rounded-pill nav-fill mb-3")
                            attrs {
                                role = "tablist"
                            }
                        }
                        linkItem {
                            name = "Create Room"
                            isActive = loginState == GameState.CREATE
                            onClickFunction = {
                                loginState = GameState.CREATE
                            }
                        }
                        linkItem {
                            name = "Join Room"
                            isActive = loginState == GameState.JOIN
                            onClickFunction = {
                                loginState = GameState.JOIN
                            }
                        }
                    }
                    styledDiv {
                        css {
                            classes = mutableListOf("tab-content")
                        }
                        when (loginState) {
                            GameState.CREATE -> {
                                createRoom {
                                    errorMsg = props.createRoomErrorMsg
                                }
                            }
                            GameState.JOIN -> {
                                joinRoom {
                                    roomNames = props.roomNames
                                    errorMsg = props.joinRoomErrorMsg
                                }
                            }
                            else -> {
                                println("Error state...")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.gameMode(handler: GameModeProps.() -> Unit) = child(gameMode) {
    attrs {
        handler()
    }
}
