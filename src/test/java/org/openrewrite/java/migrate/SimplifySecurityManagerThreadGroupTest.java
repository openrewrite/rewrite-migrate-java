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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class SimplifySecurityManagerThreadGroupTest implements RewriteTest {
    @Test
    void removeNullLocalAndDeadBranch() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.SystemGetSecurityManagerToNull")
            .allSources(source -> version(source, 25)),
          java(
            """
              class Test {
                  ThreadGroup group;
                  Test() {
                      SecurityManager s = System.getSecurityManager();
                      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
                  }
              }
              """,
            """
              class Test {
                  ThreadGroup group;
                  Test() {
                      group = Thread.currentThread().getThreadGroup();
                  }
              }
              """
          )
        );
    }
    @Test
    void retainLocalWithAnotherUse() {
        rewriteRun(
          spec -> spec.recipe(new SimplifySecurityManagerThreadGroup())
            .allSources(source -> version(source, 25)),
          java(
            """
              class Test {
                  ThreadGroup group;
                  Test() {
                      SecurityManager s = null;
                      group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
                      System.out.println(s);
                  }
              }
              """
          )
        );
    }
    @Test
    void retainNestedComments() {
        rewriteRun(
          spec -> spec.recipe(new SimplifySecurityManagerThreadGroup())
            .allSources(source -> version(source, 25)),
          java(
            """
              class InitializerComment {
                  ThreadGroup group;
                  InitializerComment() {
                      SecurityManager s = /* keep rationale */ null;
                      group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
                  }
              }
              """
          ),
          java(
            """
              class BranchComment {
                  ThreadGroup group;
                  BranchComment() {
                      SecurityManager s = null;
                      group = s != null ? /* keep rationale */ s.getThreadGroup() : Thread.currentThread().getThreadGroup();
                  }
              }
              """
          )
        );
    }
}
