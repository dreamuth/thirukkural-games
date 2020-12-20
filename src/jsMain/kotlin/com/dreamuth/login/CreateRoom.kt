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
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledForm
import styled.styledInput
import styled.styledLabel
import styled.styledSmall

external interface CreateRoomProps: RProps {
    var onCreateBtnClick: () -> Unit
}

private var createRoom = functionalComponent<CreateRoomProps> { props ->
    styledForm {
        css {
            attrs {
                role = "form"
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
            }
            styledSmall {
                css {
                    classes = mutableListOf("form-text text-muted")
                }
                +"Your room name can contain letters, numbers and space only"
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
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                props.onCreateBtnClick()
            }
        }
    }
}

fun RBuilder.createRoom(handler: CreateRoomProps.() -> Unit) = child(createRoom) {
    attrs {
        handler()
    }
}
