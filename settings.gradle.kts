rootProject.name = "rewrite-migrate-java"

plugins {
    id("com.gradle.enterprise") version "3.7"
}

gradleEnterprise {
    server = "https://ge.openrewrite.org/"

    buildCache {
        local {
            isEnabled = true
        }

        remote<HttpBuildCache> {
            isPush = true
            setUrl("https://ge.openrewrite.org/cache/")
        }

    }

    buildScan {
        publishAlways()
    }
}
