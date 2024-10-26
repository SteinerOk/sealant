plugins {
    id("dev.steinerok.sealant.kotlin-library")
    id("dev.steinerok.sealant.spotless")
    id("dev.steinerok.sealant.publish-module")
}

dependencies {
    api(libs.dagger.runtime)
}
