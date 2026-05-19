plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-user-domain")
}

dependencies {
    api(projects.labCommonDomain)
    api(projects.labCommonPort)
}
