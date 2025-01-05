plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    id("org.openrewrite.build.moderne-source-available-license") version "latest.release"
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

    runtimeOnly("tech.picnic.error-prone-support:error-prone-contrib:latest.release:recipes")

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
    testImplementation("org.threeten:threeten-extra:1.8.0")

    testRuntimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr353")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-core")
    testRuntimeOnly("com.fasterxml.jackson.core:jackson-databind")
    testRuntimeOnly("com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    testRuntimeOnly("commons-logging:commons-logging:1.3.2")
    testRuntimeOnly("org.apache.logging.log4j:log4j-api:2.23.1")
    testRuntimeOnly("org.apache.johnzon:johnzon-core:1.2.18")
    testRuntimeOnly("org.codehaus.groovy:groovy:latest.release")
    testRuntimeOnly("org.jboss.logging:jboss-logging:3.6.0.Final")
    testRuntimeOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    testRuntimeOnly("org.springframework:spring-core:6.1.13")
    testRuntimeOnly("com.google.code.findbugs:jsr305:3.0.2")
    testRuntimeOnly(gradleApi())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType(Javadoc::class.java) {
    exclude("**/PlanJavaMigration.java")
}

tasks.test {
    maxHeapSize = "2g"  // Set max heap size to 2GB or adjust as necessary
}
