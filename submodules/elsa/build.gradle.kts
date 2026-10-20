plugins {
    java
}
java.testResultsDir.set(layout.buildDirectory.dir("junit-xml"))

tasks.register("updateLocalGradlePlugins") {
    group = "elsa"
}

tasks.register("eCodeGen") {
    group = "elsa"
}


