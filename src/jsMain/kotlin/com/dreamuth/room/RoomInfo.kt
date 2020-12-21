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

import kotlinx.css.Position
import kotlinx.css.bottom
import kotlinx.css.minWidth
import kotlinx.css.position
import kotlinx.css.px
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

external interface RoomInfoProps: RProps {
    var roomName: String
    var adminPasscode: String
    var guestPasscode: String
}

class RoomInfo : RComponent<RoomInfoProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("card text-white bg-dark m-2")
                position = Position.absolute
                bottom = 3.px
                minWidth = 250.px
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-body")
                }
                keyValue("Room name", props.roomName)
                keyValue("Admin passcode", props.adminPasscode)
                keyValue("Guest passcode", props.guestPasscode)
            }
        }
    }

    private fun StyledDOMBuilder<DIV>.keyValue(key: String, value: String) {
        styledDiv {
            css {
                classes = mutableListOf("d-flex justify-content-between")
            }
            styledP {
                css {
                    classes = mutableListOf("card-text")
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

fun RBuilder.roomInfo(handler: RoomInfoProps.() -> Unit): ReactElement {
    return child(RoomInfo::class) {
        this.attrs(handler)
    }
}
