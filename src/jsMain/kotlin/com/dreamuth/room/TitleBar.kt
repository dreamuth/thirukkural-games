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
import com.dreamuth.components.dropdown
import kotlinx.css.LinearDimension
import kotlinx.css.width
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledDiv

external interface TitleBarProps: RProps {
    var persons: List<String>
    var selectedPerson: String
    var selectedTopic: Topic
    var showAnswer: Boolean
    var firstRowStyle: String
    var firstRowWidth: LinearDimension?
    var personButtonWidth: LinearDimension?
    var topicButtonWidth: LinearDimension?
    var secondOptionalRowStyle: String?
    var secondRowStyle: String
    var secondRowWidth: LinearDimension?
    var allKuralsWidth: LinearDimension?
    var navigationBtnWidth: LinearDimension
    var navigationWidth: LinearDimension
    var onPersonClick: (String) -> Unit
    var onTopicClick: (Topic) -> Unit
    var onFilterClick: (Int) -> Unit
    var onPreviousClick: () -> Unit
    var onResetClick: () -> Unit
    var onNextClick: () -> Unit
    var onTimerClick: () -> Unit
    var onRetryClick: () -> Unit
}

class TitleBar : RComponent<TitleBarProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("row m-2")
            }
            styledDiv {
                css {
                    classes = mutableListOf(props.firstRowStyle)
                    props.firstRowWidth?.let { width = it }
                }
                styledDiv {
                    css {
                        classes = mutableListOf("btn-group")
                        props.topicButtonWidth?.let { width = it }
                    }
                    dropdown {
                        id = "topicDropDown"
                        names = listOf(
                            listOf(
                                Topic.Athikaram.tamil,
                                Topic.FirstWord.tamil,
                                Topic.LastWord.tamil,
                                Topic.KuralPorul.tamil,
                                Topic.Kural.tamil
                            )
                        )
                        selectedName = props.selectedTopic.tamil
                        onDropdownClick = { _, name ->
                            props.onTopicClick(Topic.getTopic(name))
                        }
                    }
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf(props.secondRowStyle)
                    props.secondRowWidth?.let { width = it }
                }
                navigation {
                    buttonSize = props.navigationWidth
                    smallBtnWidth = props.navigationBtnWidth
//                    timer = props.timer
//                    onShowAnswerClick = {
//                        props.onShowAnswerClick(!props.showAnswer)
//                    }
                    onPreviousClick = {
//                        props.onShowAnswerClick(false)
                        props.onPreviousClick()
                    }
                    onResetClick = {
//                        props.onShowAnswerClick(false)
                        props.onResetClick()
                    }
                    onNextClick = {
//                        props.onShowAnswerClick(false)
                        props.onNextClick()
                    }
                    onTimerClick = {
                        props.onTimerClick()
                    }
                }
            }
        }
    }
}

fun RBuilder.titleBar(handler: TitleBarProps.() -> Unit): ReactElement {
    return child(TitleBar::class) {
        this.attrs(handler)
    }
}
