package com.hiper2d

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter


@SpringBootApplication
class Application {

    @Configuration
    class WebSocketConfig {

        @Bean
        fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()

        @Bean
        fun echoWebSocketHandler(): WebSocketHandler {
            val sessions = mutableSetOf<WebSocketSession>()

            return WebSocketHandler { session ->
                val sessionId = session.id
                val output = session.receive()
                    .doOnSubscribe {
                        println("Session $sessionId has been subscribed")
                        sessions.add(session)
                    }
                    .map { msg ->
                        var tmp = msg
                        sessions.forEach {
                            tmp = it.textMessage(it.id)
                        }
                        tmp
                    }
                    .doFinally {
                        sessions.remove(session)
                        println("Session $sessionId has been terminated")
                    }

                session.send(output).doFinally { println("Output Stream Disconnected") }
            }
        }

        @Bean
        fun webSocketURLMapping(): HandlerMapping {
            val mapping = SimpleUrlHandlerMapping()
            mapping.urlMap = mapOf("/ws/echo" to echoWebSocketHandler())
            mapping.order = 10
            mapping.setCorsConfigurations(
                mapOf("*" to CorsConfiguration().applyPermitDefaultValues())
            )
            return mapping
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}