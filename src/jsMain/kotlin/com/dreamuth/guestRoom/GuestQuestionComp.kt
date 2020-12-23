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

package com.dreamuth.guestRoom

import com.dreamuth.GuestQuestion
import com.dreamuth.TimerState
import kotlinx.css.fontSize
import kotlinx.css.pct
import kotlinx.css.rem
import kotlinx.css.width
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledDiv
import styled.styledImg
import styled.styledP

external interface GuestQuestionProps: RProps {
    var guestQuestion: GuestQuestion
    var timerState: TimerState
}

class GuestQuestionComp : RComponent<GuestQuestionProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("container-md")
            }
            styledDiv {
                css {
                    classes = mutableListOf("row")
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-9 pr-0")
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
                            +props.guestQuestion.topic.tamilDisplay
                        }
                    }
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-3 pl-0")
                    }
                    styledDiv {
                        css {
                            val style = if (props.timerState.isLive && props.timerState.time == 0L) "danger" else "success"
                            classes = mutableListOf("card bg-$style text-white m-2 text-center")
                        }
                        styledDiv {
                            css {
                                classes = mutableListOf("card-header")
                                fontSize = 1.5.rem
                            }
                            if (props.timerState.isLive) +"${props.timerState.time / 60 % 60} : ${props.timerState.time % 60} " else +"Time"
                        }
                    }
                }
            }
            if (props.timerState.isLive) {
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
                            +props.guestQuestion.question
                        }
                        props.guestQuestion.question2?.let { question2 ->
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
            } else {
                styledDiv {
                    css {
                        classes = mutableListOf("m-1")
                    }
                    styledImg {
                        attrs.src = "img/thiruvalluvar.jpg"
                        css {
                            classes = mutableListOf("")
                            width = 100.pct
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.guestQuestionComp(handler: GuestQuestionProps.() -> Unit): ReactElement {
    return child(GuestQuestionComp::class) {
        this.attrs(handler)
    }
}
