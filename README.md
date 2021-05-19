![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
### Migrate to Java 11. Automatically.

[![ci](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-migrate-java/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-migrate-java.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite.recipe/rewrite-migrate-java.svg)](https://mvnrepository.com/artifact/org.openrewrite.recipe/rewrite-migrate-java)

### What is this?

This project implements a [Rewrite module](https://github.com/openrewrite/rewrite) that performs common tasks when migrating from Java 8 to Java 11.

# Proposed Recipes

| Recipe Name | Description |
| ----------- | ----------- |
| [AddJdeprscanToMaven](#AddJdeprscanToMaven) | Add jdeprscan to the parent POM of project
| [AddJavaxLibrariesToMaven](#AddJavaxLibrariesToMaven) | Add javax-related dependencies to a Maven Build.
| [ConvertPrimitiveWrapperConstructors](#ConvertPrimitiveWrapperConstructors) | Convert Primitive Wrapper Class Constructors into `valueOf` methods.
| [ConvertBigDecimalRoundingMode](#ConvertBigDecimalRoundingMode) | Convert the use of BigDecimal rounding constants to their enum counterparts
| [SuppressIllegalReflectiveAccess](#SuppressIllegalReflectiveAccess) | Suppress Illegal reflective access warning via JAR manifest file 
| [UnsafeMigration](#UnsafeMigration) | Migrate the use of `sun.misc.Unsafe` to `VarHandle`

## Add `jdeprscan` plugin to Maven <a name="AddJdeprscanToMaven"></a>

There is a java tool called `jdeprscan` that will find uses of deprecated or removed APIs and this tool can be added to a project's build via a Maven plugin. In a multi-module project, this only needs to be added to the top-level, parent pom. This will fail the build if deprecated APIs are used.



The recipe would ensure the following plugin is present:
```
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
		.
		.
		.

	</build>
```

## Converting Primitive Wrapper Class Constructors to `valueOf` Method <a name="ConvertPrimitiveWrapperConstructors"></a>

The constructor of all primitive types has been deprecated in favor of using the static factory method `valueOf` available of each of the primitive types. This is a recipe to convert these constructors to their `valueOf` counterparts on the following classes: 

- Boolean
- Byte
- Character
- Double
- Float
- Long
- Short

## Adding `javax` Dependencies to Maven Poms <a name="AddJavaxLibrariesToMaven"></a>

There are several libraries that, prior to Java 11, were bundled with each release of JDK. However, starting in Java 11, these libraries are no longer part of the JDK release and any projects that were leveraging these libraries must now add them as additional dependencies. 

> **_NOTE:_** The governance of some of these projects has moved to the Jakarta EE project and this move includes some changes to the package names and we may need recipes that support both name spaces.
 
This will be a set of recipes that detects the use of these libraries and adds the appropriate dependencies to an existing project's Maven build.

These recipes will include migrations paths for

## JavaBeans Activation Framework

```
<properties>
	<jakara.activation.version>1.2.1</jakara.activation.version>
</properties>
...
<dependencies>
	<dependency>
		<groupId>com.sun.activation</groupId>
		<artifactId>jakarta.activation</artifactId>
		<version>${jakara.activation.version}</version>
	</dependency>
</dependencies>
```
## Java Architecture for XML Binding (JAXB)

```
<properties>
	<jakarta.xml.bind-api.version>2.3.3</jakarta.xml.bind-api.version>
</properties>

...

<dependencies>
	<!-- JAXB API -->
	<dependency>
		<groupId>jakarta.xml.bind</groupId>
		<artifactId>jakarta.xml.bind-api</artifactId>
		<version>${jakarta.xml.bind-api.version}</version>
	</dependency>
	<!-- JAXB runtime -->
	<dependency>
		<groupId>com.sun.xml.bind</groupId>
		<artifactId>jaxb-impl</artifactId>
		<version>3.0.0</version>
		<scope>runtime</scope>
	</dependency>	
</dependencies>
```

There is also a maven plugin for generating code from xsds and this tool also relies on the activation framework.

```
<properties>
		<maven-jaxb2-plugin.version>0.14.0</maven-jaxb2-plugin.version>
		<jaxb-xjc.version>2.3.3</jaxb-xjc.version>
</properties>

<!-- Configuring the plugin for generating java model from xsds. -->
<plugin>
	<groupId>org.jvnet.jaxb2.maven2</groupId>
	<artifactId>maven-jaxb2-plugin</artifactId>
	<version>${maven-jaxb2-plugin.version}</version>
	<dependencies>
		<dependency>
			<groupId>com.sun.xml.bind</groupId>
			<artifactId>jaxb-xjc</artifactId>
			<version>${jaxb-xjc.version}</version>
		</dependency>
		<dependency>
			<groupId>com.sun.activation</groupId>
			<artifactId>jakarta.activation</artifactId>
			<version>${jakara.activation.version}</version>
		</dependency>
	</dependencies>
</plugin>

```

## Java API for XML Web Services (JAXWS)

```
<properties>
	<jaxws-rt.version>2.3.2-1</jaxws-rt.version>
</properties>

...

<dependencies>
	<dependency>
		<groupId>com.sun.xml.ws</groupId>
		<artifactId>jaxws-rt</artifactId>
		<version>${jaxws-rt.version}</version>
	</dependency>
</dependencies>
```

There is also a maven plugin for generating code from WSDLs that also may be impacted. 

```
<properties>
	<jaxws-maven-plugin.version>2.6</jaxws-maven-plugin.version>
</properties>

...

<!-- Configuring the plugin for generating java model from Soap WSDLs -->
<plugin>
	<groupId>org.codehaus.mojo</groupId>
	<artifactId>jaxws-maven-plugin</artifactId>
	<version>${jaxws-maven-plugin.version}</version>
	<dependencies>
		<dependency>
			<groupId>com.sun.xml.ws</groupId>
			<artifactId>jaxws-tools</artifactId>
			<version>${jaxws-rt.version}</version>
		</dependency>
	</dependencies>
</plugin>

```

## Other libraries that may need to be added
- java.corba
- java.transaction
- JavaFX
- Web Start

## Converting BigDecimal Rounding Constants to Enums <a name="ConvertBigDecimalRoundingMode"></a>

There are a set of constants (static public integers) representing the various rounding strategies for the BigDecimal class. These have been deprecated in favor of an enum. Any use of these constants will result in a breaking change when using the `jdeprscan` tool. This recipe will migrate the use of these constants to their enum counterparts.


Methods in the `BigDecimal` classes that will be migrated: 

- `divide(BigDecimal, int)` --> `divide(BigDecimal, RoundingMode)`
- `divide(BigDecimal, int, int)` --> `divide(BigDecimal, int, RoundingMode)`
- `setScale(int, int)` --> `setScale(int, RoundingMode)`

## Suppress Illegal Reflective Access <a name="SuppressIllegalReflectiveAccess"></a>

The Java module system was introduced in Java 9 and provides a higher-level abstraction for grouping a set of java packages and resources along with additional meta-data. The meta-data is used to identify what services the module offers, what dependencies the module requires, and provides a mechanism for explicitly defining which module classes are “visible” to Java classes that are external to the module.

The module system provides strong encapsulation and the core Java libraries, starting with Java 9, have been designed to use the module specification. The rules of the module system, if strictly enforced, introduce breaking changes to downstream projects that have not yet adopted the module system. In fact, it is very common for a typical Java application to have a mix of module-compliant code along with code that is not aware of modules.

Even as Java has reached Java 15, there are a large number of applications and libraries that are not compliant with the rules defined by the Java module system. Rather than breaking those libraries, the Java runtime has been configured to allow mixed-use applications. If an application makes an illegal, reflective call to a module’s unpublished resource, a warning will be logged. 

The default behavior of the Java runtime is to log a warning the first time an illegal access call is made. All subsequent calls will not be logged and the warning looks similar to the following:

```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.thoughtworks.xstream.core.util.Fields (file.....)
WARNING: Please consider reporting this to the maintainers of com.thoughtworks.xstream.core.util.Fields
WARNING: Use --illage-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

This warning, while valid, produces noise in an organization's logging infrastructure and can be suppressed when packaging an application into an executable JAR file by adding the following to the JAR's manifest: 

```
<Add-Opens>java.base/java.lang java.base/java.util java.base/java.lang.reflect java.base/java.text java.desktop/java.awt.font</Add-Opens>
```

This solution will suppress the warning in the deployed artifacts while still surfacing the warning when developers run the application from their development environments.

This recipe can be used to add or modify the `maven-jar-plugin` to add manifest warnings. It will need to target pom files that build FAT application JARs or WAR files. The recipe can be configured with a list of packages for the `<Add-Opens>` manifest entries:

```
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


## Other deprecated/removed packages and classes: 

- com.sun.awt.AWTUtilities
- com.sun.image.codec.jpeg.*
- com.sun.browser.plugin2.DOM
- com.sun.security.auth.*
- com.suh.xml.internal.bind.*
- java.awt.dnd.peer.*
- java.awt.peer.*
- javax.security.auth.Policy
- sun.misc.BASE64Decoder
- sun.misc.BASE64Encoder
- sun.plugin.dom.DOMObject
- java.lang.Runtime.getLocalizedInputStream()
- java.lang.Runtime.getLocalizedOutputStream()
- java.lang.Runtime.runFinalizersOnExit()
- java.lang.SecurityManager.checkAwtEventQueueAccess()
- java.lang.SecurityManager.checkMemberAccess()
- java.lang.SecurityManager.checkSystemClipboardAccess()
- java.lang.SecurityManager.checkTopLevelWindow()
- java.lang.SecurityManager.classDepth()
- java.lang.SecurityManager.classLoaderDepth()
- java.lang.SecurityManager.currentClassLoader()
- java.lang.SecurityManager.currentLoadedClass()
- java.lang.SecurityManager.getInCheck()
- java.lang.SecurityManager.inClass()
- java.lang.SecurityManager.inClassLoader()



## Helpful tools

- http://ibm.biz/WAMT4AppBinaries
