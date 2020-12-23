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

package com.dreamuth.student

import com.dreamuth.Topic
import kotlinx.html.ButtonType
import kotlinx.html.DIV
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import react.useState
import styled.StyledDOMBuilder
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledFieldSet
import styled.styledForm
import styled.styledInput
import styled.styledLabel
import styled.styledLegend

external interface StudentInfoProps: RProps {
    var studentName: String?
    var availableCategories: List<Topic>
    var selectedCategory: Topic
    var onCategoryChangeFunction: (Topic) -> Unit
    var onStartBtnClick: (String) -> Unit
}

private var studentInfo = functionalComponent<StudentInfoProps> { props ->
    var studentName by useState("")

    styledForm {
        css {
            classes = mutableListOf("col-lg-5 mx-auto")
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                if (props.studentName != null) {
                    props.onStartBtnClick(props.studentName!!)
                } else {
                    props.onStartBtnClick(studentName)
                }
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("form-group row")
            }
            styledLabel {
                css {
                    classes = mutableListOf("col-sm-4 col-form-label")
                    attrs {
                        attributes["for"] = "studentNameInput"
                    }
                }
                +"Student name"
            }
            styledDiv {
                css {
                    classes = mutableListOf("col-sm-8")
                }
                styledInput {
                    css {
                        classes = mutableListOf("form-control")
                        attrs {
                            id = "studentNameInput"
                            type = InputType.text
                            required = true
                            props.studentName?.let {
                                readonly = true
                                required = false
                            }
                        }
                    }
                    attrs {
                        onChangeFunction = {
                            val target = it.target as HTMLInputElement
                            studentName = target.value
                        }
                    }
                }
            }
        }
        styledFieldSet {
            css {
                classes = mutableListOf("form-group")
            }
            styledDiv {
                css {
                    classes = mutableListOf("row")
                }
                styledLegend {
                    css {
                        classes = mutableListOf("col-sm-4 col-form-label pt-0")
                    }
                    +"Question category"
                }
                styledDiv {
                    css {
                        classes = mutableListOf("form-check")
                    }
                    Topic.values().forEach {
                        customRadio(
                            it.tamilDisplay,
                            it == props.selectedCategory,
                            !props.availableCategories.contains(it),
                            props.onCategoryChangeFunction
                        )
                    }
                }
            }
        }
        styledButton {
            css {
                classes = mutableListOf("btn btn-primary btn-block rounded-pill")
                attrs {
                    type = ButtonType.submit
                }
            }
            +"Start"
        }
    }
}

private fun StyledDOMBuilder<DIV>.customRadio(
    radioName: String,
    isSelected: Boolean,
    isDisabled: Boolean,
    onCategoryChangeFunction: (Topic) -> Unit
) {
    styledDiv {
        css {
            classes = mutableListOf("custom-control custom-radio")
        }
        styledInput {
            css {
                classes = mutableListOf("custom-control-input")
                attrs {
                    type = InputType.radio
                    id = "questionCategory${radioName}Radio"
                    name = "questionCategoryRadio"
                    disabled = isDisabled
                    checked = isSelected
                }
            }
            attrs {
                onChangeFunction = {
                    val htmlInputElement = it.target as HTMLInputElement
                    onCategoryChangeFunction(Topic.getTopic(htmlInputElement.value))
                }
            }
        }
        styledLabel {
            css {
                classes = mutableListOf("custom-control-label")
                attrs {
                    attributes["for"] = "questionCategory${radioName}Radio"
                }
            }
            +radioName
        }
    }
}

fun RBuilder.studentInfo(handler: StudentInfoProps.() -> Unit) = child(studentInfo) {
    attrs {
        handler()
    }
}
