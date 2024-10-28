plugins {
    id("dev.steinerok.sealant.codegen-library")
    alias(libs.plugins.ksp)
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

kotlin {
    explicitApi()
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    ksp(libs.auto.service.ksp)
    compileOnly(libs.auto.service.annotations)

    implementation(libs.ksp.api)

    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    implementation(projects.sealant.compilerUtils)
    implementation(projects.sealant.compilerUtilsKsp)


    testImplementation(kotlin("test"))
}
