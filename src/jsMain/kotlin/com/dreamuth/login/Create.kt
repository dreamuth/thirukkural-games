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

import kotlinx.css.minWidth
import kotlinx.css.pct
import kotlinx.css.px
import kotlinx.css.width
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledInput
import styled.styledLabel

external interface CreateProps: RProps {
    var onCreateBtnClick: () -> Unit
}

private var create = functionalComponent<CreateProps> { props ->
    styledDiv {
        css {
            classes = mutableListOf("form-floating mb-3")
//            height = 100.pct
        }
        styledInput {
            css {
                classes = mutableListOf("form-control")
                width = 50.pct
            }
            attrs {
                id = "createRoomNameInput"
                type = InputType.text
                placeholder = "game room 1"
            }
        }
        styledLabel {
            +"Room name"
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-primary btn-lg m-2")
                minWidth = 200.px
            }
            attrs {
                onClickFunction = {
                    props.onCreateBtnClick()
                }
            }
            +"Create"
        }
    }
}

fun RBuilder.create(handler: CreateProps.() -> Unit) = child(create) {
    attrs {
        handler()
    }
}
