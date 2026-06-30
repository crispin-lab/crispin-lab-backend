rootProject.name = "crispin-lab-backend"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":lab-common-domain",
    ":lab-common-port",
    ":lab-common",
    ":lab-common-infra",
    ":lab-common-persistence",
    ":lab-api-support",
    ":lab-space:domain",
    ":lab-space:app",
    ":lab-user:domain",
    ":lab-user:app",
    ":lab-notification:domain",
    ":lab-notification:app",
    ":app"
)
