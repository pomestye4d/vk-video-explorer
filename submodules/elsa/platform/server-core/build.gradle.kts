plugins {
    java
    id("java-test-fixtures")
    id("elsa-java")
    id("java-library")
}

repositories {
    mavenCentral()
}
tasks.compileJava.get().dependsOn(tasks.getByName("eCodeGen"))

elsa {
    codegen {
        l10n(
            "src/main/java-gen",
            "com.gridnine.platform.elsa.server.core.CoreL10nMessagesRegistryConfigurator",
            "com.gridnine.platform.elsa.server.core.CoreL10nMessagesRegistryFactory",
            arrayListOf("src/main/codegen/core-server-l10n-messages.xml")
        )
        domain(
                "src/main/java-gen", "com.gridnine.platform.elsa.server.core.CoreDomainConfigurator",
           arrayListOf("src/main/codegen/core-server-domain.xml")
        )
        remoting(
            "src/main/java-gen", null,
            "com.gridnine.platform.elsa.server.core.CoreRemotingConstants", true,
            arrayListOf("../common-core/src/main/codegen/elsa-core-remoting.xml")
        )
    }
}

dependencies {
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")

    implementation("org.springframework:spring-webmvc:7.0.9")
    implementation("org.springframework:spring-context:7.0.9")
//    implementation("org.springframework:spring-websocket:7.0.9")
//    implementation("org.springframework:spring-messaging:7.0.9")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.25")
    implementation("com.google.code.gson:gson:2.14.0")


    implementation("com.fasterxml.jackson.core:jackson-core:2.22.2")
    implementation("com.nothome:javaxdelta:2.0.1")
    implementation("org.ehcache:ehcache:3.12.0")
    api("jakarta.servlet:jakarta.servlet-api:6.1.0")
    api("org.springframework:spring-jdbc:7.0.9")
    api("org.springframework:spring-tx:7.0.9")
    api("io.swagger.core.v3:swagger-models-jakarta:2.2.54")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
    api("io.swagger.core.v3:swagger-models-jakarta:2.2.54")
    implementation("io.swagger.core.v3:swagger-core-jakarta:2.2.54")
    implementation(project(":common-meta"))
    implementation(project(":common-core"))
    testImplementation("org.junit.platform:junit-platform-suite:6.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation(testFixtures(project(":common-core")))
    testImplementation("org.springframework:spring-test:7.0.9")
    testFixturesImplementation(testFixtures(project(":common-core")))
    testFixturesImplementation("org.springframework:spring-jdbc:7.0.9")
    testFixturesImplementation("org.springframework:spring-context:7.0.9")
    testFixturesImplementation("org.springframework:spring-test:7.0.9")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testFixturesImplementation("com.mchange:c3p0:0.9+")
    testFixturesImplementation("org.hsqldb:hsqldb:2+")
}


sourceSets.main {
    java.srcDirs("src/main/java", "src/main/java-gen")
}

group ="com.gridnine.elsa"
version ="1.0"
tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    failFast = true
}
