package com.crispinlab

import com.epages.restdocs.apispec.gradle.OpenApi3Extension
import groovy.lang.Closure
import io.swagger.v3.oas.models.servers.Server

fun OpenApi3Extension.setServers(vararg urls: String) {
    val owner = this
    setServers(
        urls.map { url ->
            object : Closure<Server>(owner, owner) {
                @Suppress("unused")
                fun doCall(server: Server): Server =
                    server.apply {
                        this.url = url
                    }
            }
        },
    )
}
