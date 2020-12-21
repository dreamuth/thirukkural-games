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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 *
 * @author Uttran Ishtalingam
 */
class GameState {
    private val practiceRoomNo = AtomicLong(0)
    private val roomState = ConcurrentHashMap<String, QuestionState>()
    private val users = ConcurrentHashMap<UserSession, UserInfo>()
    private val sessions = ConcurrentHashMap<UserSession, MutableList<WebSocketSession>>()

    suspend fun userJoin(userSession: UserSession, socketSession: WebSocketSession) {
        val list = sessions.computeIfAbsent(userSession) { CopyOnWriteArrayList() }
        list.add(socketSession)
        if (users.containsKey(userSession)) {
            users[userSession]?.roomName?.let { roomName ->
                roomState[roomName]?.let { questionState ->
                    println("Adding $userSession to room [$roomName]")
                    socketSession.trySend(createPracticeData(questionState))
                }
            }
        } else {
            socketSession.trySend("Welcome $userSession")
        }
    }

    fun userLeft(userSession: UserSession, serverSession: WebSocketSession) {
        val socketSessions = sessions[userSession]
        socketSessions?.remove(serverSession)
        socketSessions?.let {
            if (socketSessions.isEmpty()) {
                val userInfo = users.remove(userSession)
                userInfo?.let {
                    println("Removing user session on browser close ${userInfo.userSession}")
                    if (users.values.none { it.roomName == userInfo.roomName }) {
                        val questionState = roomState.remove(userInfo.roomName)
                        questionState?.let {
                            println("Removing room ${userInfo.roomName}")
                        }
                    }
                }
            }
        }
    }

    fun userSignOut(userSession: UserSession) {
        val userInfo = users.remove(userSession)
        userInfo?.let {
            println("Removing user session on sign out ${userInfo.userSession}")
            if (users.values.none { it.roomName == userInfo.roomName }) {
                val questionState = roomState.remove(userInfo.roomName)
                questionState?.let {
                    println("Removing room ${userInfo.roomName}")
                }
            }
        }
    }

    fun addUserInfo(userInfo: UserInfo): UserInfo {
        val result = users.putIfAbsent(userInfo.userSession, userInfo)
        if (result != null) {
            println("session already exists, ${userInfo.userSession}")
        }
        return result ?: userInfo
    }

    fun getUserInfo(userSession: UserSession): UserInfo? {
        return users[userSession]
    }

    fun createPracticeRoom(): String {
        return "Practice-" + practiceRoomNo.getAndIncrement()
    }

    fun isExistingRoom(randomString: String) = roomState.containsKey(randomString)

    fun createAdminPasscode(): String {
        return getRandomNumber(8)
    }

    fun createGuestPasscode(): String {
        return getRandomNumber(4)
    }

    private fun getRandomNumber(length: Int): String {
        val charset = ('0'..'9')
        return (1..length).map { charset.random() }.joinToString("")
    }

    fun isValidAdmin(adminJoinRoom: AdminJoinRoom): Boolean {
        return users.map { it.value }
            .filter { it.roomName == adminJoinRoom.roomName }
            .any { it.adminPasscode == adminJoinRoom.passcode }
    }

    fun isValidGuest(guestJoinRoom: GuestJoinRoom): Boolean {
        return users.map { it.value }
            .filter { it.roomName == guestJoinRoom.roomName }
            .any { it.guestPasscode == guestJoinRoom.passcode }
    }

    fun getGuestPasscode(adminJoinRoom: AdminJoinRoom): String {
        return users.map { it.value }
            .filter { it.roomName == adminJoinRoom.roomName }
            .filter { it.adminPasscode == adminJoinRoom.passcode }
            .map { it.guestPasscode }
            .first()!!
    }

    fun addRoomState(roomName: String, questionState: QuestionState): QuestionState {
        val result = roomState.putIfAbsent(roomName, questionState)
        if (result != null) {
            println("RoomState already exists for room, $roomName")
        }
        return result ?: questionState
    }

    fun getRoomState(roomName: String): QuestionState? {
        return roomState[roomName]
    }

    fun getSessionsForRoom(roomName: String): List<WebSocketSession> {
        return users.values
            .filter { it.roomName == roomName }
            .map { it.userSession }
            .map { sessions.getOrDefault(it, listOf()) }
            .flatten()
    }

    fun getSessionsForUser(userSession: UserSession): MutableList<WebSocketSession>? {
        return sessions[userSession]
    }
}

