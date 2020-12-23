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

import com.dreamuth.Group
import com.dreamuth.Room
import com.dreamuth.ServerCommand
import com.dreamuth.components.linkItem
import com.dreamuth.scope
import com.dreamuth.wsClient
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import react.useState
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledForm
import styled.styledInput
import styled.styledLabel
import styled.styledSmall
import styled.styledUl

external interface CreateRoomProps: RProps {
    var errorMsg: String?
}

private var createRoom = functionalComponent<CreateRoomProps> { props ->
    var roomName by useState("")
    var group by useState(Group.TWO)

    styledForm {
        css {
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                scope.launch {
                    val data = Json.encodeToString(Room(roomName.trim(), group))
                    wsClient.trySend(ServerCommand.CREATE_ROOM.name + data)
                }
            }
        }
        props.errorMsg?.let { errorMsg ->
            styledDiv {
                css {
                    classes = mutableListOf("alert alert-danger")
                    attrs {
                        role = "alert"
                    }
                }
                +errorMsg
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("form-group")
            }
            styledLabel {
                +"Admin password"
            }
            styledInput {
                css {
                    classes = mutableListOf("form-control")
                    attrs {
                        type = InputType.text
                        name = "adminPassword"
                        value = "HTS-Kids-2021"
                        readonly = true
//                        required = true
                    }
                }
//                attrs {
//                    onChangeFunction = {
//                        val target = it.target as HTMLInputElement
//                        roomName = target.value
//                    }
//                }
            }
            styledSmall {
                css {
                    classes = mutableListOf("form-text text-muted")
                }
                +"Password is hardcoded for demo purpose, before game it will be removed"
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("form-group")
            }
            styledUl {
                css {
                    classes = mutableListOf("nav bg-light nav-pills rounded-pill nav-fill mb-3")
                    attrs {
                        role = "tablist"
                    }
                }
                linkItem {
                    name = "Group II"
                    isActive = group == Group.TWO
                    onClickFunction = {
                        group = Group.TWO
                    }
                }
                linkItem {
                    name = "Group III"
                    isActive = group == Group.THREE
                    onClickFunction = {
                        group = Group.THREE
                    }
                }
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("form-group")
            }
            styledLabel {
                +"Room name"
            }
            styledInput {
                css {
                    classes = mutableListOf("form-control")
                    attrs {
                        type = InputType.text
                        name = "roomName"
                        required = true
                        pattern = "^[a-zA-Z0-9 ]+$"
                    }
                }
                attrs {
                    onChangeFunction = {
                        val target = it.target as HTMLInputElement
                        roomName = target.value
                    }
                }
            }
            styledSmall {
                css {
                    classes = mutableListOf("form-text text-muted")
                }
                +"Room name can contain letters, numbers and space only"
            }
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-primary btn-block rounded-pill")
                attrs {
                    type = ButtonType.submit
                }
            }
            +"Create"
        }
    }
}

fun RBuilder.createRoom(handler: CreateRoomProps.() -> Unit) = child(createRoom) {
    attrs {
        handler()
    }
}
