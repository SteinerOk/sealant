allprojects {
    group = property("GROUP") as String
    version = property("VERSION_NAME") as String
}

extra["isSnapshot"] = version.toString().endsWith("-SNAPSHOT")
