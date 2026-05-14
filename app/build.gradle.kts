plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labCommonInfra)
    implementation(projects.labSpace.app)

    runtimeOnly(libs.postgresql)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("app.jar")
}
tasks.named("jar") {
    enabled = false
}
