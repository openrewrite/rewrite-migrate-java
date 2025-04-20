/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;


class IBMSemeruTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal.new"))
          .recipeFromResource("/META-INF/rewrite/ibm-java.yml", "org.openrewrite.java.migrate.IBMSemeru");
    }

    @DocumentExample
    @Test
    void doNotUseSunNetSslInternalWwwProtocolHttpsHandler() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this

              class Foo{
                void bar() {
                    com.sun.net.ssl.internal.www.protocol.https.Handler handler_1 =           //flag
                        new com.sun.net.ssl.internal.www.protocol.https.Handler();            //flag
                    Handler handler_2 =   new Handler("String", 1); //flag (2)
                    testMethod(handler_1);
                    testMethod(handler_2);
                    if (handler_1 instanceof com.sun.net.ssl.internal.www.protocol.https.Handler){ //flag
                        //do nothing
                    }

                    if (handler_1 instanceof Handler){ //flag
                        //do nothing
                    }
                }

                public static com.sun.net.ssl.internal.www.protocol.https.Handler testMethod(Handler handler){ //flag (2)
                    return handler;
                }
              }
              """,
            """
              import com.ibm.net.ssl.www2.protocol.https.Handler;
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this

              class Foo{
                void bar() {
                    Handler handler_1 =           //flag
                        new Handler();            //flag
                    Handler handler_2 =   new Handler("String", 1); //flag (2)
                    testMethod(handler_1);
                    testMethod(handler_2);
                    if (handler_1 instanceof Handler){ //flag
                        //do nothing
                    }

                    if (handler_1 instanceof Handler){ //flag
                        //do nothing
                    }
                }

                public static Handler testMethod(Handler handler){ //flag (2)
                    return handler;
                }
              }
              """
          )
        );
    }

    @Test
    void doNotUseSunNetSslInternalWwwProtocol() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo{
                private String flagMe = "com.sun.net.ssl.internal.www.protocol"; //flag this

                void bar() {
                    System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol"); //flag this
                    String s1 = "com.sun.net.ssl";                              //DO NOT FLAG
                    String s2 = "com.sun.net.ssl.internal";                     //DO NOT FLAG
                    String s3 = "com.sun.net.ssl.internal.ssl";                 //DO NOT FLAG
                    String s4 = "com.sun.net.ssl.internal.www";                 //DO NOT FLAG
                    String s5 = "com.sun.net.ssl.internal.www.protocol";        //flag this
                    String s6 = "com.sun.net.ssl.internal.www.protocol.https";  //DO NOT FLAG
                }
              }
              """,
            """
              class Foo{
                private String flagMe = "com.ibm.net.ssl.www2.protocol"; //flag this

                void bar() {
                    System.setProperty("java.protocol.handler.pkgs", "com.ibm.net.ssl.www2.protocol"); //flag this
                    String s1 = "com.sun.net.ssl";                              //DO NOT FLAG
                    String s2 = "com.sun.net.ssl.internal";                     //DO NOT FLAG
                    String s3 = "com.sun.net.ssl.internal.ssl";                 //DO NOT FLAG
                    String s4 = "com.sun.net.ssl.internal.www";                 //DO NOT FLAG
                    String s5 = "com.ibm.net.ssl.www2.protocol";        //flag this
                    String s6 = "com.sun.net.ssl.internal.www.protocol.https";  //DO NOT FLAG
                }
              }
              """
          )
        );
    }

    @Test
    void doNotUseSunNetSslInternalSslProvider() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule

              class TestClass_2{
                void bar() {
                    Provider provider_4 = new Provider();  // flag (2)
                }

                private void fdsa( Provider p1 ){} // flag
              }
              """,
            """
              import com.ibm.jsse2.IBMJSSEProvider2;
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule

              class TestClass_2{
                void bar() {
                    IBMJSSEProvider2 provider_4 = new IBMJSSEProvider2();  // flag (2)
                }

                private void fdsa( IBMJSSEProvider2 p1 ){} // flag
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedPackage() {
        rewriteRun(
          version(
            //language=java
            java("""
                import com.sun.net.ssl.HostnameVerifier;
                import com.sun.net.ssl.HttpsURLConnection;
                import com.sun.net.ssl.KeyManager;
                import com.sun.net.ssl.KeyManagerFactory;
                import com.sun.net.ssl.KeyManagerFactorySpi;
                import com.sun.net.ssl.SSLContext;
                import com.sun.net.ssl.SSLContextSpi;
                import com.sun.net.ssl.SSLPermission;
                import com.sun.net.ssl.TrustManager;
                import com.sun.net.ssl.TrustManagerFactory;
                import com.sun.net.ssl.TrustManagerFactorySpi;
                import com.sun.net.ssl.X509KeyManager;
                import com.sun.net.ssl.X509TrustManager;

                class TestFullyQualifiedPackage {
                    com.sun.net.ssl.HostnameVerifier hv;
                    com.sun.net.ssl.HttpsURLConnection huc;
                    com.sun.net.ssl.KeyManager km;
                    com.sun.net.ssl.KeyManagerFactory kmf;
                    com.sun.net.ssl.KeyManagerFactorySpi kmfs;
                    com.sun.net.ssl.SSLContext sslc;
                    com.sun.net.ssl.SSLContextSpi sslcs;
                    com.sun.net.ssl.SSLPermission sslp;
                    com.sun.net.ssl.TrustManager tm;
                    com.sun.net.ssl.TrustManagerFactory tmf;
                    com.sun.net.ssl.TrustManagerFactorySpi tmfs;
                    com.sun.net.ssl.X509KeyManager x509km;
                    com.sun.net.ssl.X509TrustManager xtm;

                    HostnameVerifier hv2;
                    HttpsURLConnection huc2;
                    KeyManager km2;
                    KeyManagerFactory kmf2;
                    KeyManagerFactorySpi kmfs2;
                    SSLContext sslc2;
                    SSLContextSpi sslcs2;
                    SSLPermission sslp2;
                    TrustManager tm2;
                    TrustManagerFactory tmf2;
                    TrustManagerFactorySpi tmfs2;
                    X509KeyManager x509km2;
                    X509TrustManager xtm2;
                }
                """,
              """
                import javax.net.ssl.HostnameVerifier;
                import javax.net.ssl.HttpsURLConnection;
                import javax.net.ssl.KeyManager;
                import javax.net.ssl.KeyManagerFactory;
                import javax.net.ssl.KeyManagerFactorySpi;
                import javax.net.ssl.SSLContext;
                import javax.net.ssl.SSLContextSpi;
                import javax.net.ssl.SSLPermission;
                import javax.net.ssl.TrustManager;
                import javax.net.ssl.TrustManagerFactory;
                import javax.net.ssl.TrustManagerFactorySpi;
                import javax.net.ssl.X509KeyManager;
                import javax.net.ssl.X509TrustManager;

                class TestFullyQualifiedPackage {
                    javax.net.ssl.HostnameVerifier hv;
                    javax.net.ssl.HttpsURLConnection huc;
                    javax.net.ssl.KeyManager km;
                    javax.net.ssl.KeyManagerFactory kmf;
                    javax.net.ssl.KeyManagerFactorySpi kmfs;
                    javax.net.ssl.SSLContext sslc;
                    javax.net.ssl.SSLContextSpi sslcs;
                    javax.net.ssl.SSLPermission sslp;
                    javax.net.ssl.TrustManager tm;
                    javax.net.ssl.TrustManagerFactory tmf;
                    javax.net.ssl.TrustManagerFactorySpi tmfs;
                    javax.net.ssl.X509KeyManager x509km;
                    javax.net.ssl.X509TrustManager xtm;

                    HostnameVerifier hv2;
                    HttpsURLConnection huc2;
                    KeyManager km2;
                    KeyManagerFactory kmf2;
                    KeyManagerFactorySpi kmfs2;
                    SSLContext sslc2;
                    SSLContextSpi sslcs2;
                    SSLPermission sslp2;
                    TrustManager tm2;
                    TrustManagerFactory tmf2;
                    TrustManagerFactorySpi tmfs2;
                    X509KeyManager x509km2;
                    X509TrustManager xtm2;
                }
                """
            ), 6)
        );
    }

    @Test
    void hostnameVerifier() {
        rewriteRun(
          version(
            //language=java
            java("""
                import com.sun.net.ssl.HostnameVerifier;

                class TestHostnameVerifier implements HostnameVerifier {
                    public boolean verify(String arg0, String arg1) {
                        return false;
                    }
                }
                """,
              """
                import javax.net.ssl.HostnameVerifier;

                class TestHostnameVerifier implements HostnameVerifier {
                    public boolean verify(String arg0, String arg1) {
                        return false;
                    }
                }
                """
            ), 6)
        );
    }

    @Test
    void removeMavenXMLWSModuleDependency() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycom.myapp</groupId>
                <artifactId>myapp</artifactId>
                <version>2.0.0</version>
                <packaging>war</packaging>
                <name>MyApp</name>
                <properties>
                  <maven.compiler.target>1.8</maven.compiler.target>
                  <maven.compiler.source>1.8</maven.compiler.source>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.version>2.0.0</project.version>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.code.gson</groupId>
                    <artifactId>gson</artifactId>
                    <version>2.10.1</version>
                  </dependency>
                  <dependency>
                        <groupId>javax.xml.ws</groupId>
                        <artifactId>jaxws-api</artifactId>
                        <version>2.2</version>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-war-plugin</artifactId>
                      <version>3.1.0</version>
                      <configuration>
                        <failOnMissingWebXml>false</failOnMissingWebXml>
                        <packagingExcludes>pom.xml, src/, target/, WebContent/</packagingExcludes>
                        <warSourceDirectory>WebContent</warSourceDirectory>
                        <webResources>
                          <resource>
                            <directory>src/main/resources</directory>
                            <targetPath>WEB-INF/classes</targetPath>
                          </resource>
                        </webResources>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycom.myapp</groupId>
                <artifactId>myapp</artifactId>
                <version>2.0.0</version>
                <packaging>war</packaging>
                <name>MyApp</name>
                <properties>
                  <maven.compiler.target>1.8</maven.compiler.target>
                  <maven.compiler.source>1.8</maven.compiler.source>
                  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  <project.version>2.0.0</project.version>
                  <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.code.gson</groupId>
                    <artifactId>gson</artifactId>
                    <version>2.10.1</version>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-war-plugin</artifactId>
                      <version>3.1.0</version>
                      <configuration>
                        <failOnMissingWebXml>false</failOnMissingWebXml>
                        <packagingExcludes>pom.xml, src/, target/, WebContent/</packagingExcludes>
                        <warSourceDirectory>WebContent</warSourceDirectory>
                        <webResources>
                          <resource>
                            <directory>src/main/resources</directory>
                            <targetPath>WEB-INF/classes</targetPath>
                          </resource>
                        </webResources>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void gradleDependencyXMLWSModuleExclusion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipeFromResource("/META-INF/rewrite/ibm-java.yml", "org.openrewrite.java.migrate.RemovedJavaXMLWSModuleProvided"),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("javax.xml.ws:jaxws-api:2.0")
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
              }
              """
          )
        );
    }

    @Test
    void removeMavenXMLJaxBModuleDependency() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycom.myapp</groupId>
                <artifactId>myapp</artifactId>
                <version>2.0.0</version>
                <packaging>war</packaging>
                <name>MyApp</name>
                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.code.gson</groupId>
                    <artifactId>gson</artifactId>
                    <version>2.10.1</version>
                  </dependency>
                  <dependency>
                    <groupId>javax.xml.bind</groupId>
                    <artifactId>jaxb-api</artifactId>
                     <version>2.3.1</version>
                  </dependency>
                  <dependency>
                    <groupId>javax.activation</groupId>
                    <artifactId>activation</artifactId>
                    <version>1.1.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycom.myapp</groupId>
                <artifactId>myapp</artifactId>
                <version>2.0.0</version>
                <packaging>war</packaging>
                <name>MyApp</name>
                <dependencies>
                  <dependency>
                    <groupId>javax</groupId>
                    <artifactId>javaee-api</artifactId>
                    <version>7.0</version>
                    <scope>provided</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.code.gson</groupId>
                    <artifactId>gson</artifactId>
                    <version>2.10.1</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void gradleDependencyXMLJaxBModuleExclusion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipeFromResource("/META-INF/rewrite/ibm-java.yml", "org.openrewrite.java.migrate.RemovedJaxBModuleProvided"),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("javax.xml.bind:jaxb-api:2.3.1")
                  implementation("javax.activation:activation:1.1.1")
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
              }
              """
          )
        );
    }
}
