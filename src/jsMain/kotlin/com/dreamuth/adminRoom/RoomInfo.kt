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

import com.dreamuth.ActiveUsers
import com.dreamuth.StudentInfo
import kotlinx.css.fontSize
import kotlinx.css.pct
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
import styled.styledSpan

external interface RoomInfoProps: RProps {
    var activeStudent: StudentInfo
    var adminPasscode: String
    var guestPasscode: String
    var activeUsers: ActiveUsers
}

class RoomInfo : RComponent<RoomInfoProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("card text-white bg-dark m-2")
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-header p-2")
                }
                +props.activeStudent.getRoomName()
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-body p-2")
                }
                keyValue("Admin passcode", props.adminPasscode, props.activeUsers.admins)
                keyValue("Guest passcode", props.guestPasscode, props.activeUsers.guests)
            }
        }
    }

    private fun StyledDOMBuilder<DIV>.keyValue(key: String, value: String, users: Int? = null) {
        styledDiv {
            css {
                classes = mutableListOf("d-flex justify-content-between align-items-center mt-1 mb-1")
            }
            styledDiv {
                css {
                    classes = mutableListOf("row m-0")
                }
                users?.let {
                    styledSpan {
                        css {
                            classes = mutableListOf("badge badge-secondary mr-2")
                            fontSize = 100.pct
                        }
                        +"$users"
                    }
                }
                styledP {
                    css {
                        classes = mutableListOf("card-text mb-0")
                    }
                    +"$key: "
                }
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

fun RBuilder.roomInfo(handler: RoomInfoProps.() -> Unit): ReactElement {
    return child(RoomInfo::class) {
        this.attrs(handler)
    }
}
