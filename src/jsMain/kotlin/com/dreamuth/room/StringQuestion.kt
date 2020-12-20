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

import com.dreamuth.Thirukkural
import kotlinx.css.LinearDimension
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement

external interface StringQuestionProps: RProps {
    var question: String
    var questionSize: LinearDimension
    var thirukkurals: List<Thirukkural>
    var showAnswer: Boolean
}

class StringQuestion : RComponent<StringQuestionProps, RState>() {
    override fun RBuilder.render() {
        question {
            question = props.question
            questionSize = props.questionSize
        }
        if (props.showAnswer) {
            props.thirukkurals.forEach { thirukkural ->
                kuralDisplay {
                    selectedThirukkural = thirukkural
                }
            }
        }
    }
}

fun RBuilder.stringQuestion(handler: StringQuestionProps.() -> Unit): ReactElement {
    return child(StringQuestion::class) {
        this.attrs(handler)
    }
}
