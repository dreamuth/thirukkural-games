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
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import react.RBuilder
import react.RProps
import react.child
import react.dom.option
import react.functionalComponent
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
    var onJoinBtnClick: () -> Unit
}

private var joinRoom = functionalComponent<JoinRoomProps> { props ->
    styledForm {
        css {
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                props.onJoinBtnClick()
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
                        }
                    }
                    props.roomNames.forEach { roomName ->
                        option {
                            +roomName
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
                }
                styledSmall {
                    css {
                        classes = mutableListOf("form-text text-muted")
                    }
//                +"Room admin might have provided the admin or guest passcode"
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
