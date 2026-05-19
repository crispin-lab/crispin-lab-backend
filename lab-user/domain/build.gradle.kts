plugins {
    alias(libs.plugins.crispinlab.jvm)
    `java-test-fixtures`
}

// default jar 이름이 sub-project 명("domain") 이라 :app:bootJar 의 BOOT-INF/lib 에서 다른 도메인의
// domain jar 와 충돌한다. 컨벤션 수준 (path 기반 자동 명명) fix 는 후속 별도 티켓 후보.
tasks.named<Jar>("jar") {
    archiveBaseName.set("lab-user-domain")
}

dependencies {
    api(projects.labCommonDomain)
    api(projects.labCommonPort)
}
