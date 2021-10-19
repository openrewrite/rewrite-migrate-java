rootProject.name = "rewrite-migrate-java"

plugins {
    id("com.gradle.enterprise") version "3.7"
}

gradleEnterprise {
    server = "https://ge.openrewrite.org"
}
