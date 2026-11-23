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
package org.openrewrite.java.migrate.snakeyaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class AddSnakeYamlOptionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddSnakeYamlOptions())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.33"));
    }

    @DocumentExample
    @Test
    void constructorOfRootType() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;

              class Config {
                  String name;

                  static Config load(String yaml) {
                      return new Yaml(new Constructor(Config.class)).load(yaml);
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;

              class Config {
                  String name;

                  static Config load(String yaml) {
                      return new Yaml(new Constructor(Config.class, new LoaderOptions())).load(yaml);
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorsWithoutLoaderOptions() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.TypeDescription;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class A {
                  void yamls(TypeDescription description) throws ClassNotFoundException {
                      new Yaml(new SafeConstructor());
                      new Yaml(new Constructor());
                      new Yaml(new Constructor(description));
                      new Yaml(new Constructor("java.util.ArrayList"));
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.TypeDescription;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class A {
                  void yamls(TypeDescription description) throws ClassNotFoundException {
                      new Yaml(new SafeConstructor(new LoaderOptions()));
                      new Yaml(new Constructor(new LoaderOptions()));
                      new Yaml(new Constructor(description, new LoaderOptions()));
                      new Yaml(new Constructor("java.util.ArrayList", new LoaderOptions()));
                  }
              }
              """
          )
        );
    }

    @Test
    void customClassLoaderConstructorWhereTheProjectDeclaresSnakeYaml() {
        rewriteRun(
          mavenProject("app",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.yaml</groupId>
                            <artifactId>snakeyaml</artifactId>
                            <version>1.33</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            srcMainJava(
              //language=java
              java(
                """
                  import org.yaml.snakeyaml.Yaml;
                  import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                  class A {
                      void yamls(ClassLoader loader) {
                          new Yaml(new CustomClassLoaderConstructor(loader));
                          new Yaml(new CustomClassLoaderConstructor(A.class, loader));
                      }
                  }
                  """,
                """
                  import org.yaml.snakeyaml.LoaderOptions;
                  import org.yaml.snakeyaml.Yaml;
                  import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                  class A {
                      void yamls(ClassLoader loader) {
                          new Yaml(new CustomClassLoaderConstructor(loader, new LoaderOptions()));
                          new Yaml(new CustomClassLoaderConstructor(A.class, loader, new LoaderOptions()));
                      }
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void customClassLoaderConstructorWhereSnakeYamlStaysOn1() {
        // SnakeYAML 1.33 has no CustomClassLoaderConstructor that takes LoaderOptions, and nothing raises it here
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

              class A {
                  void yamls(ClassLoader loader) {
                      new Yaml(new CustomClassLoaderConstructor(loader));
                  }
              }
              """
          )
        );
    }

    @Test
    void oldSnakeYamlWhereTheProjectDeclaresIt() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20")),
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
                        <module>app</module>
                    </modules>
                    <properties>
                        <snakeyaml.version>1.20</snakeyaml.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.yaml</groupId>
                                <artifactId>snakeyaml</artifactId>
                                <version>${snakeyaml.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """
            ),
            mavenProject("app",
              //language=xml
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>app</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.yaml</groupId>
                              <artifactId>snakeyaml</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              ),
              srcMainJava(
                //language=java
                java(
                  """
                    import org.yaml.snakeyaml.Yaml;
                    import org.yaml.snakeyaml.constructor.Constructor;

                    class A {
                        Yaml yaml() {
                            return new Yaml(new Constructor(A.class));
                        }
                    }
                    """,
                  """
                    import org.yaml.snakeyaml.LoaderOptions;
                    import org.yaml.snakeyaml.Yaml;
                    import org.yaml.snakeyaml.constructor.Constructor;

                    class A {
                        Yaml yaml() {
                            return new Yaml(new Constructor(A.class, new LoaderOptions()));
                        }
                    }
                    """
                )
              )
            )
          )
        );
    }

    @Test
    void oldSnakeYamlInAGradleBuildThatIsNotRaised() {
        // The Gradle project declares SnakeYAML 1.20, which has no Representer that takes DumperOptions, and nothing
        // raises it here
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20")),
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
                  implementation 'org.yaml:snakeyaml:1.20'
              }
              """
          ),
          srcMainJava(
            //language=java
            java(
              """
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.representer.Representer;

                class A {
                    Yaml yaml() {
                        return new Yaml(new Representer());
                    }
                }
                """
            )
          )
        );
    }

    @Test
    void oldSnakeYamlThatNothingRaises() {
        // SnakeYAML 1.20 has no Constructor that takes LoaderOptions, so passing one wouldn't compile
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20")),
          //language=java
          java(
            """
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  Yaml yaml() {
                      return new Yaml(new Constructor(A.class), new Representer());
                  }
              }
              """
          )
        );
    }

    @Test
    void representer() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  Yaml yaml() {
                      DumperOptions options = new DumperOptions();
                      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                      Representer representer = new Representer();
                      representer.getPropertyUtils().setSkipMissingProperties(true);
                      return new Yaml(new Constructor(A.class), representer, options);
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  Yaml yaml() {
                      DumperOptions options = new DumperOptions();
                      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                      Representer representer = new Representer(new DumperOptions());
                      representer.getPropertyUtils().setSkipMissingProperties(true);
                      return new Yaml(new Constructor(A.class, new LoaderOptions()), representer, options);
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousSubclass() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  Yaml yaml() {
                      return new Yaml(new Representer() {
                          {
                              getPropertyUtils().setSkipMissingProperties(true);
                          }
                      });
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  Yaml yaml() {
                      return new Yaml(new Representer(new DumperOptions()) {
                          {
                              getPropertyUtils().setSkipMissingProperties(true);
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void explicitSuperCalls() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.representer.Representer;

              class MyConstructor extends Constructor {
                  MyConstructor() {
                      super();
                  }

                  MyConstructor(Class<?> root) {
                      super(root);
                  }
              }

              class MyRepresenter extends Representer {
                  MyRepresenter() {
                      super();
                      addClassTag(MyRepresenter.class, null);
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.representer.Representer;

              class MyConstructor extends Constructor {
                  MyConstructor() {
                      super(new LoaderOptions());
                  }

                  MyConstructor(Class<?> root) {
                      super(root, new LoaderOptions());
                  }
              }

              class MyRepresenter extends Representer {
                  MyRepresenter() {
                      super(new DumperOptions());
                      addClassTag(MyRepresenter.class, null);
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitSuperCallInConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class MyConstructor extends SafeConstructor {
                  private final String name;

                  MyConstructor(String name) {
                      // remember the name
                      this.name = name;
                  }

                  MyConstructor() {
                      this("default");
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class MyConstructor extends SafeConstructor {
                  private final String name;

                  MyConstructor(String name) {
                      super(new LoaderOptions());
                      // remember the name
                      this.name = name;
                  }

                  MyConstructor() {
                      this("default");
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitSuperCallInEmptyConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.representer.Representer;

              public class MyRepresenter extends Representer {
                  public MyRepresenter() {}
              }
              """,
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.representer.Representer;

              public class MyRepresenter extends Representer {
                  public MyRepresenter() {
                      super(new DumperOptions());
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitDefaultConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.introspector.Property;
              import org.yaml.snakeyaml.nodes.NodeTuple;
              import org.yaml.snakeyaml.representer.Representer;

              public class SkipNullRepresenter extends Representer {
                  private static final String UNUSED = "";

                  @Override
                  protected NodeTuple representJavaBeanProperty(Object bean, Property property, Object value, org.yaml.snakeyaml.nodes.Tag tag) {
                      return value == null ? null : super.representJavaBeanProperty(bean, property, value, tag);
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.introspector.Property;
              import org.yaml.snakeyaml.nodes.NodeTuple;
              import org.yaml.snakeyaml.representer.Representer;

              public class SkipNullRepresenter extends Representer {
                  private static final String UNUSED = "";

                  public SkipNullRepresenter() {
                      super(new DumperOptions());
                  }

                  @Override
                  protected NodeTuple representJavaBeanProperty(Object bean, Property property, Object value, org.yaml.snakeyaml.nodes.Tag tag) {
                      return value == null ? null : super.representJavaBeanProperty(bean, property, value, tag);
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitDefaultConstructorOfNestedClass() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class A {
                  private static class Strict extends SafeConstructor {
                      @Override
                      protected void flattenMapping(org.yaml.snakeyaml.nodes.MappingNode node) {
                      }
                  }

                  Yaml yaml() {
                      return new Yaml(new Strict());
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class A {
                  private static class Strict extends SafeConstructor {
                      private Strict() {
                          super(new LoaderOptions());
                      }

                      @Override
                      protected void flattenMapping(org.yaml.snakeyaml.nodes.MappingNode node) {
                      }
                  }

                  Yaml yaml() {
                      return new Yaml(new Strict());
                  }
              }
              """
          )
        );
    }

    @Test
    void blankLineWithTrailingSpacesBeforeTheFirstMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.constructor.Constructor;

              class FilterConstructor extends Constructor {
                  \s
                  @Override
                  protected Class<?> getClassForName(String name) throws ClassNotFoundException {
                      return super.getClassForName(name);
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.constructor.Constructor;

              class FilterConstructor extends Constructor {
                  FilterConstructor() {
                      super(new LoaderOptions());
                  }
                  \s
                  @Override
                  protected Class<?> getClassForName(String name) throws ClassNotFoundException {
                      return super.getClassForName(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void argumentsOnTheirOwnLines() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.TypeDescription;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;

              class A {
                  Yaml yaml() {
                      return new Yaml(new Constructor(
                          new TypeDescription(A.class)));
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.TypeDescription;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;

              class A {
                  Yaml yaml() {
                      return new Yaml(new Constructor(
                          new TypeDescription(A.class),
                          new LoaderOptions()));
                  }
              }
              """
          )
        );
    }

    @Test
    void optionsAlreadyPassed() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.DumperOptions;
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.Yaml;
              import org.yaml.snakeyaml.constructor.Constructor;
              import org.yaml.snakeyaml.constructor.SafeConstructor;
              import org.yaml.snakeyaml.representer.Representer;

              class A {
                  void yamls(LoaderOptions loaderOptions, DumperOptions dumperOptions) {
                      new Yaml(new SafeConstructor(loaderOptions));
                      new Yaml(new Constructor(A.class, loaderOptions), new Representer(dumperOptions));
                  }
              }

              class MyConstructor extends Constructor {
                  MyConstructor(LoaderOptions options) {
                      super(A.class, options);
                  }
              }
              """
          )
        );
    }

    @Test
    void subclassOfASubclassIsLeftAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.LoaderOptions;
              import org.yaml.snakeyaml.constructor.SafeConstructor;

              class Base extends SafeConstructor {
                  Base() {
                      super(new LoaderOptions());
                  }
              }

              class Derived extends Base {
              }

              class A {
                  Object o = new Derived();
              }
              """
          )
        );
    }

    @Test
    void lombokConstructorsAreLeftAlone() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.33", "lombok")),
          //language=java
          java(
            """
              import lombok.RequiredArgsConstructor;
              import org.yaml.snakeyaml.representer.Representer;

              @RequiredArgsConstructor
              class MyRepresenter extends Representer {
                  private final String name;
              }
              """
          )
        );
    }
}
