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

package com.dreamuth.adminRoom

import com.dreamuth.AdminQuestion
import com.dreamuth.KuralOnly
import com.dreamuth.TimerState
import com.dreamuth.Topic
import com.dreamuth.TopicState
import kotlinx.css.pct
import kotlinx.css.px
import kotlinx.css.rem
import kotlinx.css.width
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv
import styled.styledImg

external interface AdminRoomProps: RProps {
    var topicState: TopicState
    var timerState: TimerState
    var adminQuestion: AdminQuestion
}

private var adminRoom = functionalComponent<AdminRoomProps> { props ->
    styledDiv {
        css {
            classes = mutableListOf("")
        }
        styledDiv {
            // Desktop
            css {
//            classes = mutableListOf("d-none d-lg-block")
            }
            titleBar {
                timerState = props.timerState
                topicState = props.topicState
                adminQuestion = props.adminQuestion
                firstRowStyle = "col pl-0 pr-0"
                topicButtonWidth = 200.px
                secondRowStyle = "col-md-auto pr-0"
                navigationWidth = 120.px
                navigationBtnWidth = 120.px
            }
        }
        if (props.timerState.isLive) {
            when (props.adminQuestion.topic) {
                Topic.Kural -> {
                    kuralQuestion {
                        question = KuralOnly(props.adminQuestion.question, props.adminQuestion.question2!!)
                        questionSize = 2.rem
                        thirukkurals = props.adminQuestion.thirukkurals
                    }
                }
                else -> {
                    stringQuestion {
                        question = props.adminQuestion.question
                        questionSize = 2.rem
                        thirukkurals = props.adminQuestion.thirukkurals
                    }
                }
            }
        } else {
            styledDiv {
                css {
                    classes = mutableListOf("m-2")
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

fun RBuilder.adminRoom(handler: AdminRoomProps.() -> Unit) = child(adminRoom) {
    attrs {
        handler()
    }
}
