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

import static org.openrewrite.java.Assertions.java;

class UseScalarNodeScalarStyleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseScalarNodeScalarStyle())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.33"));
    }

    @DocumentExample
    @Test
    void styleOfCopiedNode() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class KeyScalarNode extends ScalarNode {
                  KeyScalarNode(ScalarNode node) {
                      super(node.getTag(), true, node.getValue(), node.getStartMark(), node.getEndMark(), node.getStyle());
                  }

                  static ScalarNode copy(ScalarNode node, String value) {
                      return new ScalarNode(node.getTag(), value, node.getStartMark(), node.getEndMark(), node.getStyle());
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class KeyScalarNode extends ScalarNode {
                  KeyScalarNode(ScalarNode node) {
                      super(node.getTag(), true, node.getValue(), node.getStartMark(), node.getEndMark(), node.getScalarStyle());
                  }

                  static ScalarNode copy(ScalarNode node, String value) {
                      return new ScalarNode(node.getTag(), value, node.getStartMark(), node.getEndMark(), node.getScalarStyle());
                  }
              }
              """
          )
        );
    }

    @Test
    void styleAsCharacter() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class A {
                  boolean quoted(ScalarNode node) {
                      Character style = node.getStyle();
                      return style != null && style == '"';
                  }
              }
              """,
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class A {
                  boolean quoted(ScalarNode node) {
                      Character style = node.getScalarStyle().getChar();
                      return style != null && style == '"';
                  }
              }
              """
          )
        );
    }

    @Test
    void scalarStyleIsLeftAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class A {
                  ScalarNode copy(ScalarNode node) {
                      return new ScalarNode(node.getTag(), node.getValue(), node.getStartMark(), node.getEndMark(), node.getScalarStyle());
                  }
              }
              """
          )
        );
    }

    @Test
    void olderSnakeYamlWithoutGetScalarStyle() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20")),
          //language=java
          java(
            """
              import org.yaml.snakeyaml.nodes.ScalarNode;

              class A {
                  Object style(ScalarNode node) {
                      return node.getStyle();
                  }
              }
              """
          )
        );
    }
}
