plugins {
    id("com.gradle.develocity")
}

val publishBuildScan = false

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing.onlyIf { publishBuildScan }
    }
}
