plugins {
    id("dev.steinerok.sealant.codegen-library")
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
    implementation(libs.kotlinpoet)

    implementation(libs.dagger.runtime)
    implementation(libs.anvil.annotations)


    testImplementation(kotlin("test"))
}
