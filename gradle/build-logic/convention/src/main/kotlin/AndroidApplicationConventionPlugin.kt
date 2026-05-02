import dev.steinerok.sealant.androidApplication
import dev.steinerok.sealant.configureAndroidApplication
import dev.steinerok.sealant.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.kotlin
import kotlin.text.get

/**
 * Convention plugin for Android application projects.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val builtInKotlin = providers
            .gradleProperty("android.builtInKotlin")
            .getOrElse("true").toBooleanStrict()

        with(pluginManager) {
            if (!builtInKotlin) {
                apply(libs.plugins.kotlin.android.get().pluginId)
            }
            apply(libs.plugins.android.application.get().pluginId)
        }

        configureAndroidApplication()

        androidApplication {
            packaging {
                resources {
                    resources.excludes += listOf(
                        "**.properties",
                        "**/*.kotlin_*",
                        "/META-INF/*.md",
                        "/META-INF/*.txt",
                        "/META-INF/*.dot",
                        "/META-INF/{AL2.0,LGPL2.1}",
                        "/META-INF/NOTICE",
                        "/META-INF/README",
                        "/META-INF/LICENSE",
                        "/META-INF/CHANGES",
                        "/META-INF/DEPENDENCIES",
                        "NOTICE",
                        "README",
                        "LICENSE",
                        "CHANGES",
                        "DEPENDENCIES",
                    )
                }
            }
        }
    }
}
