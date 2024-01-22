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
    compileOnly("com.google.errorprone:error_prone_core:2.19.1") {
        exclude("com.google.auto.service", "auto-service-annotations")
    }

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite.recipe:rewrite-github-actions:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-static-analysis:$rewriteVersion")
    implementation("org.openrewrite.gradle.tooling:model:$rewriteVersion")
    implementation("org.openrewrite:rewrite-templating:$rewriteVersion")

    implementation("commons-io:commons-io:2.+")
    implementation("org.apache.commons:commons-lang3:3.+")
    implementation("org.apache.maven.shared:maven-shared-utils:3.+")
    implementation("org.codehaus.plexus:plexus-utils:3.+")

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

    testImplementation("org.assertj:assertj-core:latest.release")

    testImplementation("com.google.guava:guava:29.0-jre")

    testImplementation("commons-codec:commons-codec:1.+")

    testRuntimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr353")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-core")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind")
    testRuntimeOnly("org.codehaus.groovy:groovy:latest.release")
    testRuntimeOnly(gradleApi())
}

// Add a source set for refaster rules that allows for Java 9+ syntax
val refaster by sourceSets.registering {
    java {
        val main = sourceSets.main.get()
        annotationProcessorPath += main.annotationProcessorPath
        compileClasspath += main.output + main.compileClasspath
        runtimeClasspath += main.output + main.runtimeClasspath
    }
}
sourceSets.named("test").configure {
    compileClasspath += refaster.get().output.classesDirs
    runtimeClasspath += refaster.get().output.classesDirs
}

tasks {
    jar {
        from(refaster.get().output)
    }
}
