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
import com.dreamuth.Topic
import kotlinx.css.px
import kotlinx.css.rem
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledDiv

external interface AdminRoomProps: RProps {
    var topic: Topic
    var question: String
    var question2: String?
    var thirukkurals: List<Thirukkural>
    var onTopicClick: (Topic) -> Unit
    var onPreviousClick: () -> Unit
    var onNextClick: () -> Unit
}


private var adminRoom = functionalComponent<AdminRoomProps> { props ->
    styledDiv {
        // Desktop
        css {
//            classes = mutableListOf("d-none d-lg-block")
        }
        titleBar {
            selectedTopic = props.topic
            firstRowStyle = "col pl-0 pr-0"
            topicButtonWidth = 200.px
            secondRowStyle = "col-md-auto pr-0"
            navigationWidth = 120.px
            navigationBtnWidth = 120.px
            onTopicClick = { it -> props.onTopicClick(it) }
            onPreviousClick = props.onPreviousClick
            onNextClick = { props.onNextClick() }
        }
    }

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
}

fun RBuilder.adminRoom(handler: AdminRoomProps.() -> Unit) = child(adminRoom) {
    attrs {
        handler()
    }
}
