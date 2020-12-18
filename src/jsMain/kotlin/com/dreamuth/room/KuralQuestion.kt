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
import kotlinx.css.LinearDimension
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement

external interface KuralQuestionProps: RProps {
    var question: KuralOnly
    var questionSize: LinearDimension
    var thirukkurals: List<Thirukkural>
    var showAnswer: Boolean
}

class KuralQuestion : RComponent<KuralQuestionProps, RState>() {
    override fun RBuilder.render() {
        questionMultiline {
            question = props.question
            questionSize = props.questionSize
        }
        if (props.showAnswer) {
            props.thirukkurals.forEach { thirukkural ->
                kuralDisplay {
                    selectedThirukkural = thirukkural
                    showPorul = true
                }
            }
        }
    }
}

fun RBuilder.kuralQuestion(handler: KuralQuestionProps.() -> Unit): ReactElement {
    return child(KuralQuestion::class) {
        this.attrs(handler)
    }
}
