plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-space-domain")
}

dependencies {
    api(projects.labCommonDomain)
    api(projects.labCommonPort)
}
