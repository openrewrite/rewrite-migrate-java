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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class InvocationContextGetConstructorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InvocationContextGetConstructor()).parser(JavaParser.fromJavaVersion().dependsOn(
          "package jakarta.interceptor; public interface InvocationContext { java.lang.reflect.Method getMethod(); java.lang.reflect.Constructor<?> getConstructor(); }"
        ));
    }

    @Test
    void completeAnonymousDecorator() {
        rewriteRun(java(
          """
            import jakarta.interceptor.InvocationContext;
            import java.lang.reflect.Method;

            class A {
                InvocationContext wrap(InvocationContext delegate) {
                    return new InvocationContext() {
                        @Override
                        public Method getMethod() {
                            return delegate.getMethod();
                        }
                    };
                }
            }
            """,
          """
            import jakarta.interceptor.InvocationContext;

            import java.lang.reflect.Constructor;
            import java.lang.reflect.Method;

            class A {
                InvocationContext wrap(InvocationContext delegate) {
                    return new InvocationContext() {
                        @Override
                        public Method getMethod() {
                            return delegate.getMethod();
                        }

                        @Override
                        public Constructor<?> getConstructor() {
                            return delegate.getConstructor();
                        }
                    };
                }
            }
            """
        ));
    }
    @Test
    void preserveExistingConstructorImplementation() {
        rewriteRun(java(
          """
            import jakarta.interceptor.InvocationContext;
            import java.lang.reflect.Constructor;
            import java.lang.reflect.Method;
            class Wrapper implements InvocationContext {
                InvocationContext delegate;
                public Method getMethod() { return delegate.getMethod(); }
                public Constructor<?> getConstructor() { return null; }
            }
            """
        ));
    }

    @Test
    void doNotGuessForNonDelegatingContext() {
        rewriteRun(java(
          """
            import jakarta.interceptor.InvocationContext;
            import java.lang.reflect.Method;
            abstract class MethodContext implements InvocationContext {
                public Method getMethod() { return null; }
            }
            """
        ));
    }

    @Test
    void completeNamedDecorator() {
        rewriteRun(java(
          """
            import jakarta.interceptor.InvocationContext;
            import java.lang.reflect.Method;

            class Wrapper implements InvocationContext {
                private InvocationContext delegate;
                public Method getMethod() { return delegate.getMethod(); }
            }
            """,
          """
            import jakarta.interceptor.InvocationContext;

            import java.lang.reflect.Constructor;
            import java.lang.reflect.Method;

            class Wrapper implements InvocationContext {
                private InvocationContext delegate;
                public Method getMethod() { return delegate.getMethod(); }

                @Override
                public Constructor<?> getConstructor() {
                    return delegate.getConstructor();
                }
            }
            """
        ));
    }

}
