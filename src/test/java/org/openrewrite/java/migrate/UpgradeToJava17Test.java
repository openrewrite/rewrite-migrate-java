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
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

@SuppressWarnings({"removal", "ResultOfMethodCallIgnored", "RedundantThrows", "deprecation"})
class UpgradeToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava17")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.annotation-api-1.3.2"));
    }

    @DocumentExample
    @Test
    void upgradeFromJava8ToJava17() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>1.8</java.version>
                      <maven.compiler.source>1.8</maven.compiler.source>
                      <maven.compiler.target>1.8</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <dependencies>
                      <dependency>
                        <groupId>javax.annotation</groupId>
                        <artifactId>javax.annotation-api</artifactId>
                        <version>1.3.2</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>17</java.version>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>

                    <dependencies>
                      <dependency>
                        <groupId>jakarta.annotation</groupId>
                        <artifactId>jakarta.annotation-api</artifactId>
                        <version>2.0.0</version>
                        <scope>provided</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              ),
              //language=java
              srcMainJava(
                java(
                  """
                    package com.abc;

                    import javax.annotation.PostConstruct;

                    class A {
                       public String test() {
                           return String.format("Hello %s", "world");
                       }

                       @PostConstruct
                       void init() {}
                    }
                    """,
                  """
                    package com.abc;

                    import jakarta.annotation.PostConstruct;

                    class A {
                       public String test() {
                           return "Hello %s".formatted("world");
                       }

                       @PostConstruct
                       void init() {}
                    }
                    """
                )
              )
            ),
            8)
        );
    }

    @Test
    void referenceToJavaVersionProperty() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>1.8</java.version>
                      <maven.compiler.source>${java.version}</maven.compiler.source>
                      <maven.compiler.target>${java.version}</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>17</java.version>
                      <maven.compiler.source>${java.version}</maven.compiler.source>
                      <maven.compiler.target>${java.version}</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """
              ),
              srcMainJava(
                java(
                  //language=java
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return String.format("Hello %s", "world");
                       }
                    }
                    """,
                  //language=java
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return "Hello %s".formatted("world");
                       }
                    }
                    """,
                  spec -> spec.afterRecipe(cu ->
                    assertThat(cu.getMarkers().findFirst(JavaVersion.class))
                      .map(JavaVersion::getSourceCompatibility)
                      .hasValue("17"))
                )
              )
            ),
            8)
        );
    }

    @Test
    void changeJavaxSecurityCertPackage() {
        rewriteRun(
          version(
            //language=java
            java("""
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                import java.io.InputStream;

                import javax.security.cert.*;

                class Test {
                    void foo() throws CertificateException, FileNotFoundException {
                        InputStream inStream = new FileInputStream("cert");
                        Certificate cert = X509Certificate.getInstance(inStream);
                        Certificate cert2 = X509Certificate.getInstance(inStream);
                        cert.hashCode();
                        cert2.hashCode();
                    }
                }
                """,
              """
                import java.io.FileInputStream;
                import java.io.FileNotFoundException;
                import java.io.InputStream;

                import java.security.cert.*;

                class Test {
                    void foo() throws CertificateException, FileNotFoundException {
                        InputStream inStream = new FileInputStream("cert");
                        Certificate cert = X509Certificate.getInstance(inStream);
                        Certificate cert2 = X509Certificate.getInstance(inStream);
                        cert.hashCode();
                        cert2.hashCode();
                    }
                }
                """
            ), 17)
        );
    }

    @Test
    void removedLegacySunJSSEProviderName() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import javax.net.ssl.SSLContext;

                class RemovedLegacySunJSSEProviderName {
                    String legacyProviderName = "com.sun.net.ssl.internal.ssl.Provider"; //flagged
                    String newProviderName = "SunJSSE"; //not flagged

                    void test() throws Exception {
                        SSLContext.getInstance("TLS", "com.sun.net.ssl.internal.ssl.Provider"); //flagged
                        SSLContext.getInstance("TLS", "SunJSSE"); //not flagged
                    }

                    void test2() throws Exception {
                        System.out.println("com.sun.net.ssl.internal.ssl.Provider"); //flagged
                    }
                }
                """,
              """
                import javax.net.ssl.SSLContext;

                class RemovedLegacySunJSSEProviderName {
                    String legacyProviderName = "SunJSSE"; //flagged
                    String newProviderName = "SunJSSE"; //not flagged

                    void test() throws Exception {
                        SSLContext.getInstance("TLS", "SunJSSE"); //flagged
                        SSLContext.getInstance("TLS", "SunJSSE"); //not flagged
                    }

                    void test2() throws Exception {
                        System.out.println("SunJSSE"); //flagged
                    }
                }
                """
            ), 17)
        );
    }

    @Test
    void replaceLogRecordSetThreadID() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.logging.LogRecord;

                class Foo {
                    void bar(LogRecord record) {
                        int threadID = record.getThreadID();
                        record.setThreadID(1);
                    }
                }
                """,
              """
                import java.util.logging.LogRecord;

                class Foo {
                    void bar(LogRecord record) {
                        long threadID = record.getLongThreadID();
                        record.setLongThreadID(1);
                    }
                }
                """
            ), 17)
        );
    }

    @Test
    void needToUpgradeMavenCompilerPluginToSupportReleaseTag() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <version>3.3.0</version>
                          <configuration>
                            <source>1.8</source>
                            <target>1.8</target>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                after -> after.after(pomXml -> """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <version>%s</version>
                          <configuration>
                            <release>17</release>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """.formatted(Pattern.compile("<version>(3\\.\\d\\d.*)</version>").matcher(pomXml)
                  .results().findFirst().orElseThrow().group(1)))
              )
            ),
            8)
        );
    }

    @Test
    void agentMainPreMainPublicApp() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                package com.test;

                import java.lang.instrument.Instrumentation;

                public class AgentMainPreMainPublicApp {

                	private static void premain(String agentArgs) {
                		//This should flag
                	}

                	public static void premain(String agentArgs, Instrumentation inst) {
                		//This shouldn't flag
                	}

                	public static void premain(String agentArgs, Instrumentation inst, String foo) {
                		//This shouldn't flag
                	}

                	private static void premain1(String agentArgs) {
                		//This shouldn't flag
                	}

                	protected void agentmain(String agentArgs) {
                		//This should flag
                	}

                    static void agentmain(String agentArgs, Instrumentation inst) {
                		//This should flag
                	}

                	private static void agentmain(String agentArgs, Instrumentation inst, String foo) {
                		//This shouldn't flag
                	}

                    private static void agentmain(String agentArgs, String inst) {
                		//This shouldn't flag
                	}
                }
                """,
              """
                package com.test;

                import java.lang.instrument.Instrumentation;

                public class AgentMainPreMainPublicApp {

                	public static void premain(String agentArgs) {
                		//This should flag
                	}

                	public static void premain(String agentArgs, Instrumentation inst) {
                		//This shouldn't flag
                	}

                	public static void premain(String agentArgs, Instrumentation inst, String foo) {
                		//This shouldn't flag
                	}

                	private static void premain1(String agentArgs) {
                		//This shouldn't flag
                	}

                	public void agentmain(String agentArgs) {
                		//This should flag
                	}

                    public static void agentmain(String agentArgs, Instrumentation inst) {
                		//This should flag
                	}

                	private static void agentmain(String agentArgs, Instrumentation inst, String foo) {
                		//This shouldn't flag
                	}

                    private static void agentmain(String agentArgs, String inst) {
                		//This shouldn't flag
                	}
                }
                """
            ), 17)
        );
    }

    @Test
    void lombokBumpedGoingTo17() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.16.22</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <maven.compiler.release>17</maven.compiler.release>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>%s</version>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(Pattern.compile("<version>(1\\.18.*)</version>")
                .matcher(actual).results().findFirst().orElseThrow().group(1))
            )
          )
        );
    }

    @Test
    void removedSSLSessionGetPeerCertificateChainMethodImplTest() {
        rewriteRun(
          //language=java
          java(
            """
              import java.security.cert.Certificate;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.SSLEngine;
              import javax.net.ssl.SSLSession;
              class RemovedSSLSessionGetPeerCertificateChainMethodImplApp {
                      void test() throws Exception {
                           SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine();
                           SSLSession session = sslEngine.getHandshakeSession();
                           session.getPeerCertificateChain(); //This should trigger
                           Certificate[] certs = session.getPeerCertificates(); //This should not trigger
                      }
              }
              """,
            """
              import java.security.cert.Certificate;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.SSLEngine;
              import javax.net.ssl.SSLSession;
              class RemovedSSLSessionGetPeerCertificateChainMethodImplApp {
                      void test() throws Exception {
                           SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine();
                           SSLSession session = sslEngine.getHandshakeSession();
                           session.getPeerCertificates(); //This should trigger
                           Certificate[] certs = session.getPeerCertificates(); //This should not trigger
                      }
              }
              """
          )
        );
    }

}
