![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
### Migrate to later Java versions. Automatically.

[![ci](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-migrate-java.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite.recipe/rewrite-migrate-java.svg)](https://mvnrepository.com/artifact/org.openrewrite.recipe/rewrite-migrate-java)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.openrewrite.org/scans)

### What is this?

This project implements a [Rewrite module](https://github.com/openrewrite/rewrite) that performs common tasks when migrating to new version of either Java and/or J2EE.  

# Java 

In releases of Java prior to Java 11, it was not uncommon for there to be more than three years between major releases
of the platform. This changed, in June 2018 when a new, six-month release cadence was adopted by the OpenJDK community.
The new model allows features to be released within any six-month window allowing features to be incremental introduced
when they are ready. Additionally, there are Java LTS (Long term support) releases on which there exists enterprise
support offered through several vendors that provide builds of the JVM, compiler, and standard libraries. The current
LTS versions of the Java platform (Java 8, 11, and 17) are the most common versions in use within the Java ecosystem.

# Java EE/Jakarta EE

The Java Platform, Enterprise Edition (Java EE) consists of a set of specifications that extend Java Standard Edition to
enable development of distributed applications and web services. Examples of the most commonly used parts of Java EE include
JAXB, JAX-WS, and the activation framework. These APIs and their associated reference implementations were bundled with
the Java standard library in JDK 6 through JDK 8, and deprecated in JDK 9. Starting with JDK 11, the libraries were
removed from the standard library to reduce the footprint of the Java standard library ([See JEP 320 for details](https://openjdk.org/jeps/320)).

**Any projects that continue to use the JAXB framework (on JDK 11+) must now explicitly add the JAXB API and a runtime implementation to their builds.**

To muddy  the waters further, the governance of the Java Platform, Enterprise Edition was transferred to the Eclipse foundation and was renamed to
Jakarta EE. The Jakarta EE 8 release (the first under the Jakarta name) maintains the `javax.xml.bind` package namespace whereas Jakarta EE 9
is the first release where the package namespace was changed to `jakarta.xml.bind`:

## Java Architecture for XML Binding (JAXB)

Java Architecture for XML Binding (JAXB) provides a framework for mapping XML documents to/from a Java representation of
those documents. The specification/implementation of this library that is bundled with older version of the JDK are part of
the Java EE specification, prior to being moved to the Jakarta project. It can be confusing because Java EE 8
and Jakarta EE 8 provide exactly the same specification (they use the same javax.xml.bind namespace) and there are
two different reference implementations for the specification. 

| Jakarta EE Version | XML Binding Artifact                        | Package Namespace | Description                      |
|--------------------|---------------------------------------------| ------------------| -------------------------------- |
| Java EE 8          | javax.xml.bind:jaxb-api:2.3.x               | javax.xml.bind    | JAXB API                         |
| Jakarta EE 8       | com.sun.xml.bind:jaxb-impl:2.3.x            | javax.xml.bind    | JAXB Reference Implementation    |
| Jakarta EE 8       | jakarta.xml.bind:jakarta.xml.bind-api:2.3.x | javax.xml.bind    | JAXB API                         |
| Jakarta EE 8       | org.glassfish.jaxb:jaxb-runtime:2.3.x       | javax.xml.bind    | JAXB Reference Implementation    |
| Jakarta EE 9       | jakarta.xml.bind:jakarta.xml.bind-api:3.x   | jakarta.xml.bind  | JAXB API                         |
| Jakarta EE 9       | org.glassfish.jaxb:jaxb-runtime:3.x         | jakarta.xml.bind  | JAXB Reference Implementation    |


## Java API for XML Web Services (JAX-WS)

Java API for XML Web Services (JAX-WS) provides a framework for building SOAP-based XML web services in Java. This framework was
originally part of the Java Platform, Enterprise Edition (J2EE) and both the API and the reference implementation were governed as
part of the J2EE specification.

| Jakarta EE Version | XML Web Services Artifact               | Package Namespace | Description                      |
|--------------------|-----------------------------------------|-------------------|----------------------------------|
| Java EE 8          | javax.xml.ws:jaxws-api:2.3.1            | javax.jws         | JAX-WS API                       |
| Jakarta EE 8       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x | javax.jws         | JAX-WS API                       |
| Jakarta EE 8       | com.sun.xml.ws:jaxws-rt:2.3.x           | javax.jws         | JAX-WS Reference Implementation  |
| Jakarta EE 9       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x | jakarta.jws       | JAX-WS API                       |
| Jakarta EE 9       | com.sun.xml.ws:jaxws-rt:2.3.x           | jakarta.jws       | JAX-WS Reference Implementation  |

# Java Migration Recipes

OpenRewrite provides a set of recipes that will help developers migrate to the to either Java 11 or Java 17. These two
LTS releases are the most common targets for organizations that are looking to modernize their applications.  

## Java 11 Migrations

OpenRewrite provides a set of recipes that will help developers migrate to Java 11 when their existing application
workloads are on Java 8 through 10. The biggest obstacles for the move to Java 11 are the introduction of the module system
(in Java 9) and the removal of J2EE libraries that were previously packaged with the core JDK.

The composite recipe for migrating to Java 11 `org.openrewrite.java.migrate.Java8toJava11` will allow developers to
migrate applications that were previous running on Java 8 through 10. This recipe covers the following themes:  

- Applications that use any of the Java EE specifications will have those dependencies migrated to Jakarta EE 8. Additionally,
  the migration to Jakarta EE 8 will also add explicit runtime dependencies on those projects that have transitive
  dependencies on the Jakarta EE APIs. **Currently, only Maven-based build files are supported.**
- Application that use maven plugins for generating source code from XSDs and WSDLs will have their plugins
  updated to use a version of the plugin that is compatible with Java 11.
- Any deprecated APIs in the earlier versions of Java that have a well-defined migration path will be automatically
  applied to an application's sources. The remediation included with this recipe were originally identified using
  a build plugin called [`Jdeprscan`](https://docs.oracle.com/javase/9/tools/jdeprscan.htm).
- Illegal Reflective Access warnings will be logged when an application attempts to use an API that has not been
  publically exported via the module system. This recipe will upgrade well-known, third-party libraries if they provide
  a version that is compliant with the Java module system. See [Illegal Reflective Access](#IllegalReflectiveAccess) for
  more information.

## Java 17 Migrations

OpenRewrite provides a set of recipes that will help developers migrate to Java 17 when their existing application
workloads are on Java 11 through 16. The composite recipe `org.openrewrite.java.migrate.UpgradeJava17` will cover the
following themes:

- Any deprecated APIs in the earlier versions of Java that have a well-defined migration path will be automatically
  applied to an application's sources. The remediation included with this recipe were originally identified using
  a build plugin called [`Jdeprscan`](https://docs.oracle.com/javase/9/tools/jdeprscan.htm).
- Illegal Reflective Access errors are fatal in Java 17 and will result in the application terminating when an application
  or a third-party library attempts to access an API that has not been publicly exported via the module system. This
  recipe will upgrade well-known, third-party libraries if they provide a version that is compliant with the Java module
  system.

## Illegal Reflective Access<a name="IllegalReflectiveAccess"></a>

The Java module system was introduced in Java 9 and provides a higher-level abstraction for grouping a set of java
packages and resources along with additional meta-data. The meta-data is used to identify what services the module offers,
what dependencies the module requires, and provides a mechanism for explicitly defining which module classes are
“visible” to Java classes that are external to the module.

The module system provides strong encapsulation and the core Java libraries, starting with Java 9, have been designed
to use the module specification. The rules of the module system, if strictly enforced, introduce breaking changes to
downstream projects that have not yet adopted the module system. In fact, it is very common for a typical Java
application to have a mix of module-compliant code along with code that is not aware of modules.

Even as Java has reached Java 15, there are a large number of applications and libraries that are not compliant with
the rules defined by the Java module system. Rather than breaking those libraries, the Java runtime has been configured
to allow mixed-use applications. If an application makes an illegal, reflective call to a module’s unpublished resource,
a warning will be logged.

The default behavior, starting with Java 11, is to log a warning the first time an illegal access call is made. All
subsequent calls will not be logged and the warning looks similar to the following:

```log
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.thoughtworks.xstream.core.util.Fields (file.....)
WARNING: Please consider reporting this to the maintainers of com.thoughtworks.xstream.core.util.Fields
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

This warning, while valid, produces noise in an organization's logging infrastructure. In Java 17, these types of issues
will are now fatal and the application will terminate if such illegal access occurs. 

### Suppressing Illegal Reflective Access Exceptions.

In situations where there a third-party library does not provide a version that is compliant with the Java module system,
it is possible to suppress these warnings/errors. An application may add `Add-Opens` declarations to it's top-level JAR's manifest:

```xml
<Add-Opens>java.base/java.lang java.base/java.util java.base/java.lang.reflect java.base/java.text java.desktop/java.awt.font</Add-Opens>
```

This solution will suppress the warnings and errors in the deployed artifacts while still surfacing the warning when developers run
the application from their development environments.

**NOTE: You cannot add these directives to a library that is transitively included by an application. The only place the
Java runtime will enforce the suppressions when they are applied to the top-level, executable Jar.**

There are currently no recipes that will automatically apply "<Add-Opens>" directives to Jar manifests.

## Helpful tools

- http://ibm.biz/WAMT4AppBinaries

# Jakarta EE 8 Migration Recipes
# Jakarta EE 9 Migration Recipes
