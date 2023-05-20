import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.diffplug.spotless")
}

// https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
val Project.libs: LibrariesForLibs
    get() = the()

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlintTool.get())
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(rootProject.file("gradle/spotless/copyright.kt"))
    }
    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**")
        licenseHeaderFile(rootProject.file("gradle/spotless/copyright.xml"), "(<[^!?])")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlintTool.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    groovyGradle {
        target("**/*.gradle")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
