@Suppress("GradlePackageUpdate")

plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate to later Java versions. Automatically."

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    compileOnly("org.codehaus.groovy:groovy:latest.release")

    annotationProcessor("org.projectlombok:lombok:latest.release")
    testImplementation("org.projectlombok:lombok:latest.release")

    annotationProcessor("org.openrewrite:rewrite-templating:$rewriteVersion")
    compileOnly("com.google.errorprone:error_prone_core:2.+") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite.recipe:rewrite-github-actions:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-jenkins:$rewriteVersion")
    implementation("org.openrewrite:rewrite-templating:$rewriteVersion")
    implementation("org.openrewrite.meta:rewrite-analysis:$rewriteVersion")

    runtimeOnly("org.openrewrite:rewrite-java-8")
    runtimeOnly("org.openrewrite:rewrite-java-11")
    runtimeOnly("org.openrewrite:rewrite-java-17")
    runtimeOnly("org.openrewrite:rewrite-java-21")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-java-tck")
    testImplementation("org.openrewrite:rewrite-kotlin:$rewriteVersion")
    testImplementation("org.openrewrite.gradle.tooling:model:$rewriteVersion")

    testImplementation("org.assertj:assertj-core:latest.release")

    testImplementation("com.google.guava:guava:33.0.0-jre")
    testImplementation("joda-time:joda-time:2.12.3")

    testRuntimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr353")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-core")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind")
    testRuntimeOnly("org.codehaus.groovy:groovy:latest.release")
    testRuntimeOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    testRuntimeOnly("com.google.code.findbugs:jsr305:3.0.2")
    testRuntimeOnly(gradleApi())
}
