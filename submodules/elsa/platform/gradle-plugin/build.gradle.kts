plugins {
    java
    id("com.gradle.plugin-publish") version "2.1.1"
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(gradleApi())
}

group = "com.gridnine.elsa"
val elsaVersion = "0.0.1"
version = elsaVersion

gradlePlugin {
    website.set("http://gridnine.com")
    vcsUrl.set("https://github.com/gn-avramenko/elsa")
    plugins {
        create("elsa-java") {
            id = "elsa-java"
            displayName = "Elsa java plugin"
            version = elsaVersion
            description = "Apply Elsa Framework"
            tags.set(listOf("java", "elsa"))
            implementationClass = "com.gridnine.platform.elsa.gradle.plugin.ElsaJavaPlugin"
        }
        create("elsa-web") {
            id = "elsa-web"
            displayName = "Elsa web plugin"
            version = elsaVersion
            description = "Apply Elsa Framework to web projects"
            tags.set(listOf("elsa"))
            implementationClass = "com.gridnine.platform.elsa.gradle.plugin.ElsaWebPlugin"
        }
    }
}


