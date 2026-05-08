package com.crispinlab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = ["com.crispinlab.app", "com.crispinlab.space"]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
