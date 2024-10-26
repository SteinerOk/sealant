plugins {
    id("dev.steinerok.sealant.codegen-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(libs.auto.service.ksp)
    compileOnly(libs.auto.service.annotations)

    implementation(libs.kotlinpoet)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.anvil.compiler.api)

    implementation(libs.dagger.runtime)
    implementation(libs.anvil.annotations)
    implementation(libs.anvil.compiler.utils)

    implementation(projects.sealant.compilerUtils)
    implementation(projects.sealant.compilerUtilsEmbedded)
}
