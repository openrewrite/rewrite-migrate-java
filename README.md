<p align="center">
  <a href="https://docs.openrewrite.org">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-dark.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg">
      <img alt="OpenRewrite Logo" src="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg" width='600px'>
    </picture>
  </a>
</p>

<div align="center">
  <h1>rewrite-migrate-java</h1>
</div>

<div align="center">

<!-- Keep the gap above this line, otherwise they won't render correctly! -->
[![ci](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite.recipe/rewrite-migrate-java.svg)](https://mvnrepository.com/artifact/org.openrewrite.recipe/rewrite-migrate-java)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.openrewrite.org/scans)
[![Contributing Guide](https://img.shields.io/badge/Contributing-Guide-informational)](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md)
</div>

## What is this?

This project implements an [OpenRewrite](https://github.com/openrewrite/rewrite) module with recipes for migrating to newer versions of Java and Jakarta EE. It supports both Maven and Gradle projects.

Browse [all available recipes in the recipe catalog](https://docs.openrewrite.org/recipes/java/migrate).

## Java version migration

Java adopted a six-month release cadence in 2018, with Long-Term Support (LTS) releases every two years. The current LTS versions are **Java 8, 11, 17, 21, and 25**.

This module provides composite migration recipes for each LTS version:

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.java.migrate.Java8toJava11` | Migrate from Java 8 to 11 |
| `org.openrewrite.java.migrate.UpgradeToJava17` | Migrate from Java 11+ to 17 |
| `org.openrewrite.java.migrate.UpgradeToJava21` | Migrate from Java 17+ to 21 |
| `org.openrewrite.java.migrate.UpgradeToJava25` | Migrate from Java 21+ to 25 |

Each composite recipe builds on the previous one, so `UpgradeToJava25` includes all changes from `UpgradeToJava21`, which includes those from `UpgradeToJava17`, and so on. These recipes handle:

- Replacing deprecated APIs with their modern equivalents
- Updating build files to target the new Java version
- Upgrading build plugins to versions compatible with the target Java version
- Upgrading third-party libraries to versions compatible with the Java module system
- Adopting new language features (e.g., text blocks, pattern matching, sequenced collections, switch expressions)

## Jakarta EE migration

When Java EE governance transferred to the Eclipse Foundation, it was renamed Jakarta EE. The key migration milestones are:

| Version | Key change |
|---------|-----------|
| **Jakarta EE 8** | Same APIs as Java EE 8, under the `javax` namespace |
| **Jakarta EE 9** | Package namespace changed from `javax.*` to `jakarta.*` |
| **Jakarta EE 10** | Updated API versions on the `jakarta` namespace |
| **Jakarta EE 11** | Latest API versions |

This module provides comprehensive migration recipes for each version:

| Recipe | Description |
|--------|-------------|
| `org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta` | Migrate to Jakarta EE 9 |
| `org.openrewrite.java.migrate.jakarta.JakartaEE10` | Migrate to Jakarta EE 10 |
| `org.openrewrite.java.migrate.jakarta.JakartaEE11` | Migrate to Jakarta EE 11 |

These recipes cover the full breadth of Jakarta EE specifications, including Servlet, JPA, EJB, CDI, Bean Validation, JSON-B, JAX-RS, WebSocket, Mail, JMS, and more. They also update related third-party libraries (Jackson, Jetty, EhCache, EclipseLink, etc.) to Jakarta-compatible versions.

### JAXB and JAX-WS reference

Two of the most commonly encountered Jakarta EE migration challenges are JAXB (XML binding) and JAX-WS (web services), because these APIs were bundled with the JDK through Java 8, deprecated in Java 9, and removed in Java 11 ([JEP 320](https://openjdk.org/jeps/320)). Projects using these APIs on Java 11+ must add explicit dependencies.

<details>
<summary>JAXB artifact mapping</summary>

| Jakarta EE Version | Artifact                                    | Package Namespace | Description                   |
|--------------------|---------------------------------------------|-------------------|-------------------------------|
| Java EE 8          | javax.xml.bind:jaxb-api:2.3.x               | javax.xml.bind    | JAXB API                      |
| Jakarta EE 8       | com.sun.xml.bind:jaxb-impl:2.3.x            | javax.xml.bind    | JAXB Reference Implementation |
| Jakarta EE 8       | jakarta.xml.bind:jakarta.xml.bind-api:2.3.x | javax.xml.bind    | JAXB API                      |
| Jakarta EE 8       | org.glassfish.jaxb:jaxb-runtime:2.3.x       | javax.xml.bind    | JAXB Reference Implementation |
| Jakarta EE 9       | jakarta.xml.bind:jakarta.xml.bind-api:3.x   | jakarta.xml.bind  | JAXB API                      |
| Jakarta EE 9       | org.glassfish.jaxb:jaxb-runtime:3.x         | jakarta.xml.bind  | JAXB Reference Implementation |

</details>

<details>
<summary>JAX-WS artifact mapping</summary>

| Jakarta EE Version | Artifact                                 | Package Namespace | Description                     |
|--------------------|------------------------------------------|-------------------|---------------------------------|
| Java EE 8          | javax.xml.ws:jaxws-api:2.3.1            | javax.jws         | JAX-WS API                      |
| Jakarta EE 8       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x | javax.jws         | JAX-WS API                      |
| Jakarta EE 8       | com.sun.xml.ws:jaxws-rt:2.3.x           | javax.jws         | JAX-WS Reference Implementation |
| Jakarta EE 9       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x | jakarta.jws       | JAX-WS API                      |
| Jakarta EE 9       | com.sun.xml.ws:jaxws-rt:2.3.x           | jakarta.jws       | JAX-WS Reference Implementation |

</details>

## Other recipes

Beyond Java and Jakarta EE version migrations, this module also includes recipes for:

- **Guava** — Replace Guava utilities with Java standard library equivalents
- **Lombok** — Best practices and modernization
- **JSpecify** — Adopt [JSpecify](https://jspecify.dev/) nullability annotations
- **DataNucleus** — Upgrade DataNucleus versions

## Contributing

We appreciate all types of contributions. See the [contributing guide](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md) for detailed instructions on how to get started.

## Licensing

For more information about licensing, please visit our [licensing page](https://docs.openrewrite.org/licensing/openrewrite-licensing).
