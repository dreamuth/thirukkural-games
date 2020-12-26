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

package com.dreamuth

import kotlinx.coroutines.launch
import kotlinx.css.Position
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.right
import kotlinx.css.top
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RProps
import react.child
import react.functionalComponent
import styled.css
import styled.styledButton

external interface SignOutProps: RProps {
    var onSignOutHandler: () -> Unit
}

private var signOut = functionalComponent<SignOutProps> { props ->
    styledButton {
        css {
            classes = mutableListOf("btn btn-outline-primary mr-2")
            position = Position.absolute
            top = 6.px
            right = 4.px
        }
        attrs {
            onClickFunction = { props.onSignOutHandler() }
        }
        +"Sign out"
    }
}

fun RBuilder.signOut(handler: SignOutProps.() -> Unit) = child(signOut) {
    attrs {
        handler()
    }
}
