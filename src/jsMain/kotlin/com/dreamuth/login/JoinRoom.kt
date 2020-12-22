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

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onFormChangeFunction
import kotlinx.html.js.onSelectFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.RProps
import react.child
import react.dom.option
import react.functionalComponent
import react.useState
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledFieldSet
import styled.styledForm
import styled.styledInput
import styled.styledLabel
import styled.styledSelect
import styled.styledSmall

external interface JoinRoomProps: RProps {
    var roomNames: List<String>
    var roomName: String?
    var errorMsg: String?
    var onRoomNameChange: (String) -> Unit
    var onAdminJoinBtnClick: (String, String) -> Unit
    var onGuestJoinBtnClick: (String, String) -> Unit
}

private var joinRoom = functionalComponent<JoinRoomProps> { props ->
    var passcode by useState("")

    styledForm {
        css {
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                props.roomName?.let { roomName ->
                    if (passcode.length == 8) {
                        props.onAdminJoinBtnClick(roomName, passcode)
                    } else {
                        props.onGuestJoinBtnClick(roomName, passcode)
                    }
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
        if (props.roomNames.isEmpty()) {
            styledDiv {
                css {
                    classes = mutableListOf("alert alert-warning")
                }
                +"There is no active room at this time!"
            }
        }
        styledFieldSet {
            css {
                attrs {
                    disabled = props.roomNames.isEmpty()
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("form-group")
                }
                styledLabel {
                    +"Room name"
                }
                styledSelect {
                    css {
                        classes = mutableListOf("form-control custom-select")
                        attrs {
                            id = "selectRoom1"
                            props.roomName?.let { roomName ->
                                value = roomName
                            }
                        }
                    }
                    props.roomNames.forEach { roomName ->
                        option { +roomName }
                    }
                    attrs {
                        onChangeFunction = {
                            val target = it.target as HTMLSelectElement
                            props.onRoomNameChange(target.value)
                        }
                    }
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("form-group")
                }
                styledLabel {
                    +"Room Passcode"
                }
                styledInput {
                    css {
                        classes = mutableListOf("form-control")
                        attrs {
                            type = InputType.password
                            name = "roomPasscode"
                            required = true
                        }
                    }
                    attrs {
                        onChangeFunction = {
                            val target = it.target as HTMLInputElement
                            passcode = target.value
                        }
                    }
                }
            }
            styledButton {
                css {
                    classes = mutableListOf("btn btn-primary btn-block rounded-pill")
                    attrs {
                        type = ButtonType.submit
                    }
                }
                +"Join"
            }
        }
    }
}

fun RBuilder.joinRoom(handler: JoinRoomProps.() -> Unit) = child(joinRoom) {
    attrs {
        handler()
    }
}
