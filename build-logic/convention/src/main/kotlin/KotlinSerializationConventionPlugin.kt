import com.crispinlab.libs
import com.crispinlab.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("unused")
class KotlinSerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", libs.findLibrary("kotlin.serialization.json").get())
                add("implementation", libs.findLibrary("kotlin.reflect").get())
            }
        }
    }
}
