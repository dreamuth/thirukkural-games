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

import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.color
import kotlinx.css.height
import kotlinx.css.px
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledButton
import styled.styledHeader
import styled.styledLabel

external interface HeaderProps: RProps {
    var activeState: GameState
    var onSignOutBtnClick: () -> Unit
}

private var header = functionalComponent<HeaderProps> { props ->
    styledHeader {
        css {
            classes = mutableListOf("d-flex justify-content-between align-items-center")
            backgroundColor = Color("#563d7c")
            height = 50.px
        }
        styledLabel {
            css {
                classes = mutableListOf("m-2")
                color = Color.white
            }
            +"திருக்குறள் விளையாட்டு"
        }
        if (props.activeState != GameState.NONE) {
            styledButton {
                css {
                    classes = mutableListOf("btn btn-outline-warning mr-2")
                }
                attrs {
                    onClickFunction = {
                        props.onSignOutBtnClick()
                    }
                }
                val text = when (props.activeState) {
                    GameState.PRACTICE, GameState.ADMIN_ROOM -> "வெளியேறு"
                    else -> "முன்பு"
                }
                +text
            }
        }
    }

}

fun RBuilder.header(handler: HeaderProps.() -> Unit) = child(header) {
    attrs {
        handler()
    }
}
