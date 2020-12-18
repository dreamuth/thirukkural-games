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

import kotlinx.css.height
import kotlinx.css.minWidth
import kotlinx.css.pct
import kotlinx.css.px
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledButton
import styled.styledDiv

external interface GameModeProps: RProps {
    var onGameBtnClick: () -> Unit
    var onPracticeBtnClick: () -> Unit
}

private var gameMode = functionalComponent<GameModeProps> { props ->
    styledDiv {
        css {
            classes = mutableListOf("d-flex justify-content-center align-items-center flex-column")
            height = 100.pct
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-primary btn-lg m-2")
                minWidth = 200.px
            }
            attrs {
                onClickFunction = {
                    props.onGameBtnClick()
                }
            }
            +"Game"
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-primary btn-lg m-2")
                minWidth = 200.px
            }
            attrs {
                onClickFunction = {
                    props.onPracticeBtnClick()
                }
            }
            +"Practice"
        }
    }
}

fun RBuilder.gameMode(handler: GameModeProps.() -> Unit) = child(gameMode) {
    attrs {
        handler()
    }
}