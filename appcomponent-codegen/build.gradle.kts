plugins {
    id("dev.steinerok.sealant.codegen-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(libs.auto.service.ksp)
    compileOnly(libs.auto.service.annotations)

    api(libs.kotlin.compiler.embeddable)
    api(libs.anvil.compiler.api)
    api(libs.kotlinpoet)

    implementation(libs.dagger.runtime)
    implementation(libs.anvil.annotations)
    implementation(libs.anvil.compiler.utils)

    api(projects.sealant.coreCodegen)
}
