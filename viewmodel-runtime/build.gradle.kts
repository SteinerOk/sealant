plugins {
    id("dev.steinerok.sealant.android-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

android {
    namespace = "dev.steinerok.sealant.viewmodel"
}

dependencies {
    implementation(libs.anvil.annotations.optional)

    api(projects.sealant.coreRuntime)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
}
