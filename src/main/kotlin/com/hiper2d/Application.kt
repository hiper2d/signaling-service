package com.hiper2d

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@SpringBootApplication
class Application {

    @Configuration
    class WebSocketConfig {

        @Bean
        fun webSocketHandlerAdapter() = WebSocketHandlerAdapter()

        @Bean
        fun echoWebSocketHandler(): WebSocketHandler {
            val processor = EmitterProcessor.create<String>()
            val outputEvents = Flux.from(processor)

            return WebSocketHandler { session ->
                val input = session.receive()
                    .doOnNext { processor.onNext(it.payloadAsText) }
                    .then()

                val output = session.send(
                    outputEvents.map(session::textMessage)
                )
                Mono.zip(input, output).then()
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