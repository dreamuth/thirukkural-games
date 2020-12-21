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

import kotlinx.css.Color
import kotlinx.css.color
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledA
import styled.styledLi

external interface LinkItemProps: RProps {
    var name: String
    var isActive: Boolean
    var onClickFunction: () -> Unit
}

private var linkItem = functionalComponent<LinkItemProps> { props ->
    styledLi {
        css {
            classes = mutableListOf("nav-item")
        }
        styledA {
            css {
                val activeStyle = if (props.isActive) "active" else ""
                classes = mutableListOf("nav-link $activeStyle rounded-pill")
                color = Color("#555")
                put("data-toggle", "pill")
                attrs {
                    role = "button"
                    onClickFunction = { props.onClickFunction() }
                }
            }
            +props.name
        }
    }
}

fun RBuilder.linkItem(handler: LinkItemProps.() -> Unit) = child(linkItem) {
    attrs {
        handler()
    }
}
