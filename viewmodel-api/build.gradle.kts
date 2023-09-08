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

    api(projects.sealant.coreApi)

    api(libs.androidx.activity.mainKtx)
    api(libs.androidx.fragment.mainKtx)
}
