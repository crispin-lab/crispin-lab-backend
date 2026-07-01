package com.crispinlab.app

import com.crispinlab.composition.CompositionModule
import com.crispinlab.notification.NotificationModule
import com.crispinlab.space.SpaceModule
import com.crispinlab.user.UserModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackageClasses = [
        Application::class,
        SpaceModule::class,
        UserModule::class,
        NotificationModule::class,
        CompositionModule::class
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
