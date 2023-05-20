plugins {
    id("com.vanniktech.maven.publish")
}

publishing {
    repositories {
        maven {
            name = "CustomLocal"
            val releasesRepoUrl = "${project.rootDir}/repos/releases"
            val snapshotsRepoUrl = "${project.rootDir}/repos/snapshots"
            val isSnapshot: Boolean by rootProject.extra
            url = uri(if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl)
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/SteinerOk/sealant")
            credentials {
                username = project.findProperty("GitHub.username") as String?
                    ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("GitHub.token") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral("S01")
}
