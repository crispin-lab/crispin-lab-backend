package com.crispinlab

import com.epages.restdocs.apispec.gradle.OpenApi3Extension
import groovy.lang.Closure
import io.swagger.v3.oas.models.servers.Server
import org.gradle.kotlin.dsl.closureOf

fun OpenApi3Extension.setServers(vararg urls: String) {
    setServers(
        urls.map {
            @Suppress("UNCHECKED_CAST")
            closureOf<Server> {
                url = it
            } as Closure<Server>
        },
    )
}
