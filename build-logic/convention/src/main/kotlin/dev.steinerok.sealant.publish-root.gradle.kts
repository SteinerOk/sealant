allprojects {
    group = property("GROUP") as String
    version = property("VERSION_NAME") as String
}

val isSnapshot by extra {
    version.toString().endsWith("-SNAPSHOT")
}
