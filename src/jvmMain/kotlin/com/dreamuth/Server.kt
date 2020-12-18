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
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

@ExperimentalCoroutinesApi
fun main() {
    embeddedServer(
        Netty,
//        watchPaths = listOf("thirukkural-games"),
        port = 9090,
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
            val userInfo = UserInfo(
                userSession = userSession,
                socketSession = socketSession,
                roomName = gameState.createRoomName(),
                userType = UserType.PRACTICE)
            val activeUserInfo = gameState.addUserInfo(userInfo)
            val roomState = gameState.addRoomState(activeUserInfo.roomName, createQuestionState())
            sendPracticeData(roomState, activeUserInfo.roomName, gameState)
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
                        Topic.KuralPorul -> roomState.thirukkuralState.goNext()
                        else -> println("Error: Invalid next request...")
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
                        Topic.KuralPorul -> roomState.thirukkuralState.goPrevious()
                        else -> println("Error: Invalid next request...")
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

suspend fun sendPracticeData(roomState: QuestionState, roomName: String, gameState: GameState) {
    val question = when (roomState.selectedTopic) {
        Topic.Athikaram -> roomState.athikaramState.getCurrent()
        Topic.KuralPorul -> roomState.thirukkuralState.getCurrent().porul
        else -> "Error..."
    }
    val thirukkurals = when (roomState.selectedTopic) {
        Topic.Athikaram -> roomState.thirukkuralState.kurals.filter { it.athikaram == question }
        Topic.KuralPorul -> roomState.thirukkuralState.kurals.filter { it.porul == question }
        else -> listOf()
    }
    val practiceData = PracticeData(roomState.selectedTopic, question, thirukkurals)
    gameState.getSessionsForRoom(roomName).forEach { it.send(createMessage(practiceData)) }
}

fun createMessage(practiceData: PracticeData) =
    Frame.Text(ClientCommand.PRACTICE_RESPONSE.name + Json.encodeToString(practiceData))

private suspend fun createQuestionState(): QuestionState {
    val thirukkurals = fetchSource()
    return QuestionState(Topic.Athikaram, thirukkurals, AthikaramState(thirukkurals), ThirukkuralState(thirukkurals))
}

/**
 * Sends a message to a list of [this] [WebSocketSession].
 */
suspend fun List<WebSocketSession>.send(frame: Frame) {
    forEach {
        try {
            it.send(frame.copy())
        } catch (t: Throwable) {
            try {
                it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
            } catch (ignore: ClosedSendChannelException) {
                // at some point it will get closed
            }
        }
    }
}

/**
 * A user session is identified by a unique nonce ID. This nonce comes from a secure random source.
 */
data class UserSession(val id: String)
