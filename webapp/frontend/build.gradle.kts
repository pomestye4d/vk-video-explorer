import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask

plugins {
    id("elsa-web")
    id("com.github.node-gradle.node") version "7.1.0"
}


elsa {
    codegen {
        webApp("src-gen", "src", "registry.RegistryConfigurator.ts", "ru.vga.jtasks.",  "jtasks",true, arrayListOf<String>(
            "../backend/src/main/codegen/web-app.xml"))
    }
}

node {
    version.set("24.20.0")
    download.set(true)
    workDir.set(project.layout.buildDirectory.file("nodejs").get().asFile)
    npmWorkDir.set(project.layout.buildDirectory.file("npm").get().asFile)
}

tasks.register<NpxTask>("eslint-fix") {
    group = "elsa"
    description = "Run ESLint code analysis"

    command.set("eslint")
    args.set(listOf(
        "src/**",
        "src-gen/**",
        "--ext", ".js,.ts,.jsx,.tsx",
        "--format", "stylish",
        "--fix"
    ))

    ignoreExitValue.set(false)
}
tasks.register<NpmTask>("install-dependencies") {
    group = "other"
    description = "Install dependencies"

    args.set(listOf("install"))
    ignoreExitValue.set(false)
}
tasks.register<NpmTask>("frontend-dist") {
    group = "other"
    description = "Build distribution"
    dependsOn(":web-admin:install-dependencies", "install-dependencies")
    args.set(listOf("run", "build"))
    ignoreExitValue.set(false)
    doFirst {
        delete(fileTree("dist"))
    }
}