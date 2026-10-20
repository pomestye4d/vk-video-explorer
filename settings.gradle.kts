
includeBuild("submodules/elsa/platform/gradle-plugin")
include("common-meta", "common-core",
     "server-atomikos", "server-file-storage", "server-core","server-postgres", "web-core", "main-backend", "main-frontend")
project(":common-meta").projectDir = file("submodules/elsa/platform/common-meta")
project(":common-core").projectDir = file("submodules/elsa/platform/common-core")
project(":server-atomikos").projectDir = file("submodules/elsa/platform/server-atomikos")
project(":server-file-storage").projectDir = file("submodules/elsa/platform/server-file-storage")
project(":server-core").projectDir = file("submodules/elsa/platform/server-core")
project(":server-postgres").projectDir = file("submodules/elsa/platform/server-postgres")
project(":web-core").projectDir = file("submodules/elsa/platform/web-core")
project(":main-frontend").projectDir = file("webapp/frontend")
project(":main-backend").projectDir = file("webapp/backend")