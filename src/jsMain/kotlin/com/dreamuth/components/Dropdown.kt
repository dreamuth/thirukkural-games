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

package com.dreamuth.components

import kotlinx.css.LinearDimension
import kotlinx.css.width
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledButton
import styled.styledDiv
import styled.styledSpan

external interface DropdownProps: RProps {
    var id: String
    var names: List<List<String>>
    var selectedName: String
    var badge: Int?
    var width: LinearDimension?
    var onDropdownClick: (Int, String) -> Unit
}

class Dropdown : RComponent<DropdownProps, RState>() {
    override fun RBuilder.render() {
        styledButton {
            css {
                val disabledStyle = if (props.names.flatten().isEmpty()) "disabled" else ""
                classes = mutableListOf("btn btn-success dropdown-toggle $disabledStyle")
                props.width?.let { width = it }
            }
            attrs {
                id = props.id
                role = "button"
                attributes["data-toggle"] = "dropdown"
                attributes["aria-haspopup"] = "true"
                attributes["aria-expanded"] = "false"
            }
            +props.selectedName
            props.badge?.let {
                styledSpan {
                    css {
                        classes = mutableListOf("badge badge-light")
                    }
                    +"$it"
                }
            }
        }
        styledDiv {
            css {
                classes = mutableListOf("dropdown-menu dropdown-menu-right")
            }
            attrs {
                attributes["aria-labelledby"] = props.id
            }

            for ((listIndex, sectionList) in props.names.withIndex()) {
                for ((nameIndex, name) in sectionList.withIndex()) {
                    styledButton {
                        css {
                            classes = mutableListOf("dropdown-item")
                        }
                        +name
                        attrs {
                            onClickFunction = {
                                props.onDropdownClick(nameIndex, name)
                            }
                        }
                    }
                }
                if (listIndex + 1 < props.names.size) {
                    styledDiv {
                        css {
                            classes = mutableListOf("dropdown-divider")
                        }
                    }
                }
            }
        }
    }
}

fun RBuilder.dropdown(handler: DropdownProps.() -> Unit): ReactElement {
    return child(Dropdown::class) {
        this.attrs(handler)
    }
}
