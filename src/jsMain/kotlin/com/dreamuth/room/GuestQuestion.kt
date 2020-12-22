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

import com.dreamuth.Topic
import kotlinx.css.fontSize
import kotlinx.css.rem
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledDiv
import styled.styledP

external interface GuestQuestionProps: RProps {
    var topic: Topic
    var question: String
    var question2: String?
}

class GuestQuestion : RComponent<GuestQuestionProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("container-md")
            }
            styledDiv {
                css {
                    classes = mutableListOf("card bg-success text-white m-2 text-center")
                }
                styledDiv {
                    css {
                        classes = mutableListOf("card-header")
                        fontSize = 1.5.rem
                    }
                    +props.topic.tamil
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("card bg-warning text-dark m-2 text-center")
                }
                styledDiv {
                    css {
                        classes = mutableListOf("card-body")
                    }
                    styledP {
                        css {
                            classes = mutableListOf("card-text")
                            fontSize = 2.rem
                        }
                        +props.question
                    }
                    props.question2?.let { question2 ->
                        styledP {
                            css {
                                classes = mutableListOf("card-text")
                                fontSize = 2.rem
                            }
                            +question2
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.guestQuestion(handler: GuestQuestionProps.() -> Unit): ReactElement {
    return child(GuestQuestion::class) {
        this.attrs(handler)
    }
}
