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

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

@ExperimentalCoroutinesApi
fun main() {
    val myPort = System.getenv("PORT")?.toInt() ?: 9090
    embeddedServer(
        Netty,
//        watchPaths = listOf("thirukkural-games"),
        port = myPort,
        module = Application::myModule
    ).apply { start(wait = true) }
}

@ExperimentalCoroutinesApi
fun Application.myModule() {

    val gameState = GameState()

    /**
     * First we install the features we need. They are bound to the whole application.
     * Since this method has an implicit [Application] receiver that supports the [install] method.
     */
    // This adds automatically Date and Server headers to each response, and would allow you to configure
    // additional headers served to each response.
    install(DefaultHeaders)
    // This uses use the logger to log every call (request/response)
    install(CallLogging)
    // This installs the websockets feature to be able to establish a bidirectional configuration
    // between the server and the client
    install(WebSockets) {
        pingPeriod = Duration.ofMinutes(1)
        maxFrameSize = Long.MAX_VALUE
    }
    // This enables the use of sessions to keep information between requests/refreshes of the browser.
    install(Sessions) {
        cookie<UserSession>("SESSION")
    }

    // This adds an interceptor that will create a specific session in each request if no session is available already.
    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<UserSession>() == null) {
            call.sessions.set(UserSession(generateNonce()))
        }
    }

    routing {
        get("/") {
            call.respondText(this::class.java.classLoader.getResource("index.html")!!.readText(), ContentType.Text.Html)
        }
        static("/") {
            resources()
        }
        webSocket("/ws") {
            val session = call.sessions.get<UserSession>()
            // We check that we actually have a session. We should always have one,
            // since we have defined an interceptor before to set one.
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }
            println("client $session connected...")
            gameState.userJoin(session, this)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        println("Client $session said: ${frame.readText()}")
                        processRequest(gameState, session, this, frame.readText())
                    }
                }
            } finally {
                println("client $session disconnected...")
                gameState.userLeft(session, this)
            }
        }
    }
}

suspend fun processRequest(gameState: GameState, userSession: UserSession, socketSession: WebSocketSession, command: String) {
    when {
        command.startsWith(ServerCommand.PRACTICE.name) -> {
            val userInfo = gameState.getUserInfo(userSession)
                ?: gameState.addUserInfo(createUserInfo(userSession, socketSession, gameState))
            val roomState = gameState.addRoomState(userInfo.roomName, createQuestionState())
//            timer(name = "practiceTimer", daemon = true, period = 1000) {
//                println("On Timer...")
//            }
            sendPracticeData(roomState, userInfo.roomName, gameState)
        }
        command.startsWith(ServerCommand.CREATE_ROOM.name) -> {
            val data = command.removePrefix(ServerCommand.CREATE_ROOM.name)
            val createRoom = Json.decodeFromString<CreateRoom>(data)
            val userInfo = UserInfo(
                userSession = userSession,
                socketSession = socketSession,
                roomName = createRoom.roomName,
                userType = UserType.ADMIN,
                adminPasscode = gameState.createAdminPasscode(),
                guestPasscode = gameState.createGuestPasscode())
            val activeUserInfo = gameState.addUserInfo(userInfo)
            gameState.addRoomState(activeUserInfo.roomName, createQuestionState())
            val response = AdminRoomResponse(activeUserInfo.adminPasscode!!, activeUserInfo.guestPasscode!!)
            socketSession.send(Frame.Text(ClientCommand.ADMIN_ROOM_RESPONSE.name + Json.encodeToString(response)))
        }
        command.startsWith(ServerCommand.ADMIN_JOIN_ROOM.name) -> {
            val data = command.removePrefix(ServerCommand.ADMIN_JOIN_ROOM.name)
            val adminJoinRoom = Json.decodeFromString<AdminJoinRoom>(data)
            if (!gameState.isExistingRoom(adminJoinRoom.roomName)) {
                socketSession.send(Frame.Text("ERROR: Invalid room name"))
            } else if (!gameState.isValidAdmin(adminJoinRoom)) {
                socketSession.send(Frame.Text("ERROR: Invalid passcode"))
            } else {
                val userInfo = UserInfo(
                    userSession = userSession,
                    socketSession = socketSession,
                    roomName = adminJoinRoom.roomName,
                    userType = UserType.ADMIN,
                    adminPasscode = adminJoinRoom.passcode,
                    guestPasscode = gameState.getGuestPasscode(adminJoinRoom)
                )
                gameState.addUserInfo(userInfo)
                val response = AdminRoomResponse(userInfo.adminPasscode!!, userInfo.guestPasscode!!)
                socketSession.send(Frame.Text(ClientCommand.ADMIN_ROOM_RESPONSE.name + Json.encodeToString(response)))
            }
        }
        command.startsWith(ServerCommand.GUEST_JOIN_ROOM.name) -> {
            val data = command.removePrefix(ServerCommand.GUEST_JOIN_ROOM.name)
            val guestJoinRoom = Json.decodeFromString<GuestJoinRoom>(data)
            if (!gameState.isExistingRoom(guestJoinRoom.roomName)) {
                socketSession.send(Frame.Text("ERROR: Invalid room name"))
            } else if (!gameState.isValidGuest(guestJoinRoom)) {
                socketSession.send(Frame.Text("ERROR: Invalid passcode"))
            } else {
                val userInfo = UserInfo(
                    userSession = userSession,
                    socketSession = socketSession,
                    roomName = guestJoinRoom.roomName,
                    userType = UserType.GUEST,
                    guestPasscode = guestJoinRoom.passcode
                )
                gameState.addUserInfo(userInfo)
            }
        }
        command.startsWith(ServerCommand.NEXT.name) -> {
            val userInfo = gameState.getUserInfo(userSession)
            userInfo?.let {
                val roomState = gameState.getRoomState(userInfo.roomName)
                roomState?.let {
                    when (roomState.selectedTopic) {
                        Topic.Athikaram -> roomState.athikaramState.goNext()
                        Topic.Kural, Topic.KuralPorul -> roomState.thirukkuralState.goNext()
                        Topic.FirstWord -> roomState.firstWordState.goNext()
                        Topic.LastWord -> roomState.lastWordState.goNext()
                    }
                    sendPracticeData(roomState, userInfo.roomName, gameState)
                }
            }
        }
        command.startsWith(ServerCommand.PREVIOUS.name) -> {
            val userInfo = gameState.getUserInfo(userSession)
            userInfo?.let {
                val roomState = gameState.getRoomState(userInfo.roomName)
                roomState?.let {
                    when (roomState.selectedTopic) {
                        Topic.Athikaram -> roomState.athikaramState.goPrevious()
                        Topic.Kural, Topic.KuralPorul -> roomState.thirukkuralState.goPrevious()
                        Topic.FirstWord -> roomState.firstWordState.goPrevious()
                        Topic.LastWord -> roomState.lastWordState.goPrevious()
                    }
                    sendPracticeData(roomState, userInfo.roomName, gameState)
                }
            }
        }
        command.startsWith(ServerCommand.TOPIC_CHANGE.name) -> {
            val newTopic = Topic.valueOf(command.removePrefix(ServerCommand.TOPIC_CHANGE.name))
            val userInfo = gameState.getUserInfo(userSession)
            userInfo?.let {
                val roomState = gameState.getRoomState(userInfo.roomName)
                roomState?.let {
                    roomState.selectedTopic = newTopic
                    sendPracticeData(roomState, userInfo.roomName, gameState)
                }
            }
        }
    }
}

