plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-notification-domain")
}

dependencies {
    api(projects.labCommonDomain)
    api(projects.labCommonPort)
    api(projects.labUser.domain)
}
