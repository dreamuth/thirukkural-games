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

package com.dreamuth.room

import kotlinx.css.LinearDimension
import kotlinx.css.height
import kotlinx.css.px
import kotlinx.css.width
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledImg
import styled.styledSpan

external interface NavigationProps: RProps {
    var buttonSize: LinearDimension
    var smallBtnWidth: LinearDimension
//    var timer: Timer
    var onShowAnswerClick: () -> Unit
    var onTimerClick: () -> Unit
    var onPreviousClick: () -> Unit
    var onResetClick: () -> Unit
    var onNextClick: () -> Unit
}

class Navigation : RComponent<NavigationProps, RState>() {
    override fun RBuilder.render() {
//        styledButton {
//            css {
//                classes = mutableListOf("btn btn-success mr-2")
//                width = props.buttonSize
//            }
//            if (props.timer.show) +"${props.timer.time / 60 % 60} : ${props.timer.time % 60} " else +"நேரம் "
//            attrs {
//                onClickFunction = {
//                    props.onTimerClick()
//                }
//            }
//            if (props.timer.show) {
//                styledSpan {
//                    css {
//                        classes = mutableListOf("badge badge-light")
//                    }
//                    +"${props.timer.count}"
//                }
//            }
//        }
        styledDiv {
            styledButton {
                css {
                    classes = mutableListOf("btn btn-success me-2 p-0")
                    width = props.buttonSize
                }
                attrs {
                    onClickFunction = {
                        props.onPreviousClick()
                    }
                }
                +"முன்பு"
//            styledImg {
//                attrs.src = "back.svg"
//            }
            }
            styledButton {
                css {
                    classes = mutableListOf("btn btn-danger me-2")
                    width = props.buttonSize
                }
                +"பதில்"
                attrs {
                    onClickFunction = {
                        props.onShowAnswerClick()
                    }
                }
            }
            styledButton {
                css {
                    classes = mutableListOf("btn btn-success p-0")
                    width = props.buttonSize
                }
                attrs {
                    onClickFunction = {
                        props.onNextClick()
                    }
                }
                +"அடுத்து"
            }
        }
    }
}

fun RBuilder.navigation(handler: NavigationProps.() -> Unit): ReactElement {
    return child(Navigation::class) {
        this.attrs(handler)
    }
}
