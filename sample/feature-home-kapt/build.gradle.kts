plugins {
    id("dev.steinerok.sealant.android-library")
    //id("dev.steinerok.sealant.anvil-with-factories")
    id ("dev.steinerok.sealant.anvil")
    id ("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "dev.steinerok.sealant.sample.feature.home"

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
    implementation(libs.androidx.fragmentKtx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtimeKtx)

    implementation(libs.timber)

    implementation(libs.dagger.runtime)
    kapt(libs.dagger.compiler)

    implementation(projects.sealant.diCommon)
    implementation(projects.sealant.coreApi)
    implementation(projects.sealant.appcomponentApi)
    implementation(projects.sealant.viewmodelApi)
    implementation(projects.sealant.fragmentApi)
    implementation(projects.sealant.workApi)
    anvil(projects.sealant.coreCodegen)
    anvil(projects.sealant.appcomponentCodegen)
    anvil(projects.sealant.viewmodelCodegen)
    anvil(projects.sealant.fragmentCodegen)
    anvil(projects.sealant.workCodegen)

    implementation(libs.anvil.annotations.optional)
    implementation(projects.sealant.sample.coreDi)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junitKtx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
