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

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch

class WsClient(private val client: HttpClient) {
    var session: WebSocketSession? = null

    suspend fun initConnection() {
        try {
            println("Creating ws connection...")
            connect()
        } catch (e: Exception) {
            if (e is ClosedReceiveChannelException) {
                println("Disconnected. ${e.message}.")
            } else if (e is WebSocketException) {
                println("Unable to connect.")
            }

            window.setTimeout({
                GlobalScope.launch { initConnection() }
            }, 5000)
        }
    }


    private suspend fun connect() {
        println("Connecting to host: ${window.location.hostname}, port: ${window.location.port} ")
        session = client.webSocketSession(
            method = HttpMethod.Get,
            host = window.location.hostname.replace("https", "wss"),
//            port = window.location.port.toInt(),
            path = "/ws"
        )
    }

    suspend fun send(message: String) {
        session?.send(Frame.Text(message))
    }

    suspend fun send(command: ServerCommand) {
        session?.send(Frame.Text(command.name))
    }

    suspend fun receive(onReceive: (input: String) -> Unit) {
        while (true) {
            val frame = session?.incoming?.receive()
            if (frame is Frame.Text) {
                onReceive(frame.readText())
            }
        }
    }
}
