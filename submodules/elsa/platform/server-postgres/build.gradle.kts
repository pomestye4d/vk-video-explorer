plugins {
    java
}
repositories {
    mavenCentral()
}

group ="com.gridnine.elsa"
version ="1.0"

dependencies {
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.mchange:c3p0:0.14.1")
    implementation("org.springframework:spring-jdbc:7.0.9")
    implementation("org.springframework:spring-context:7.0.9")
    implementation(project(":server-core"))
    implementation(project(":common-core"))
}
