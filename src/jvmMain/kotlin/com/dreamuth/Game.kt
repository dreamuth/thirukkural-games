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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import kotlin.concurrent.fixedRateTimer

/**
 *
 * @author Uttran Ishtalingam
 */
class Game(private val gameState: GameState, private val logger: Logger) {
    suspend fun userJoin(userSession: UserSession) {
        gameState.userJoin(userSession)
    }

    suspend fun userLeft(userSession: UserSession) {
        gameState.userLeft(userSession)
    }

    suspend fun processRequest(userSession: UserSession, command: String) {
        when {
            command.startsWith(ServerCommand.CREATE_ROOM.name) -> {
                val data = command.removePrefix(ServerCommand.CREATE_ROOM.name)
                val room = Json.decodeFromString<Room>(data)
                if (gameState.isExistingRoom(room.name)) {
                    logger.warn(userSession, ServerCommand.CREATE_ROOM, "Room ${room.name} already exists")
                    userSession.send(ClientCommand.ERROR_ROOM_EXISTS.name + Json.encodeToString(room))
                } else {
                    val userInfo = createAdminRoom(userSession, room, gameState)
                    val activeUserInfo = gameState.addUserInfo(userInfo)
                    if (userInfo == activeUserInfo) {
                        val questionState = createQuestionState()
                        val actualQuestionState = gameState.addQuestionState(activeUserInfo.roomName, questionState)
                        if (questionState == actualQuestionState) {
                            val response = AdminRoomResponse(activeUserInfo.adminPasscode!!, activeUserInfo.guestPasscode)
                            logger.info(userSession, "Created the room [${activeUserInfo.roomName}]")
                            userSession.send(ClientCommand.ADMIN_CREATED_ROOM.name + Json.encodeToString(response))
//                            sendAdminQuestionToMe(actualQuestionState, userInfo)
                        } else {
                            logger.warn(userSession, ServerCommand.CREATE_ROOM, "Someone just created the room ${room.name}")
                            userSession.send(ClientCommand.ERROR_ROOM_EXISTS.name + Json.encodeToString(room))
                        }
                    } else {
                        userSession.send(ClientCommand.ERROR_CLOSE_BROWSER)
                    }
                }
            }
            command.startsWith(ServerCommand.ADMIN_JOIN_ROOM.name) -> {
                val data = command.removePrefix(ServerCommand.ADMIN_JOIN_ROOM.name)
                val adminJoinRoom = Json.decodeFromString<AdminJoinRoom>(data)
                if (!gameState.isExistingRoom(adminJoinRoom.roomName)) {
                    logger.warn(userSession, ServerCommand.ADMIN_JOIN_ROOM, "Room ${adminJoinRoom.roomName} no longer exists")
                    userSession.send(ClientCommand.ERROR_ROOM_NOT_EXISTS.name + Json.encodeToString(Room(adminJoinRoom.roomName)))
                } else if (!gameState.isValidAdmin(adminJoinRoom)) {
                    logger.warn(userSession, ServerCommand.ADMIN_JOIN_ROOM, "Invalid passcode: ${adminJoinRoom.passcode}")
                    userSession.send(ClientCommand.ERROR_INVALID_PASSCODE)
                } else {
                    val userInfo = UserInfo(
                        session = userSession,
                        roomName = adminJoinRoom.roomName,
                        userType = UserType.ADMIN,
                        adminPasscode = adminJoinRoom.passcode,
                        guestPasscode = gameState.getGuestPasscode(adminJoinRoom)
                    )
                    gameState.addUserInfo(userInfo)
                    val response = AdminRoomResponse(userInfo.adminPasscode!!, userInfo.guestPasscode)
                    userSession.send(ClientCommand.ADMIN_JOINED_ROOM.name + Json.encodeToString(response))
                    gameState.getQuestionState(userInfo.roomName)?.let { questionState ->
                        sendTimeToAll(questionState, userInfo)
                        sendTopicsToAll(questionState, userInfo)
                        if (questionState.timerState.isLive) {
                            sendAdminQuestionToMe(questionState, userInfo)
                        }
                    }
                }
            }
            command.startsWith(ServerCommand.GUEST_JOIN_ROOM.name) -> {
                val data = command.removePrefix(ServerCommand.GUEST_JOIN_ROOM.name)
                val guestJoinRoom = Json.decodeFromString<GuestJoinRoom>(data)
                if (!gameState.isExistingRoom(guestJoinRoom.roomName)) {
                    logger.warn(userSession, ServerCommand.GUEST_JOIN_ROOM, "Room ${guestJoinRoom.roomName} no longer exists")
                    userSession.send(ClientCommand.ERROR_ROOM_NOT_EXISTS.name + Json.encodeToString(Room(guestJoinRoom.roomName)))
                } else if (!gameState.isValidGuest(guestJoinRoom)) {
                    logger.warn(userSession, ServerCommand.GUEST_JOIN_ROOM, "Invalid passcode: ${guestJoinRoom.passcode}")
                    userSession.send(ClientCommand.ERROR_INVALID_PASSCODE)
                } else {
                    val userInfo = UserInfo(
                        session = userSession,
                        roomName = guestJoinRoom.roomName,
                        userType = UserType.GUEST,
                        guestPasscode = guestJoinRoom.passcode
                    )
                    gameState.addUserInfo(userInfo)
                    userSession.send(ClientCommand.GUEST_JOINED_ROOM.name)
                    gameState.getQuestionState(userInfo.roomName)?.let { questionState ->
                        sendTimeToAll(questionState, userInfo)
                        sendTopicsToAll(questionState, userInfo)
                        if (questionState.timerState.isLive) {
                            sendGuestQuestionToMe(questionState, userInfo)
                        }
                    }
                }
            }
            command.startsWith(ServerCommand.START_GAME.name) -> {
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        questionState.timerState.isLive = true
                        val selectedTopic = questionState.topicState.selected
                        sendQuestionToAll(questionState, userInfo)
                        fixedRateTimer(name = "LiveTimer", daemon = true, period = 1000) {
                            println("OnTimer: ${questionState.timerState.isLive} ${questionState.timerState.time}")
                            questionState.timerState.time--
                            if (questionState.timerState.isLive && questionState.timerState.time >= 0) {
                                GlobalScope.launch {
                                    sendTimeToAll(questionState, userInfo)
                                }
                            } else {
                                this.cancel()
                                if (questionState.timerState.time < 0) {
                                    questionState.timerState.time = 0
                                }
                                questionState.topicState.removeTopic(selectedTopic)
                                GlobalScope.launch {
                                    sendTopicsToAll(questionState, userInfo)
                                }
                            }
                        }
                        sendTimeToAll(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.NEXT.name) -> {
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        when (questionState.topicState.selected) {
                            Topic.Athikaram -> questionState.athikaramState.goNext()
                            Topic.Kural, Topic.KuralPorul -> questionState.thirukkuralState.goNext()
                            Topic.FirstWord -> questionState.firstWordState.goNext()
                            Topic.LastWord -> questionState.lastWordState.goNext()
                        }
                        sendQuestionToAll(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.PREVIOUS.name) -> {
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        when (questionState.topicState.selected) {
                            Topic.Athikaram -> questionState.athikaramState.goPrevious()
                            Topic.Kural, Topic.KuralPorul -> questionState.thirukkuralState.goPrevious()
                            Topic.FirstWord -> questionState.firstWordState.goPrevious()
                            Topic.LastWord -> questionState.lastWordState.goPrevious()
                        }
                        sendQuestionToAll(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.RIGHT_ANSWER.name) -> {
                val question = command.removePrefix(ServerCommand.RIGHT_ANSWER.name)
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        questionState.scoreState.score[questionState.topicState.selected]?.add(question)
                        sendQuestionToAdmins(questionState, userInfo)
                        sendScoreToAdmins(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.WRONG_ANSWER.name) -> {
                val question = command.removePrefix(ServerCommand.WRONG_ANSWER.name)
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        questionState.scoreState.score[questionState.topicState.selected]?.remove(question)
                        sendQuestionToAdmins(questionState, userInfo)
                        sendScoreToAdmins(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.TOPIC_CHANGE.name) -> {
                val newTopic = Topic.valueOf(command.removePrefix(ServerCommand.TOPIC_CHANGE.name))
                val userInfo = gameState.getUserInfo(userSession)
                userInfo?.let {
                    val questionState = gameState.getQuestionState(userInfo.roomName)
                    questionState?.let {
                        questionState.timerState = TimerState()
                        questionState.topicState.selected = newTopic
                        sendTopicsToAll(questionState, userInfo)
                        sendTimeToAll(questionState, userInfo)
                    }
                }
            }
            command.startsWith(ServerCommand.SIGN_OUT.name) -> {
                gameState.userSignOut(userSession)
                logger.info(userSession, "Sending sign out")
                userSession.send(ClientCommand.SIGN_OUT)
            }
        }
    }

    private fun createAdminRoom(userSession: UserSession, room: Room, gameState: GameState) = UserInfo(
        session = userSession,
        roomName = room.name,
        userType = UserType.ADMIN,
        adminPasscode = gameState.createAdminPasscode(),
        guestPasscode = gameState.createGuestPasscode()
    )

    private fun createQuestionState(): QuestionState {
        val thirukkurals = fetchSource()
        return QuestionState(
            TopicState(),
            thirukkurals,
            TimerState(),
            ScoreState(),
            AthikaramState(thirukkurals),
            ThirukkuralState(thirukkurals),
            FirstWordState(thirukkurals),
            LastWordState(thirukkurals)
        )
    }

    private suspend fun sendAdminQuestionToMe(questionState: QuestionState, userInfo: UserInfo) {
        val adminQuestion = createAdminQuestion(questionState)
        val adminMessage = ClientCommand.ADMIN_QUESTION.name + Json.encodeToString(adminQuestion)
        logger.info(userInfo.session, "Sending admin room data")
        userInfo.session.session.trySend(adminMessage)
    }

    private suspend fun sendGuestQuestionToMe(questionState: QuestionState, userInfo: UserInfo) {
        val adminQuestion = createAdminQuestion(questionState)
        val guestQuestion = GuestQuestion(
            adminQuestion.topic, adminQuestion.question, question2 = adminQuestion.question2)
        val guestMessage = ClientCommand.GUEST_QUESTION.name + Json.encodeToString(guestQuestion)
        logger.info(userInfo.session, "Sending guest room data")
        userInfo.session.session.trySend(guestMessage)
    }

    private suspend fun sendQuestionToAll(questionState: QuestionState, userInfo: UserInfo) {
        val adminQuestion = createAdminQuestion(questionState)
        val guestQuestion = GuestQuestion(
            adminQuestion.topic, adminQuestion.question, question2 = adminQuestion.question2)
        logger.info(userInfo.session, "Sending room data to room [${userInfo.roomName}]")
        sendAdminQuestionToAllAdmins(adminQuestion, userInfo)
        sendGuestQuestionToAllGuests(guestQuestion, userInfo)
    }

    private suspend fun sendQuestionToAdmins(questionState: QuestionState, userInfo: UserInfo) {
        val adminQuestion = createAdminQuestion(questionState)
        logger.info(userInfo.session, "Sending question to admins in room [${userInfo.roomName}]")
        sendAdminQuestionToAllAdmins(adminQuestion, userInfo)
    }

    private suspend fun sendTimeToAll(questionState: QuestionState, userInfo: UserInfo) {
        val message = ClientCommand.TIME_UPDATE.name + Json.encodeToString(questionState.timerState)
        gameState.getSessionsForRoom(userInfo.roomName).forEach { it.trySend(message) }
    }

    private suspend fun sendTopicsToAll(questionState: QuestionState, userInfo: UserInfo) {
        logger.info(userInfo.session, "Sending available topics to room [${userInfo.roomName}]")
        println("Before Json: ${questionState.topicState}")
        val message = ClientCommand.TOPIC_STATE.name + Json.encodeToString(questionState.topicState)
        println("Sending msg: $message")
        gameState.getSessionsForRoom(userInfo.roomName).forEach { it.trySend(message) }
    }

    private suspend fun sendAdminQuestionToAllAdmins(adminQuestion: AdminQuestion, userInfo: UserInfo) {
        val adminMessage = ClientCommand.ADMIN_QUESTION.name + Json.encodeToString(adminQuestion)
        gameState.getAdminSessionsForRoom(userInfo.roomName).forEach { it.trySend(adminMessage) }
    }

    private suspend fun sendGuestQuestionToAllGuests(guestQuestion: GuestQuestion, userInfo: UserInfo) {
        val guestMessage = ClientCommand.GUEST_QUESTION.name + Json.encodeToString(guestQuestion)
        gameState.getGuestSessionsForRoom(userInfo.roomName).forEach { it.trySend(guestMessage) }
    }

    private suspend fun sendScoreToAdmins(questionState: QuestionState, userInfo: UserInfo) {
        logger.info(userInfo.session, "Sending score to admins in room [${userInfo.roomName}]")
        val studentScore = StudentScore(questionState.scoreState.score.map { it.key to it.value.size }.toMap())
        val scoreMessage = ClientCommand.SCORE_UPDATE.name + Json.encodeToString(studentScore)
        gameState.getAdminSessionsForRoom(userInfo.roomName).forEach { it.trySend(scoreMessage) }
    }

    private fun createAdminQuestion(roomState: QuestionState): AdminQuestion {
        return when (roomState.topicState.selected) {
            Topic.Athikaram -> {
                val question = roomState.athikaramState.getCurrent()
                val thirukkurals = roomState.thirukkuralState.kurals.filter { it.athikaram == question }
                val answered = roomState.scoreState.score[Topic.Athikaram]!!.contains(question)
                createAdminQuestion(roomState.topicState.selected, question, thirukkurals, answered)
            }
            Topic.KuralPorul -> {
                val question = roomState.thirukkuralState.getCurrent().porul
                val thirukkurals = roomState.thirukkuralState.kurals.filter { it.porul == question }
                val answered = roomState.scoreState.score[Topic.KuralPorul]!!.contains(question)
                createAdminQuestion(roomState.topicState.selected, question, thirukkurals, answered)
            }
            Topic.FirstWord -> {
                val question = roomState.firstWordState.getCurrent()
                val thirukkurals = roomState.thirukkuralState.kurals.filter { it.words.first() == question }
                val answered = roomState.scoreState.score[Topic.FirstWord]!!.contains(question)
                createAdminQuestion(roomState.topicState.selected, question, thirukkurals, answered)
            }
            Topic.LastWord -> {
                val question = roomState.lastWordState.getCurrent()
                val thirukkurals = roomState.thirukkuralState.kurals.filter { it.words.last() == question }
                val answered = roomState.scoreState.score[Topic.LastWord]!!.contains(question)
                createAdminQuestion(roomState.topicState.selected, question, thirukkurals, answered)
            }
            Topic.Kural -> {
                val question = roomState.thirukkuralState.getCurrent().kural
                val thirukkurals = roomState.thirukkuralState.kurals.filter { it.kural == question }
                val answered = roomState.scoreState.score[Topic.Kural]!!.contains(question.firstLine + question.secondLine)
                createAdminQuestion(roomState.topicState.selected, question, thirukkurals, answered)
            }
        }
    }

    private fun createAdminQuestion(
        topic: Topic,
        question: String,
        thirukkurals: List<Thirukkural>,
        answered: Boolean
    ): AdminQuestion {
        return AdminQuestion(topic, question, thirukkurals, answered)
    }

    private fun createAdminQuestion(
        topic: Topic,
        question: KuralOnly,
        thirukkurals: List<Thirukkural>,
        answered: Boolean
    ): AdminQuestion {
        return AdminQuestion(topic, question.firstLine, thirukkurals, answered, question.secondLine)
    }
}

fun Logger.info(userSession: UserSession, message: String) {
    info("[${userSession.name}] : $message.")
}

fun Logger.warn(userSession: UserSession, command: ServerCommand, message: String) {
    warn("[${userSession.name}] [${command}] : $message.")
}
