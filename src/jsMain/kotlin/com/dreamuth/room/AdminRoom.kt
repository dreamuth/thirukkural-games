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

import com.dreamuth.KuralOnly
import com.dreamuth.Thirukkural
import com.dreamuth.TimerState
import com.dreamuth.Topic
import kotlinx.css.px
import kotlinx.css.rem
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv
import styled.styledImg

external interface AdminRoomProps: RProps {
    var topic: Topic
    var timerState: TimerState
    var question: String
    var question2: String?
    var thirukkurals: List<Thirukkural>
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
                selectedTopic = props.topic
                timerState = props.timerState
                firstRowStyle = "col pl-0 pr-0"
                topicButtonWidth = 200.px
                secondRowStyle = "col-md-auto pr-0"
                navigationWidth = 120.px
                navigationBtnWidth = 120.px
            }
        }
        if (props.timerState.isLive) {
            when (props.topic) {
                Topic.Kural -> {
                    kuralQuestion {
                        question = KuralOnly(props.question, props.question2!!)
                        questionSize = 2.rem
                        thirukkurals = props.thirukkurals
                    }
                }
                else -> {
                    stringQuestion {
                        question = props.question
                        questionSize = 2.rem
                        thirukkurals = props.thirukkurals
                    }
                }
            }
        } else {
            styledDiv {
                css {
                    classes = mutableListOf("d-flex justify-content-center align-items-center pb-5")
                }
                styledImg {
                    attrs.src = "img/thiruvalluvar.jpg"
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
