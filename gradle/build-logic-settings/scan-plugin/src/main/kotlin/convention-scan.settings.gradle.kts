plugins {
    id("com.gradle.enterprise")
}

val publishBuildScan = false

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(publishBuildScan)
    }
}
