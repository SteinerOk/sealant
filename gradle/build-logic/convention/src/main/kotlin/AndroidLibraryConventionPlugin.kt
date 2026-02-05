import dev.steinerok.sealant.androidLibrary
import dev.steinerok.sealant.configureAndroidLibrary
import dev.steinerok.sealant.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Convention plugin for Android library projects.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.kotlin.android.get().pluginId)
            apply(libs.plugins.android.library.get().pluginId)
        }

        configureAndroidLibrary()

        androidLibrary {
            //
        }

        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xexplicit-api=strict",
                )
            }
        }
    }
}
