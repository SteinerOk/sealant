plugins {
    id("dev.steinerok.sealant.android-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

android {
    namespace = "dev.steinerok.sealant.work"
}

dependencies {
    api(projects.sealant.coreRuntime)

    implementation(libs.androidx.work.runtime)
}
