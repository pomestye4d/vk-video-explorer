plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("elsa-java")
}

repositories {
    mavenCentral()
}

elsa {
    codegen {

    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework:spring-context:7.0.9")
    implementation("com.atomikos:transactions-jdbc:6.0.1:jakarta")
    implementation("com.atomikos:transactions-jta:6.0.1:jakarta")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    implementation(project(":common-meta"))
    implementation(project(":common-core"))
    implementation(project(":server-core"))
    implementation(project(":server-postgres"))
    implementation(project(":server-atomikos"))
    testImplementation(testFixtures(project(":server-core")))
    testImplementation(testFixtures(project(":common-core")))
    testImplementation(testFixtures(project(":server-atomikos")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

sourceSets.main {
    java.srcDirs("src/main/java", "src/main/java-gen")
}

tasks.test {
    useJUnitPlatform()
}
