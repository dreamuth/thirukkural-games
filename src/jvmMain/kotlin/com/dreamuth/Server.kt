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
import io.ktor.websocket.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
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
    val gameState = GameState(LoggerFactory.getLogger(GameState::class.java))
    val game = Game(gameState, LoggerFactory.getLogger(Game::class.java))

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

    routing {
        get("/") {
            call.respondText(this::class.java.classLoader.getResource("index.html")!!.readText(), ContentType.Text.Html)
        }
        static("/") {
            resources()
        }
        webSocket("/ws") {
            val logger = call.application.environment.log
            val userSession = UserSession(this)
            logger.info(userSession, "connected...")
            game.userJoin(userSession)
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val command = frame.readText()
                        logger.info(userSession, "said: $command")
                        game.processRequest(userSession, command)
                    }
                }
            } finally {
                logger.info(userSession, "disconnected...")
                game.userLeft(userSession)
            }
        }
    }
}
