import java.io.File

plugins {
    alias(libs.plugins.crispinlab.kopring.web)
    alias(libs.plugins.crispinlab.kopring.exposed)
}

dependencies {
    implementation(projects.labCommon)
    implementation(projects.labCommonDomain)
    implementation(projects.labCommonInfra)
    implementation(projects.labSpace.app)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("app.jar")
}
tasks.named("jar") {
    enabled = false
}

// `-Ppinpoint=true` 일 때 Pinpoint Java agent 를 부착. 절차는 .claude/rules/dev-infra.md.
// `run { }` wrapper 는 script object reference 가 closure 에 캡처되어 configuration cache 가
// 깨지는 문제를 막기 위함 — 제거 금지.
run {
    val pinpointEnabled = providers.gradleProperty("pinpoint")
    val pinpointAgentPath = providers.environmentVariable("PINPOINT_AGENT_PATH")
    val pinpointCollectorHost = providers.environmentVariable("PINPOINT_COLLECTOR_HOST")
    val pinpointAgentId = providers.environmentVariable("PINPOINT_AGENT_ID")
    val pinpointApplicationName = providers.environmentVariable("PINPOINT_APPLICATION_NAME")
    // :app:bootRun 의 CWD 는 app/ 이라 .env 의 프로젝트 루트 기준 상대경로를 rootDir 로 재해석.
    val projectRoot = rootDir.absolutePath

    tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
        jvmArgumentProviders.add(
            CommandLineArgumentProvider {
                if (pinpointEnabled.orNull?.toBoolean() != true) {
                    return@CommandLineArgumentProvider emptyList()
                }
                val jar = pinpointAgentPath.orNull
                    ?: throw GradleException(
                        "PINPOINT_AGENT_PATH 환경변수가 설정되어 있어야 합니다. " +
                            "먼저 ./docker/pinpoint/agent/download.sh 를 실행해 주세요."
                    )
                val jarFile = File(jar)
                val resolvedJar = (if (jarFile.isAbsolute) jarFile else File(projectRoot, jar))
                if (!resolvedJar.isFile || !resolvedJar.canRead()) {
                    throw GradleException(
                        "PINPOINT_AGENT_PATH 가 유효한 파일이 아닙니다: ${resolvedJar.absolutePath}. " +
                            "버전 변경 후라면 ./docker/pinpoint/agent/download.sh 를 다시 실행해 주세요."
                    )
                }
                val agentIdValue = pinpointAgentId.getOrElse("crispin-lab-local")
                val applicationNameValue = pinpointApplicationName.getOrElse("crispin-lab")
                val collectorHostValue = pinpointCollectorHost.getOrElse("localhost")
                listOf(
                    "-javaagent:${resolvedJar.absolutePath}",
                    "-Dpinpoint.agentId=$agentIdValue",
                    "-Dpinpoint.applicationName=$applicationNameValue",
                    "-Dpinpoint.profiler.profiles.active=local",
                    "-Dprofiler.transport.grpc.collector.ip=$collectorHostValue"
                )
            }
        )
    }
}
