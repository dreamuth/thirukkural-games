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
import kotlinx.css.LinearDimension
import kotlinx.css.fontSize
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledDiv

external interface QuestionMultilineProps: RProps {
    var question: KuralOnly
    var questionSize: LinearDimension
}

class QuestionMultiline : RComponent<QuestionMultilineProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("card bg-warning m-2 text-center")
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-header")
                    fontSize = props.questionSize
                }
                styledDiv {
                    +props.question.firstLine
                }
                styledDiv {
                    +props.question.secondLine
                }
            }
        }
    }
}

fun RBuilder.questionMultiline(handler: QuestionMultilineProps.() -> Unit): ReactElement {
    return child(QuestionMultiline::class) {
        this.attrs(handler)
    }
}
