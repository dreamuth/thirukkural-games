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

import com.dreamuth.Thirukkural
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.ReactElement
import styled.css
import styled.styledDiv
import styled.styledP
import styled.styledSmall

external interface KuralDisplayProps: RProps {
    var selectedThirukkural: Thirukkural
    var showPorul: Boolean
}

class KuralDisplay : RComponent<KuralDisplayProps, RState>() {
    override fun RBuilder.render() {
        styledDiv {
            css {
                classes = mutableListOf("card text-white bg-success m-2")
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-header d-flex justify-content-between")
                }
                styledDiv {
                    +props.selectedThirukkural.athikaram
                }
                styledDiv {
                    css {
                        classes = mutableListOf("font-italic d-flex flex-column text-right")
                    }
                    styledSmall {
                        +"அதிகாரம் : ${props.selectedThirukkural.athikaramNo}"
                    }
                    styledSmall {
                        +"குறள் : ${props.selectedThirukkural.kuralNo}"
                    }
                }
            }
            styledDiv {
                css {
                    classes = mutableListOf("card-body")
                }
                if (props.showPorul) {
                    styledP {
                        css {
                            classes = mutableListOf("card-text")
                        }
                        +props.selectedThirukkural.porul
                    }
                } else {
                    styledP {
                        css {
                            classes = mutableListOf("card-text")
                        }
                        +props.selectedThirukkural.kural.firstLine
                    }
                    styledP {
                        css {
                            classes = mutableListOf("card-text")
                        }
                        +props.selectedThirukkural.kural.secondLine
                    }
                }
            }
        }
    }
}

fun RBuilder.kuralDisplay(handler: KuralDisplayProps.() -> Unit): ReactElement {
    return child(KuralDisplay::class) {
        this.attrs(handler)
    }
}
