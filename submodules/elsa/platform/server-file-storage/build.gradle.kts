plugins {
    java
    id("java-test-fixtures")
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.xadisk:xadisk:1.2.2@jar")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.springframework:spring-context:7.0.9")
    implementation("com.atomikos:transactions-jta:6.0.1:jakarta")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    implementation("javax.resource:connector-api:1.5")
    implementation("org.springframework:spring-tx:7.0.9")
    implementation(project(":common-core"))
    implementation(project(":server-core"))
    testImplementation("org.junit.platform:junit-platform-suite:6.1.3")
    testImplementation(testFixtures(project(":common-core")))
    testImplementation(testFixtures(project(":server-core")))
    testImplementation(testFixtures(project(":server-atomikos")))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testFixturesImplementation(testFixtures(project(":common-core")))
    testFixturesImplementation(testFixtures(project(":server-core")))
    testFixturesImplementation(testFixtures(project(":server-atomikos")))
    testFixturesImplementation("org.springframework:spring-test:7.0.9")
    testFixturesImplementation("org.springframework:spring-context:7.0.9")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
}


group ="com.gridnine.elsa"
version ="1.0"

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    failFast = true
}
