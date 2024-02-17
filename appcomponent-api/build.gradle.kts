plugins {
    id("dev.steinerok.sealant.android-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

android {
    namespace = "dev.steinerok.sealant.appcomponent"
}

dependencies {
    api(projects.sealant.coreApi)

    api(libs.androidx.activity)
}
