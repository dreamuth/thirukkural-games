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

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoom(val roomName: String)

@Serializable
data class AdminRoomResponse(val adminPasscode: String, val guestPasscode: String)

@Serializable
data class AdminJoinRoom(val roomName: String, val passcode: String)

@Serializable
data class GuestJoinRoom(val roomName: String, val passcode: String)

@Serializable
data class Thirukkural(
    val athikaramNo: Int,
    val athikaram: String,
    val athikaramDesc: String,
    val kuralNo: Int,
    val kural: KuralOnly,
    val porul: String,
    val words: List<String>,
)

@Serializable
data class KuralOnly(val firstLine: String, val secondLine: String)

@Serializable
data class AdminQuestion(
    val topic: Topic,
    val question: String,
    val thirukkurals: List<Thirukkural>,
    val question2: String? = null
)

@Serializable
data class GuestQuestion(val topic: Topic, val question: String, val question2: String? = null)

@Serializable
data class RoomNamesData(val roomNames: List<String>)

enum class ServerCommand {
    CREATE_ROOM,
    ADMIN_JOIN_ROOM,
    GUEST_JOIN_ROOM,
    NEXT,
    PREVIOUS,
    TOPIC_CHANGE,
    SIGN_OUT
}

enum class ClientCommand {
    PRACTICE_KURAL_RESPONSE,
    ADMIN_ROOM_RESPONSE,
    CREATE_ROOM_EXISTS,
    JOIN_ROOM_NOT_EXISTS,
    INVALID_PASSCODE,
    SIGN_OUT,
    ACTIVE_ROOMS,
    ERROR_CLOSE_BROWSER,
    ADMIN_QUESTION,
    GUEST_QUESTION
}

enum class Topic(val tamil: String) {
    FirstWord("முதல் வார்த்தை"),
    Athikaram("அதிகாரம்"),
    Kural("குறள்"),
    KuralPorul("பொருள்"),
    LastWord("கடைசி வார்த்தை");

    companion object {
        fun getTopic(tamil: String): Topic {
            return values().first { it.tamil == tamil }
        }
    }
}

suspend fun WebSocketSession.trySend(message: String) {
    try {
        send(message)
    } catch (t: Throwable) {
        try {
            close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Failed to send message"))
        } catch (ignore: ClosedSendChannelException) {
            println("at some point it will get closed")
        }
    }
}

suspend fun WebSocketSession.trySend(command: ClientCommand) {
    trySend(command.name)
}


