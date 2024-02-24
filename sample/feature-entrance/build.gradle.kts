plugins {
    id("dev.steinerok.sealant.android-library")
    id("dev.steinerok.sealant.anvil-with-factories")
}

android {
    namespace = "dev.steinerok.sealant.sample.feature.entrance"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.coreKtx)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodelKtx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.timber)

    implementation(libs.dagger.runtime)

    implementation(projects.sealant.diCommon)
    implementation(projects.sealant.coreApi)
    implementation(projects.sealant.appcomponentApi)
    implementation(projects.sealant.viewmodelApi)
    implementation(projects.sealant.fragmentApi)
    implementation(projects.sealant.workApi)
    implementation(libs.androidx.fragmentKtx)
    anvil(projects.sealant.coreCodegen)
    anvil(projects.sealant.appcomponentCodegen)
    anvil(projects.sealant.viewmodelCodegen)
    anvil(projects.sealant.fragmentCodegen)
    anvil(projects.sealant.workCodegen)

    implementation(projects.sealant.sample.coreDi)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junitKtx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
