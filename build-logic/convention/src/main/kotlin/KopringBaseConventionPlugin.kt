import com.crispinlab.applySpringBootBom
import com.crispinlab.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class KopringBaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins(
                "org.springframework.boot",
                "org.jetbrains.kotlin.plugin.spring",
                "crispinlab.kopring.test"
            )
            applySpringBootBom()

            tasks.named("bootJar") {
                enabled = false
            }
            tasks.named("jar") {
                enabled = true
            }
        }
    }
}
