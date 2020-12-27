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

import com.dreamuth.AdminJoinRoom
import com.dreamuth.Group
import com.dreamuth.GuestJoinRoom
import com.dreamuth.School
import com.dreamuth.ServerCommand
import com.dreamuth.StudentInfo
import com.dreamuth.Students
import com.dreamuth.components.linkItem
import com.dreamuth.external.ReactSelectOption
import com.dreamuth.external.reactSelect
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
import org.w3c.dom.HTMLInputElement
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
import styled.styledUl

external interface JoinRoomProps: RProps {
    var activeStudents: Students
    var errorMsg: String?
}

private var joinRoom = functionalComponent<JoinRoomProps> { props ->
    var school by useState(School.PEARLAND)
    var group by useState(Group.II)
    var studentName:String? by useState(null)
    var passcode by useState("")

    val filteredStudents = props.activeStudents.students.filter { it.school == school && it.group == group }

    styledForm {
        css {
            attrs {
                role = "form"
            }
        }
        attrs {
            onSubmitFunction = {
                it.preventDefault()
                studentName?.let { name ->
                    val studentInfo = StudentInfo(school, group, name)
                    if (passcode.length == 8) {
                        scope.launch {
                            val data = Json.encodeToString(AdminJoinRoom(studentInfo, passcode))
                            wsClient.trySend(ServerCommand.ADMIN_JOIN_ROOM.name + data)
                        }
                    } else {
                        scope.launch {
                            val data = Json.encodeToString(GuestJoinRoom(studentInfo, passcode))
                            wsClient.trySend(ServerCommand.GUEST_JOIN_ROOM.name + data)
                        }
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
        if (props.activeStudents.students.isEmpty()) {
            styledDiv {
                css {
                    classes = mutableListOf("alert alert-warning")
                }
                +"There is no active room at this time!"
            }
        }
        styledFieldSet {
            css {
                attrs {
                    disabled = props.activeStudents.students.isEmpty()
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
                                studentName = null
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
                            isDisabled = props.activeStudents.students.isEmpty()
                            onClickFunction = {
                                if (props.activeStudents.students.isNotEmpty()) {
                                    group = Group.II
                                    studentName = null
                                }
                            }
                        }
                        linkItem {
                            name = Group.III.englishDisplay
                            isActive = group == Group.III
                            isDisabled = props.activeStudents.students.isEmpty()
                            onClickFunction = {
                                if (props.activeStudents.students.isNotEmpty()) {
                                    group = Group.III
                                    studentName = null
                                }
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
                        classes = mutableListOf("col-sm-4")
                    }
                    +"Student"
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-sm-8")
                    }
                    reactSelect {
                        attrs {
                            id = "reactSelectStudent"
                            value = if (studentName == null) null else ReactSelectOption(studentName!!, studentName!!)
                            options = filteredStudents.map { ReactSelectOption(it.name, it.name) }.toTypedArray()
                            onChange = {
                                studentName = it.label
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
                        classes = mutableListOf("col-sm-4")
                    }
                    +"Room Passcode"
                }
                styledDiv {
                    css {
                        classes = mutableListOf("col-sm-8")
                    }
                    styledInput {
                        css {
                            classes = mutableListOf("form-control")
                            attrs {
                                type = InputType.password
                                name = "roomPasscode"
                                required = true
                            }
                        }
                        attrs {
                            onChangeFunction = {
                                val target = it.target as HTMLInputElement
                                passcode = target.value
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
                        disabled = studentName == null || passcode.isBlank()
                    }
                }
                +"Join"
            }
        }
    }
}

fun RBuilder.joinRoom(handler: JoinRoomProps.() -> Unit) = child(joinRoom) {
    attrs {
        handler()
    }
}
