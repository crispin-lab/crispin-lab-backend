plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.restdocs.api.spec.gradle.plugin)
    compileOnly(libs.swagger.models)
}

gradlePlugin {
    plugins {
        // user-facing
        register("jvm") {
            id = "crispinlab.jvm"
            implementationClass = "JvmConventionPlugin"
        }
        register("kopringService") {
            id = "crispinlab.kopring.service"
            implementationClass = "KopringServiceConventionPlugin"
        }
        register("kopringWeb") {
            id = "crispinlab.kopring.web"
            implementationClass = "KopringWebConventionPlugin"
        }
        register("kopringExposed") {
            id = "crispinlab.kopring.exposed"
            implementationClass = "KopringExposedConventionPlugin"
        }
        register("snowflake") {
            id = "crispinlab.snowflake"
            implementationClass = "SnowflakeConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "crispinlab.kotlin.serialization"
            implementationClass = "KotlinSerializationConventionPlugin"
        }
        register("restAssured") {
            id = "crispinlab.rest-assured"
            implementationClass = "RestAssuredConventionPlugin"
        }
        register("restdocs") {
            id = "crispinlab.restdocs"
            implementationClass = "RestdocsConventionPlugin"
        }

        // internal building blocks
        register("kotest") {
            id = "crispinlab.kotest"
            implementationClass = "KotestConventionPlugin"
        }
        register("kopringBase") {
            id = "crispinlab.kopring.base"
            implementationClass = "KopringBaseConventionPlugin"
        }
        register("kopringTest") {
            id = "crispinlab.kopring.test"
            implementationClass = "KopringTestConventionPlugin"
        }
    }
}
