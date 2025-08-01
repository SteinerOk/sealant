plugins {
    id("version-catalog")
    id("dev.steinerok.sealant.publish-module")
}

val publishedProjectsProvider = provider {
    listOf(
        "sealant-di-common",
        "sealant-core-runtime",
        "sealant-core-compiler-ksp",
        "sealant-appcomponent-runtime",
        "sealant-appcomponent-compiler-ksp",
        "sealant-fragment-runtime",
        "sealant-fragment-compiler-ksp",
        "sealant-viewmodel-runtime",
        "sealant-viewmodel-compiler-ksp",
        "sealant-work-runtime",
        "sealant-work-compiler-ksp",
    )
}

// Projects that are published but should be excluded from the version catalog.
// This means we don't want our users to use these dependencies.
val excludedProjects = setOf<String>()

// Handle special cases when automatic mapping doesn't work well
val manualOverrides = mapOf(
    "sealant-di-common" to "di-common",
)

// A hack to prevent all projects evaluation if version catalog generation wasn't requested
// Issue: https://github.com/gradle/gradle/issues/33568
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name == VersionCatalogPlugin.GENERATE_CATALOG_FILE_TASKNAME }) {
        catalog.versionCatalog {
            configureVersionCatalog(
                group = project.group.toString(),
                version = project.version.toString(),
                publishedProjects = publishedProjectsProvider.get().toSet(),
            )
        }
    }
}

fun VersionCatalogBuilder.configureVersionCatalog(
    group: String,
    version: String,
    publishedProjects: Set<String>,
) {
    // Versions
    val versionAlias = version("sealant", version)

    // Libraries
    for (projectName in publishedProjects - excludedProjects) {
        if (projectName == "sealant-version-catalog") continue
        val effectiveProjectName = when (projectName) {
            in manualOverrides -> manualOverrides.getValue(projectName)
            else -> projectName
        }
        library(
            /* alias = */ projectName,
            /* group = */ group,
            /* artifact = */ effectiveProjectName,
        ).versionRef(versionAlias)
    }
}