private fun createUserInfo(
    userSession: UserSession,
    socketSession: WebSocketSession,
    gameState: GameState
) = UserInfo(
    userSession = userSession,
    socketSession = socketSession,
    roomName = gameState.createPracticeRoom(),
    userType = UserType.PRACTICE
)

suspend fun sendPracticeData(roomState: QuestionState, roomName: String, gameState: GameState) {
    val message = createPracticeData(roomState)
    println("Sending practice data to room [$roomName]")
    gameState.getSessionsForRoom(roomName).forEach { it.send(message) }
}

fun createPracticeData(roomState: QuestionState): String {
    return when (roomState.selectedTopic) {
        Topic.Athikaram -> {
            val question = roomState.athikaramState.getCurrent()
            val thirukkurals = roomState.thirukkuralState.kurals.filter { it.athikaram == question }
            createMessage(roomState, question, thirukkurals)
        }
        Topic.KuralPorul -> {
            val question = roomState.thirukkuralState.getCurrent().porul
            val thirukkurals = roomState.thirukkuralState.kurals.filter { it.porul == question }
            createMessage(roomState, question, thirukkurals)
        }
        Topic.FirstWord -> {
            val question = roomState.firstWordState.getCurrent()
            val thirukkurals = roomState.thirukkuralState.kurals.filter { it.words.first() == question }
            createMessage(roomState, question, thirukkurals)
        }
        Topic.LastWord -> {
            val question = roomState.lastWordState.getCurrent()
            val thirukkurals = roomState.thirukkuralState.kurals.filter { it.words.last() == question }
            createMessage(roomState, question, thirukkurals)
        }
        Topic.Kural -> {
            val question = roomState.thirukkuralState.getCurrent().kural
            val thirukkurals = roomState.thirukkuralState.kurals.filter { it.kural == question }
            createMessage(roomState, question, thirukkurals)
        }
    }
}

private fun createMessage(
    roomState: QuestionState,
    question: String,
    thirukkurals: List<Thirukkural>
): String {
    val practiceData = PracticeData(roomState.selectedTopic, question, thirukkurals)
    return ClientCommand.PRACTICE_RESPONSE.name + Json.encodeToString(practiceData)
}

private fun createMessage(
    roomState: QuestionState,
    question: KuralOnly,
    thirukkurals: List<Thirukkural>
): String {
    val practiceKuralData = PracticeKuralData(roomState.selectedTopic, question, thirukkurals)
    return ClientCommand.PRACTICE_KURAL_RESPONSE.name + Json.encodeToString(practiceKuralData)
}

private suspend fun createQuestionState(): QuestionState {
    val thirukkurals = fetchSource()
    return QuestionState(
        Topic.Athikaram,
        thirukkurals,
        AthikaramState(thirukkurals),
        ThirukkuralState(thirukkurals),
        FirstWordState(thirukkurals),
        LastWordState(thirukkurals)
    )
}

///**
// * Sends a message to a list of [this] [WebSocketSession].
// */
//suspend fun List<WebSocketSession>.send(frame: Frame) {
//    forEach {
//        try {
//            it.send(frame.copy())
//        } catch (t: Throwable) {
//            try {
//                it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
//            } catch (ignore: ClosedSendChannelException) {
//                // at some point it will get closed
//            }
//        }
//    }
//}

/**
 * A user session is identified by a unique nonce ID. This nonce comes from a secure random source.
 */
data class UserSession(val id: String)
