tasks.register("eCodeGen") {
    dependsOn(listOf("common-core","server-admin"
        ,  "server-core",  "web-admin").map { ":${it}:eCodeGen" })
    group = "elsa"
}