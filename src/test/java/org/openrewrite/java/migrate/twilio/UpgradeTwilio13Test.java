/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.twilio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeTwilio13Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.twilio.UpgradeTwilio_13")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "twilio", "httpclient"));
    }

    @DocumentExample
    @Test
    void raisesThePropertyTheVersionComesFrom() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>notifier</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <twilio.version>7.38.0</twilio.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>com.twilio.sdk</groupId>
                          <artifactId>twilio</artifactId>
                          <version>${twilio.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                assertThat(pom)
                  .containsPattern("<twilio.version>13\\.\\d+\\.\\d+</twilio.version>")
                  .contains("<version>${twilio.version}</version>");
                return pom;
            })
          )
        );
    }

    @Test
    void raisesAVersionManagedInTheParent() {
        rewriteRun(
          mavenProject("parent",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>sms</module>
                    </modules>
                    <properties>
                        <twilio.version>7.38.1</twilio.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.twilio.sdk</groupId>
                                <artifactId>twilio</artifactId>
                                <version>${twilio.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).containsPattern("<twilio.version>13\\.\\d+\\.\\d+</twilio.version>");
                  return pom;
              })
            ),
            mavenProject("sms",
              //language=xml
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>sms</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.twilio.sdk</groupId>
                              <artifactId>twilio</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void raisesAGradleDependency() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.twilio.sdk:twilio:7.38.0'
              }
              """,
            spec -> spec.after(gradle -> {
                assertThat(gradle).containsPattern("com\\.twilio\\.sdk:twilio:13\\.\\d+\\.\\d+");
                return gradle;
            })
          )
        );
    }

    @Test
    void leavesTheJavaVersionAlone() {
        rewriteRun(
          mavenProject("notifier",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>notifier</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <java.version>1.8</java.version>
                        <maven.compiler.source>1.8</maven.compiler.source>
                        <maven.compiler.target>1.8</maven.compiler.target>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.twilio.sdk</groupId>
                            <artifactId>twilio</artifactId>
                            <version>7.38.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<java.version>1.8</java.version>")
                    .contains("<maven.compiler.source>1.8</maven.compiler.source>")
                    .contains("<maven.compiler.target>1.8</maven.compiler.target>")
                    .containsPattern("<version>13\\.\\d+\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void leavesARepositoryWithoutTwilioAlone() {
        rewriteRun(
          mavenProject("unrelated",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>unrelated</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.0.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void addsTheHttpClientTwilioNoLongerBringsAlong() {
        rewriteRun(
          mavenProject("voice",
            srcMainJava(
              //language=java
              java(
                """
                  import org.apache.http.client.utils.URIBuilder;

                  class Params {
                      String host(URIBuilder uri) {
                          return uri.getHost();
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>voice</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.twilio.sdk</groupId>
                            <artifactId>twilio</artifactId>
                            <version>8.7.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .containsPattern("<version>13\\.\\d+\\.\\d+</version>")
                    .contains("<artifactId>httpclient</artifactId>")
                    .containsPattern("<version>4\\.5\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void addsTheGuavaTwilioNoLongerBringsAlong() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          mavenProject("sms",
            srcMainJava(
              //language=java
              java(
                """
                  import com.google.common.base.Strings;

                  class Numbers {
                      boolean blank(String number) {
                          return Strings.isNullOrEmpty(number);
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>sms</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.twilio.sdk</groupId>
                            <artifactId>twilio</artifactId>
                            <version>7.38.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<artifactId>guava</artifactId>")
                    .containsPattern("<version>\\d+\\.\\d+(\\.\\d+)?-jre</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void addsTheGuavaTwilioNoLongerBringsAlongToAGradleProject() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          mavenProject("sms",
            srcMainJava(
              //language=java
              java(
                """
                  import com.google.common.base.Strings;

                  class Numbers {
                      boolean blank(String number) {
                          return Strings.isNullOrEmpty(number);
                      }
                  }
                  """
              )
            ),
            //language=groovy
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'com.twilio.sdk:twilio:7.38.0'
                }
                """,
              spec -> spec.after(gradle -> {
                  assertThat(gradle)
                    .containsPattern("com\\.twilio\\.sdk:twilio:13\\.\\d+\\.\\d+")
                    .containsPattern("com\\.google\\.guava:guava:\\d+\\.\\d+(\\.\\d+)?-jre");
                  return gradle;
              })
            )
          )
        );
    }

    @Test
    void keepsAVersionTheProjectDeclaresItself() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          mavenProject("sms",
            srcMainJava(
              //language=java
              java(
                """
                  import com.google.common.base.Strings;

                  class Numbers {
                      boolean blank(String number) {
                          return Strings.isNullOrEmpty(number);
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>sms</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <guava.version>27.0-jre</guava.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.twilio.sdk</groupId>
                            <artifactId>twilio</artifactId>
                            <version>7.38.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<guava.version>27.0-jre</guava.version>")
                    .containsPattern("<version>13\\.\\d+\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void keepsAVersionTheGradleProjectDeclaresItself() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          mavenProject("sms",
            srcMainJava(
              //language=java
              java(
                """
                  import com.google.common.base.Strings;

                  class Numbers {
                      boolean blank(String number) {
                          return Strings.isNullOrEmpty(number);
                      }
                  }
                  """
              )
            ),
            //language=groovy
            buildGradle(
              """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'com.google.guava:guava:30.1.1-jre'
                    implementation 'com.twilio.sdk:twilio:10.2.0'
                }
                """,
              spec -> spec.after(gradle -> {
                  assertThat(gradle)
                    .contains("com.google.guava:guava:30.1.1-jre")
                    .containsPattern("com\\.twilio\\.sdk:twilio:13\\.\\d+\\.\\d+");
                  return gradle;
              })
            )
          )
        );
    }

    @Test
    void addsNothingWhereTheTransitiveDependencyWasNotUsed() {
        rewriteRun(
          mavenProject("sms",
            srcMainJava(
              //language=java
              java(
                """
                  import com.twilio.Twilio;

                  class Init {
                      void init() {
                          Twilio.init("AC", "token");
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>sms</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.twilio.sdk</groupId>
                            <artifactId>twilio</artifactId>
                            <version>7.38.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .doesNotContain("<artifactId>httpclient</artifactId>")
                    .doesNotContain("<artifactId>guava</artifactId>")
                    .doesNotContain("<artifactId>joda-time</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void addsNothingToARepositoryWithoutTwilio() {
        rewriteRun(
          mavenProject("unrelated",
            srcMainJava(
              //language=java
              java(
                """
                  import org.apache.http.client.utils.URIBuilder;

                  class Params {
                      String host(URIBuilder uri) {
                          return uri.getHost();
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>unrelated</artifactId>
                    <version>1.0.0</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void removesAnUnusedImportOfARemovedProduct() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.rest.autopilot.v1.assistant.Task;
              import org.apache.http.client.utils.URIBuilder;

              class Exemption {
                  String host(URIBuilder uri) {
                      return uri.getHost();
                  }
              }
              """,
            """
              import org.apache.http.client.utils.URIBuilder;

              class Exemption {
                  String host(URIBuilder uri) {
                      return uri.getHost();
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.twilio.rest.authy.v1.Service", "com.twilio.rest.fax.v1.Fax"})
    void removesUnusedImportsOfTheOtherRemovedProducts(String removed) {
        rewriteRun(
          //language=java
          java(
            """
              import %s;

              class Exemption {
              }
              """.formatted(removed),
            """
              class Exemption {
              }
              """
          )
        );
    }

    @Test
    void leavesAnImportOfARemovedProductThatIsUsedForTheCompiler() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.rest.autopilot.v1.assistant.Task;

              class Exemption {
                  Task task;
              }
              """
          )
        );
    }

    @Test
    void leavesOtherUnusedImportsAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.Twilio;
              import java.util.List;

              class Init {
                  void init() {
                      Twilio.init("AC", "token");
                  }
              }
              """
          )
        );
    }

    @Test
    void callsWithoutABreakingChangeStayAsTheyAre() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.http.TwilioRestClient;
              import com.twilio.rest.api.v2010.account.Message;
              import com.twilio.type.PhoneNumber;

              class Communicator {
                  String send(TwilioRestClient client, String to, String from, String body) {
                      Message message = Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create(client);
                      return message.getSid() + message.getTo() + message.getBody();
                  }

                  TwilioRestClient client(String accountSid, String authToken) {
                      return new TwilioRestClient.Builder(accountSid, authToken).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void renamesNestedTwimlMethods() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.twiml.voice.*;

              class Twiml {
                  Say say(SsmlBreak pause, SsmlEmphasis emphasis, SsmlLang lang, SsmlP p, SsmlPhoneme phoneme,
                          SsmlProsody prosody, SsmlS s, SsmlSayAs sayAs, SsmlSub sub, SsmlW w) {
                      return new Say.Builder("hello")
                              .ssmlBreak(pause)
                              .ssmlEmphasis(emphasis)
                              .ssmlLang(lang)
                              .ssmlP(p)
                              .ssmlPhoneme(phoneme)
                              .ssmlProsody(prosody)
                              .ssmlS(s)
                              .ssmlSayAs(sayAs)
                              .ssmlSub(sub)
                              .ssmlW(w)
                              .build();
                  }

                  Refer refer(ReferSip sip) {
                      return new Refer.Builder().referSip(sip).build();
                  }
              }
              """,
            """
              import com.twilio.twiml.voice.*;

              class Twiml {
                  Say say(SsmlBreak pause, SsmlEmphasis emphasis, SsmlLang lang, SsmlP p, SsmlPhoneme phoneme,
                          SsmlProsody prosody, SsmlS s, SsmlSayAs sayAs, SsmlSub sub, SsmlW w) {
                      return new Say.Builder("hello")
                              .break_(pause)
                              .emphasis(emphasis)
                              .lang(lang)
                              .p(p)
                              .phoneme(phoneme)
                              .prosody(prosody)
                              .s(s)
                              .sayAs(sayAs)
                              .sub(sub)
                              .w(w)
                              .build();
                  }

                  Refer refer(ReferSip sip) {
                      return new Refer.Builder().sip(sip).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void movesDeprecatedGrantsToVideoGrant() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.ConversationsGrant;
              import com.twilio.jwt.accesstoken.VideoGrant;

              class Grants {
                  String conversations(String profile) {
                      ConversationsGrant grant = new ConversationsGrant();
                      grant.setConfigurationProfileSid(profile);
                      return grant.getConfigurationProfileSid();
                  }

                  String video(String profile) {
                      VideoGrant grant = new VideoGrant();
                      grant.setConfigurationProfileSid(profile);
                      return grant.getConfigurationProfileSid();
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.VideoGrant;

              class Grants {
                  String conversations(String profile) {
                      VideoGrant grant = new VideoGrant();
                      grant.setRoom(profile);
                      return grant.getRoom();
                  }

                  String video(String profile) {
                      VideoGrant grant = new VideoGrant();
                      grant.setRoom(profile);
                      return grant.getRoom();
                  }
              }
              """
          )
        );
    }

    @Test
    void changesTheBuilderPassedToNetworkHttpClient() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.http.NetworkHttpClient;
              import org.apache.http.impl.client.HttpClientBuilder;

              class Client {
                  NetworkHttpClient client() {
                      return new NetworkHttpClient(HttpClientBuilder.create().disableRedirectHandling().useSystemProperties());
                  }
              }
              """,
            """
              import com.twilio.http.NetworkHttpClient;
              import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

              class Client {
                  NetworkHttpClient client() {
                      return new NetworkHttpClient(HttpClientBuilder.create().disableRedirectHandling().useSystemProperties());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesHttpClient4AloneElsewhere() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.http.impl.client.CloseableHttpClient;
              import org.apache.http.impl.client.HttpClientBuilder;

              class Unrelated {
                  CloseableHttpClient client() {
                      return HttpClientBuilder.create().build();
                  }
              }
              """
          )
        );
    }
}
