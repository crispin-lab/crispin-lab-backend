import com.crispinlab.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class KotestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", libs.findLibrary("kotlin.test.junit5").get())
                add("testImplementation", libs.findLibrary("kotest.runner.junit5").get())
                add("testRuntimeOnly", libs.findLibrary("junit.platform.launcher").get())
            }
        }
    }
}
