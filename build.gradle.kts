@Suppress("GradlePackageUpdate")

plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate to later Java versions. Automatically."

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")
    testImplementation("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite.recipe:rewrite-github-actions:$rewriteVersion")

    runtimeOnly("org.openrewrite:rewrite-java-8")
    runtimeOnly("org.openrewrite:rewrite-java-11")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-java-tck")

    testImplementation("org.assertj:assertj-core:latest.release")

    testImplementation("com.google.guava:guava:29.0-jre")

    testImplementation("commons-codec:commons-codec:1.+")

    testRuntimeOnly("commons-io:commons-io:2.+")
    testRuntimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr353:latest.release")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-core:latest.release")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind:latest.release")
}
