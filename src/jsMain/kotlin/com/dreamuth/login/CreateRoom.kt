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

package com.dreamuth.login

import com.dreamuth.Group
import com.dreamuth.Room
import com.dreamuth.School
import com.dreamuth.ServerCommand
import com.dreamuth.StudentInfo
import com.dreamuth.components.linkItem
import com.dreamuth.scope
import com.dreamuth.wsClient
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.RProps
import react.child
import react.dom.option
import react.functionalComponent
import react.useState
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledFieldSet
import styled.styledForm
import styled.styledInput
import styled.styledLabel
import styled.styledSelect
import styled.styledSmall
import styled.styledUl

external interface CreateRoomProps: RProps {
    var activeStudents: List<StudentInfo>
    var errorMsg: String?
}

private var createRoom = functionalComponent<CreateRoomProps> { props ->
    var school by useState(School.PEARLAND)
    var group by useState(Group.II)
    var studentName:String? by useState(null)

    val filteredStudents = props.activeStudents.filter { it.school == school && it.group == group }
    styledForm {
        css {
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = { event ->
                event.preventDefault()
                val selectedName = studentName ?: filteredStudents.firstOrNull()?.name
                selectedName?.let { validName ->
                    scope.launch {
                        val data = Json.encodeToString(Room(validName, school, group))
                        wsClient.trySend(ServerCommand.CREATE_ROOM.name + data)
                    }
                }
            }
        }
        props.errorMsg?.let { errorMsg ->
            styledDiv {
                css {
                    classes = mutableListOf("alert alert-danger")
                    attrs {
                        role = "alert"
                    }
                }
                +errorMsg
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("form-group row")
            }
            styledLabel {
                css {
                    classes = mutableListOf("col-sm-4 col-form-label")
                }
                +"Admin password"
            }
            styledDiv {
                css {
                    classes = mutableListOf("col-sm-8")
                }
                styledInput {
                    css {
                        classes = mutableListOf("form-control")
                        attrs {
                            type = InputType.text
                            name = "adminPassword"
                            value = "HTS-Kids-2021"
                            readonly = true
//                        required = true
                        }
                    }
//                attrs {
//                    onChangeFunction = {
//                        val target = it.target as HTMLInputElement
//                        roomName = target.value
//                    }
//                }
                }
                styledSmall {
                    css {
                        classes = mutableListOf("form-text text-muted")
                    }
                    +"Password is hardcoded for demo purpose, before game it will be removed"
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
                }
                +"School"
            }
            styledDiv {
                css {
                    classes = mutableListOf("col-sm-8")
                }
                styledSelect {
                    css {
                        classes = mutableListOf("form-control custom-select")
                        attrs {
                            id = "selectSchool1"
                        }
                    }
                    School.values().forEach { school ->
                        option { +school.englishDisplay }
                    }
                    attrs {
                        onChangeFunction = { event ->
                            val target = event.target as HTMLSelectElement
                            school = School.getSchoolForEnglish(target.value)
                        }
                    }
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
                }
                +"Age group"
            }
            styledDiv {
                css {
                    classes = mutableListOf("col-sm-8")
                }
                styledUl {
                    css {
                        classes = mutableListOf("nav bg-light nav-pills rounded-pill nav-fill mb-3")
                        attrs {
                            role = "tablist"
                        }
                    }
                    linkItem {
                        name = Group.II.englishDisplay
                        isActive = group == Group.II
                        onClickFunction = {
                            group = Group.II
                        }
                    }
                    linkItem {
                        name = Group.III.englishDisplay
                        isActive = group == Group.III
                        onClickFunction = {
                            group = Group.III
                        }
                    }
                }
            }
        }
        styledFieldSet {
            css {
                attrs {
                    disabled = filteredStudents.isEmpty()
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("form-group row")
                }
                styledLabel {
                    css {
                        classes = mutableListOf("col-sm-4")
                    }
                    +"Student"
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-sm-8")
                    }
                    styledSelect {
                        css {
                            classes = mutableListOf("form-control custom-select")
                            attrs {
                                id = "selectStudentName"
                            }
                        }
                        filteredStudents.forEach { student ->
                            option { +student.name }
                        }
                        attrs {
                            onChangeFunction = {
                                val target = it.target as HTMLSelectElement
                                studentName = target.value
                            }
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
                +"Create"
            }
        }
    }
}

fun RBuilder.createRoom(handler: CreateRoomProps.() -> Unit) = child(createRoom) {
    attrs {
        handler()
    }
}
