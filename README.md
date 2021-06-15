![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
### Migrate to Java 11. Automatically.

[![ci](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-migrate-java.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite.recipe/rewrite-migrate-java.svg)](https://mvnrepository.com/artifact/org.openrewrite.recipe/rewrite-migrate-java)

### What is this?

This project implements a [Rewrite module](https://github.com/openrewrite/rewrite) that performs common tasks when migrating from Java 8 to Java 11.

# Java 11 Migration Recipes

| Recipe Name | Description |
| ----------- | ----------- |
| [Add XML binding dependencies](#AddXmlBindingDependencies)   | Add or update JAXB dependencies for a Maven build.
| [Add XML Web Services dependencies](#AddXmlWebServiceDependencies) | Add or update JAX-WS dependencies for a Maven build.
| [ConvertPrimitiveWrapperConstructors](#ConvertPrimitiveWrapperConstructors) | Convert Primitive Wrapper Class Constructors into `valueOf` methods.
| [ConvertBigDecimalRoundingMode](#ConvertBigDecimalRoundingMode) | Convert the use of BigDecimal rounding constants to their enum counterparts.
| [Remediate deprecation warnings and errors](#AddressDeprecations) | Provide remediation for deprecations highlighted by `jdeprscan`.
| [Migrate XML binding framework to Jakarta namespace](#MigrateXmlBindingToJakarta) | Migrate XML binding framework from `javax.xml.bind.*` to `jakarta.xml.bind.*`
| [Migrate Java API for XML Web Services to Jakarta namespace](#MigrateXmlWebServicesToJakarta) | Migrate Java API for XML Web Services from `javax.jws.*` to `jakarta.jws.*`
| [Add `jdeprscan` plugin to Maven](#AddJdeprscanToMaven) | Add the `jdeprscan` maven plugin to a Maven build.
| [SuppressIllegalReflectiveAccess](#SuppressIllegalReflectiveAccess) | Suppress Illegal reflective access warning via JAR manifest file
| [UnsafeMigration](#UnsafeMigration) | Migrate the use of `sun.misc.Unsafe` to `VarHandle`


## Java Architecture for XML Binding (JAXB)

Java Architecture for XML Binding (JAXB) provides a framework for mapping XML documents to/from a Java representation of those documents.
This framework was originally part of the Java Platform, Enterprise Edition (J2EE) and both the API and the reference implementation
were governed as part of the J2EE specification. The framework was originally developed when XML-based web services were popular and it
made sense at the time to include the JAXB API and reference implementation as part of the Java standard library (JDK 6 through 8). Starting
with JDK 9, the libraries were removed from the standard library to reduce the footprint of the Java standard library. Any projects that
continue to use the JAXB framework (on JDK 9+) must now explicitly add the JAXB API and a runtime implementation to their builds. To muddy
the waters further, the governance of the Java Platform, Enterprise Edition was transferred to the Eclipse foundation and was renamed to
Jakarta EE. The Jakarta EE 8 release (the first under the Jakarta name) maintains the `javax.xml.bind` package namespace whereas Jakarta EE 9
is the first release where the package namespace was changed to `jakarta.xml.bind`:

| Jakarta EE Version | XML Binding Artifact                         | Package Namespace | Description                      |
| ------------------ | -------------------------------------------  | ------------------| -------------------------------- |
| Jakarta EE 8       | jakarta.xml.bind:jakarata.xml.bind-api:2.3.x | javax.xml.bind    | JAXB API                         |
| Jakarta EE 8       | org.glassfish.jaxb:jaxb-runtime:2.3.x        | javax.xml.bind    | JAXB Reference Implementation    |
| Jakarta EE 9       | jakarta.xml.bind:jakarata.xml.bind-api:3.x   | jakarta.xml.bind  | JAXB API                         |
| Jakarta EE 9       | org.glassfish.jaxb:jaxb-runtime:3.x          | jakarta.xml.bind  | JAXB Reference Implementation    |


There are two JAXB-related recipes in this project that help projects that are migrating to Java 11:

### Add XML binding dependencies<a name="AddXmlBindingDependencies"></a>

This recipe will add or update the latest 2.3.x versions of the Jakarta XML binding framework for an existing Maven project.
This recipe will only add the dependencies if references to types within the `javax.xml.bind` package namespace are detected.

```xml
<dependencies>
    <!-- JAXB API -->
    <dependency>
        <groupId>jakarta.xml.bind</groupId>
        <artifactId>jakarta.xml.bind-api</artifactId>
        <version>2.3.3</version>
    </dependency>
    <!-- JAXB runtime -->
    <dependency>
        <groupId>org.glassfish.jaxb</groupId>
        <artifactId>jaxb-runtime</artifactId>
        <version>2.3.4</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

There is also a maven plugin for generating code from xsds and this plugin must also be updated. [See this link for details](https://github.com/mojohaus/jaxb2-maven-plugin/issues/43)

```xml

<!-- Configuring the plugin for generating java model from xsds. -->
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>2.5.0</version>
</plugin>
```

### Migrate XML binding framework from `javax.xml.bind` to `jakarta.xml.bind`<a name="MigrateXmlBindingToJakarta"></a>

This recipe will migrate java projects that are using the XML binding framework from the `javax.xml.bind` package namespace
to the new 'jakarta.xml.bind' namespace. The recipe will also add/update the latest 3.x versions of the Jakarta XML binding
framework for maven-based builds.

> **_NOTE:_** The `maven-jaxb2-plugin` does not yet support the jakarta.xml.bind namespace.

## Java API for XML Web Services (JAX-WS)

Java API for XML Web Services (JAX-WS) provides a framework for building SOAP-based XML web services in Java. This framework was
originally part of the Java Platform, Enterprise Edition (J2EE) and both the API and the reference implementation were governed as
part of the J2EE specification. The framework was originally developed when XML-based web services were popular and it made sense
at the time to include the JAX-WS API and reference implementation as part of the Java standard library (JDK 6 through 8). Starting
with JDK 9, the libraries were removed from the standard library to reduce the footprint of the Java standard library. Any projects that
continue to use the JAX-WS framework (on JDK 9+) must now explicitly add the JAX-WS API and a runtime implementation to their builds.
To muddy the waters further, the governance of the Java Platform, Enterprise Edition was transferred to the Eclipse foundation and was
renamed to Jakarta EE. The Jakarta EE 8 release (the first under the Jakarta name) maintains the `javax.jws` package namespace whereas
Jakarta EE 9 is the first release where the package namespace was changed to `jakarta.jws`:

| Jakarta EE Version | XML Web Services Artifact                    | Package Namespace | Description                      |
| ------------------ | -------------------------------------------  | ------------------| -------------------------------- |
| Jakarta EE 8       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x      | javax.jws         | JAX-WS API                       |
| Jakarta EE 8       | com.sun.xml.ws:jaxws-rt:2.3.x                | javax.jws         | JAX-WS Reference Implementation  |
| Jakarta EE 9       | jakarta.xml.ws:jakarta.xml.ws-api:2.3.x      | jakarta.jws       | JAX-WS API                       |
| Jakarta EE 9       | com.sun.xml.ws:jaxws-rt:2.3.x                | jakarta.jws       | JAX-WS Reference Implementation  |


There are two JAX-WS recipes in this project that help projects that are migrating to Java 11:

### Add XML Web Services dependencies<a name="AddXmlWebServiceDependencies"></a>

This recipe will add or update the latest 2.3.x versions of the Jakarta XML Web Service dependencies for an existing Maven project.
This recipe will only add the dependencies if references to types within the `javax.jws` package namespace are detected.

```xml
<dependencies>
    <!-- JAX-WS API -->
    <dependency>
        <groupId>jakarta.xml.ws</groupId>
        <artifactId>jakarta.xml.ws-api</artifactId>
        <version>2.3.3</version>
    </dependency>
    <!-- JAX-WS reference implementation -->
    <dependency>
        <groupId>com.sun.xml.ws</groupId>
        <artifactId>jaxws-rt</artifactId>
        <version>2.3.4</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

There are also several maven plugin for generating code from WSDL and these plugins must also be updated.

### Migrate XML Web Services framework from `javax.jws` to `jakarta.jws`<a name="MigrateXmlWebServicesToJakarta"></a>

This recipe will migrate java projects that are using the XML web services framework from the `javax.jws` package namespace
to the new 'jakarta.jws' namespace. The recipe will also add/update the latest 3.x versions of the Jakarta XML Web Services
framework for maven-based builds.

> **_NOTE:_** The `maven-jaxb2-plugin` does not yet support the jakarta.xml.bind namespace.


## Converting Primitive Wrapper Class Constructors to `valueOf` Method <a name="ConvertPrimitiveWrapperConstructors"></a>

The constructor of all primitive types has been deprecated in favor of using the static factory method `valueOf` available of each of the primitive types. This is a recipe to convert these constructors to their `valueOf` counterparts on the following classes:

- Boolean
- Byte
- Character
- Double
- Float
- Long
- Short

## Converting BigDecimal Rounding Constants to Enums <a name="ConvertBigDecimalRoundingMode"></a>

There are a set of constants (static public integers) representing the various rounding strategies for the BigDecimal class. These have been deprecated in favor of an enum. Any use of these constants will result in a breaking change when using the `jdeprscan` tool. This recipe will migrate the use of these constants to their enum counterparts.

Methods in the `BigDecimal` classes that will be migrated:

- `divide(BigDecimal, int)` --> `divide(BigDecimal, RoundingMode)`
- `divide(BigDecimal, int, int)` --> `divide(BigDecimal, int, RoundingMode)`
- `setScale(int, int)` --> `setScale(int, RoundingMode)`

## Remediate deprecation warnings and errors<a name="AddressDeprecations"></a>

The `jdeprscan` tool provides a list of deprecations that have been made to the Java language. This project does a
best faith effort to remediate listed deprecations where a safe, automated solution exists. 

## Additional recipes for adding libraries that have been removed from the Java runtime 

There are several additional libraries that have been removed from the Java standard library that should be considered
when migrating to Java 11:

- java.corba
- javax.transaction
- javax.batch 
- JavaFX
- Web Start

## Add `jdeprscan` plugin to Maven <a name="AddJdeprscanToMaven"></a>

There is a java tool called `jdeprscan` that will find uses of deprecated or removed APIs and this tool can be added to a project's build via a Maven plugin. In a multi-module project, this only needs to be added to the top-level, parent pom. This will fail the build if deprecated APIs are used.

The recipe would ensure the following plugin is present:

```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jdeprscan-plugin</artifactId>
                <version>3.0.0-alpha-1</version>
                <configuration>
                    <release>11</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Suppress Illegal Reflective Access <a name="SuppressIllegalReflectiveAccess"></a>

The Java module system was introduced in Java 9 and provides a higher-level abstraction for grouping a set of java packages and resources along with additional meta-data. The meta-data is used to identify what services the module offers, what dependencies the module requires, and provides a mechanism for explicitly defining which module classes are “visible” to Java classes that are external to the module.

The module system provides strong encapsulation and the core Java libraries, starting with Java 9, have been designed to use the module specification. The rules of the module system, if strictly enforced, introduce breaking changes to downstream projects that have not yet adopted the module system. In fact, it is very common for a typical Java application to have a mix of module-compliant code along with code that is not aware of modules.

Even as Java has reached Java 15, there are a large number of applications and libraries that are not compliant with the rules defined by the Java module system. Rather than breaking those libraries, the Java runtime has been configured to allow mixed-use applications. If an application makes an illegal, reflective call to a module’s unpublished resource, a warning will be logged.

The default behavior of the Java runtime is to log a warning the first time an illegal access call is made. All subsequent calls will not be logged and the warning looks similar to the following:

```log
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.thoughtworks.xstream.core.util.Fields (file.....)
WARNING: Please consider reporting this to the maintainers of com.thoughtworks.xstream.core.util.Fields
WARNING: Use --illage-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

This warning, while valid, produces noise in an organization's logging infrastructure and can be suppressed when packaging an application into an executable JAR file by adding the following to the JAR's manifest:

```xml
<Add-Opens>java.base/java.lang java.base/java.util java.base/java.lang.reflect java.base/java.text java.desktop/java.awt.font</Add-Opens>
```

This solution will suppress the warning in the deployed artifacts while still surfacing the warning when developers run the application from their development environments.

This recipe can be used to add or modify the `maven-jar-plugin` to add manifest warnings. It will need to target pom files that build FAT application JARs or WAR files. The recipe can be configured with a list of packages for the `<Add-Opens>` manifest entries:

```xml
<!-- This is to suppress Java 11 reflective access warnings when running the application from a jar file. -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifestEntries>
                <Add-Opens>java.base/java.lang java.base/java.util java.base/java.lang.reflect java.base/java.text java.desktop/java.awt.font</Add-Opens>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

## Migrate the use of `sun.misc.Unsafe`<a name="UnsafeMigration"></a>

The `sun.misc.Unsafe` class provides direct access to CPU and other hardware features. Over the years this class, although meant for only internal use by the JDK core, has been used in many frameworks across the Java-eco system to access its features primarily for performance gains. [This is an excellent article that desribes how the `Unsafe` class has been used over the years and also details how alternatives are being added to the platform.](https://blogs.oracle.com/javamagazine/the-unsafe-class-unsafe-at-any-speed)

It will be possible to create a set of recipes to aid in the migration of `Unsafe` to the new APIs starting in Java 11.

## Helpful tools

- http://ibm.biz/WAMT4AppBinaries
