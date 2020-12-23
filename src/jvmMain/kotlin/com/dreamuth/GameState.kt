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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.LinkedHashSet

/**
 *
 * @author Uttran Ishtalingam
 */
class GameState(private val logger: Logger) {
    private val allUserSessions = Collections.synchronizedSet(LinkedHashSet<UserSession>())
    private val users = ConcurrentHashMap<UserSession, UserInfo>()
    private val rooms = ConcurrentHashMap<String, QuestionState>()

    suspend fun userJoin(userSession: UserSession) {
        allUserSessions.add(userSession)
        userSession.send("Welcome ${userSession.name}")
        sendActiveRoomsToUser(userSession)
    }

    private suspend fun sendActiveRoomsToUser(userSession: UserSession) {
        val data = Json.encodeToString(RoomNamesData(rooms.keys.filterNotNull().sorted().toList()))
        userSession.send(ClientCommand.ACTIVE_ROOMS.name + data)
    }

    suspend fun userLeft(userSession: UserSession) {
        allUserSessions -= userSession
        removeUser(userSession, "browser close")
    }

    suspend fun userSignOut(userSession: UserSession) {
        removeUser(userSession, "sign out")
    }

    private suspend fun removeUser(userSession: UserSession, reason: String) {
        val userInfo = users.remove(userSession)
        userInfo?.let {
            logger.info("Removing ${userSession.name} on $reason.")
            if (users.values.none { it.roomName == userInfo.roomName && it.adminPasscode != null }) {
                val guests = users.entries.filter { it.value.roomName == userInfo.roomName }.map { it.key }
                guests.forEach {
                    users.remove(it)
                    it.send(ClientCommand.SIGN_OUT)
                }
                val questionState = rooms.remove(userInfo.roomName)
                questionState?.let {
                    questionState.timerState = TimerState()
                    logger.info(userInfo, "room removed")
                    sendActiveRoomsToAllUsers()
                }
            }
        }
    }

    fun addUserInfo(userInfo: UserInfo): UserInfo {
        val result = users.putIfAbsent(userInfo.session, userInfo)
        if (result != null) {
            logger.error("User ${userInfo.session.name} already exists. Old: $result New: $userInfo")
        }
        return result ?: userInfo
    }

    fun getUserInfo(userSession: UserSession): UserInfo? {
        return users[userSession]
    }

    fun isExistingRoom(randomString: String) = rooms.containsKey(randomString)

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
            .map { it.guestPasscode }
            .first()
    }

    suspend fun addQuestionState(roomName: String, questionState: QuestionState): QuestionState {
        val result = rooms.putIfAbsent(roomName, questionState)
        if (result != null) {
            logger.warn("RoomState already exists for room $roomName")
        } else {
            sendActiveRoomsToAllUsers()
        }
        return result ?: questionState
    }


    private suspend fun sendActiveRoomsToAllUsers() {
        allUserSessions.forEach { sendActiveRoomsToUser(it) }
    }

    fun getQuestionState(roomName: String): QuestionState? {
        return rooms[roomName]
    }

    fun getAdminSessionsForRoom(roomName: String): List<WebSocketSession> {
        return users.values
            .filter { it.roomName == roomName }
            .filter { it.adminPasscode != null }
            .map { it.session.session }
    }

    fun getGuestSessionsForRoom(roomName: String): List<WebSocketSession> {
        return users.values
            .filter { it.roomName == roomName }
            .filter { it.adminPasscode == null }
            .map { it.session.session }
    }

    fun getSessionsForRoom(roomName: String): List<WebSocketSession> {
        return users.values
            .filter { it.roomName == roomName }
            .map { it.session.session }
    }
}
