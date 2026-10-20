plugins {
    java
    id("java-test-fixtures")
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    implementation("javax.transaction:jta:1.1")
    implementation("org.springframework:spring-context:7.0.9")
    implementation("com.atomikos:atomikos-util:6.0.1")
    implementation("com.atomikos:transactions-jdbc:6.0.1:jakarta")
    implementation("com.atomikos:transactions-jta:6.0.1:jakarta")
    implementation("org.springframework:spring-tx:7.0.9")
    implementation("org.springframework:spring-jdbc:7.0.9")
    implementation(project(":common-meta"))
    implementation(project(":common-core"))
    implementation(project(":server-core"))
    testImplementation("org.junit.platform:junit-platform-suite:6.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation(testFixtures(project(":common-core")))
    testImplementation(testFixtures(project(":server-core")))
    testFixturesImplementation(testFixtures(project(":common-core")))
    testFixturesImplementation(testFixtures(project(":server-core")))
    testFixturesImplementation("org.springframework:spring-context:7.0.9")
    testFixturesImplementation("org.springframework:spring-test:7.0.9")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:6.1.3")

}

group ="com.gridnine.elsa"
version ="1.0"

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    failFast = true
}
