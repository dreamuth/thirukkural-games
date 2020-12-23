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

import com.dreamuth.StudentScore
import kotlinx.html.DIV
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.StyledDOMBuilder
import styled.css
import styled.styledDiv
import styled.styledP

external interface ScoreInfoProps: RProps {
    var studentScore: StudentScore
}

class ScoreInfo : RComponent<ScoreInfoProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("card text-white bg-dark m-2")
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-body p-2")
                }
                for (entry in props.studentScore.score) {

                    keyValue(entry.key.tamilDisplay, entry.value.toString())
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-footer p-2")
                }
                keyValue("மொத்தம்", props.studentScore.score.values.sum().toString())
            }
        }
    }

    private fun StyledDOMBuilder<DIV>.keyValue(key: String, value: String) {
        styledDiv {
            css {
                classes = mutableListOf("d-flex justify-content-between align-items-center")
            }
            styledP {
                css {
                    classes = mutableListOf("card-text mb-0")
                }
                +"$key: "
            }
            styledP {
                css {
                    classes = mutableListOf("card-text")
                }
                +value
            }
        }
    }
}

fun RBuilder.scoreInfo(handler: ScoreInfoProps.() -> Unit): ReactElement {
    return child(ScoreInfo::class) {
        this.attrs(handler)
    }
}
