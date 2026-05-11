import com.crispinlab.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class KopringTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", libs.findLibrary("spring.boot.starter.test").get())
                add("testImplementation", libs.findLibrary("kotest.extensions.spring").get())
                add("testImplementation", libs.findLibrary("springmockk").get())
            }
        }
    }
}
