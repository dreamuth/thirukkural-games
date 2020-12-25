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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.io.UnsupportedEncodingException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.collections.LinkedHashSet

/**
 *
 * @author Uttran Ishtalingam
 */
class GameState(private val logger: Logger) {
    private val projectName = "thirukkural-games"

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
                    GlobalScope.launch {
                        SecretManagerServiceClient.create().use {
                            val emailSecretName = SecretVersionName.of(projectName, "email-password", "3")
                            val emailResponse = it.accessSecretVersion(emailSecretName)
                            val emailSecret = emailResponse.payload.data.toStringUtf8()
                            sendReport(userInfo, questionState, emailSecret)

                            val sheetsSecretName = SecretVersionName.of(projectName, "service-account-json", "1")
                            val sheetsResponse = it.accessSecretVersion(sheetsSecretName)
                            val serviceAccount = sheetsResponse.payload.data.toStringUtf8()
                            updateReportSheet(userInfo, questionState, serviceAccount)
                        }
                    }
                }
            }
        }
    }

    private fun updateReportSheet(userInfo: UserInfo, questionState: QuestionState, serviceAccount: String) {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val spreadsheetId = "1otzHv-6VKUAQuROts9cBjrbVanPaIpZrN-tN60ajgqE"
        val range = "Score!A3"
        val jacksonFactory = JacksonFactory.getDefaultInstance()

        val serviceAccountCredentials = ServiceAccountCredentials.fromStream(serviceAccount.byteInputStream())
        val googleCredentials = serviceAccountCredentials.createScoped(SheetsScopes.SPREADSHEETS)
        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(googleCredentials)

        val service = Sheets.Builder(httpTransport, jacksonFactory, requestInitializer)
            .setApplicationName(projectName)
            .build()
        val timeNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
        val values = ValueRange().setValues(
            listOf(
                listOf<Any>(
                    timeNow,
                    questionState.school.englishDisplay,
                    questionState.group.englishDisplay,
                    userInfo.roomName,
                    questionState.scoreState.score[Topic.Athikaram]!!.count(),
                    questionState.scoreState.score[Topic.KuralPorul]!!.count(),
                    questionState.scoreState.score[Topic.Kural]!!.count(),
                    questionState.scoreState.score[Topic.FirstWord]!!.count(),
                    questionState.scoreState.score[Topic.LastWord]!!.count(),
                    questionState.scoreState.score.values.flatten().count()
                )
            )
        )
        service.spreadsheets()
            .values()
            .append(spreadsheetId, range, values)
            .setValueInputOption("USER_ENTERED")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
    }

    private fun sendReport(userInfo: UserInfo, questionState: QuestionState, data: String) {
        val properties = Properties()
        properties.setProperty("mail.smtp.host", "smtp.gmail.com")
        properties.setProperty("mail.smtp.port", "587")
        properties.setProperty("mail.smtp.auth", "true")
        properties.setProperty("mail.smtp.starttls.enable", "true")
        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication("uttran.ishtalingam@gmail.com", data)
            }
        })

        try {
            val msg = MimeMessage(session)
            msg.setFrom(InternetAddress("uttran.ishtalingam@gmail.com", "HTS Kids Thirukkural Games 2021"))
            msg.addRecipient(Message.RecipientType.TO, InternetAddress("dreamuth@gmail.com"))
            msg.setSubject("HTS Kids Thirukkural Games 2021 : [${userInfo.roomName}] score", "UTF-8")
            val text = """
                School: ${questionState.school.englishDisplay}
                Age Group: ${questionState.group.englishDisplay}
                Student: ${userInfo.roomName}
                
                ${Topic.Athikaram.tamilDisplay} : ${questionState.scoreState.score[Topic.Athikaram]?.count()}
                ${Topic.KuralPorul.tamilDisplay} : ${questionState.scoreState.score[Topic.KuralPorul]?.count()}
                ${Topic.Kural.tamilDisplay} : ${questionState.scoreState.score[Topic.Kural]?.count()}
                ${Topic.FirstWord.tamilDisplay} : ${questionState.scoreState.score[Topic.FirstWord]?.count()}
                ${Topic.LastWord.tamilDisplay} : ${questionState.scoreState.score[Topic.LastWord]?.count()}
                
                Total: ${questionState.scoreState.score.values.flatten().count()}
            """.trimIndent()
            msg.setText(text, "UTF-8")
            Transport.send(msg)
        } catch (e: AddressException) {
            logger.error(userInfo, "Failed to send email", e)
        } catch (e: MessagingException) {
            logger.error(userInfo, "Failed to send email", e)
        } catch (e: UnsupportedEncodingException) {
            logger.error(userInfo, "Failed to send email", e)
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
