plugins {
    id("com.gradle.develocity")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"

        tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
        publishing.onlyIf { false }

        obfuscation {
            username { "Redacted" }
            hostname { "Redacted" }
            ipAddresses { addresses -> addresses.map { "0.0.0.0" } }
        }
    }
}
