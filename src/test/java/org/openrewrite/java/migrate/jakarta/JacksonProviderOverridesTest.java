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

class JacksonProviderOverridesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JacksonProviderOverrides()).parser(JavaParser.fromJavaVersion().dependsOn(
          "package com.fasterxml.jackson.databind; public class ObjectMapper { public void configure() {} }",
          "package com.fasterxml.jackson.jaxrs.base; public class ProviderBase { protected Object _configForWriting(com.fasterxml.jackson.databind.ObjectMapper m, java.lang.annotation.Annotation[] a) { return null; } }"
        ));
    }

    @Test
    void forwardDefaultViewInOverride() {
        rewriteRun(java(
          """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import com.fasterxml.jackson.jaxrs.base.ProviderBase;
            import java.lang.annotation.Annotation;

            class CustomProvider extends ProviderBase {
                @Override
                protected Object _configForWriting(ObjectMapper mapper, Annotation[] annotations) {
                    mapper.configure();
                    return super._configForWriting(mapper, annotations);
                }
            }
            """,
          """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import com.fasterxml.jackson.jaxrs.base.ProviderBase;
            import java.lang.annotation.Annotation;

            class CustomProvider extends ProviderBase {
                @Override
                protected Object _configForWriting(ObjectMapper mapper, Annotation[] annotations, Class<?> defaultView) {
                    mapper.configure();
                    return super._configForWriting(mapper, annotations, defaultView);
                }
            }
            """
        ));
    }
    @Test
    void leaveUnrelatedMethodUnchanged() {
        rewriteRun(java(
          """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import java.lang.annotation.Annotation;
            class Unrelated {
                Object _configForWriting(ObjectMapper mapper, Annotation[] annotations) { return null; }
            }
            """
        ));
    }

    @Test
    void avoidLocalNameCollision() {
        rewriteRun(java(
          """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import com.fasterxml.jackson.jaxrs.base.ProviderBase;
            import java.lang.annotation.Annotation;
            class CustomProvider extends ProviderBase {
                protected Object _configForWriting(ObjectMapper mapper, Annotation[] annotations) {
                    Object defaultView = null;
                    return super._configForWriting(mapper, annotations);
                }
            }
            """,
          """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import com.fasterxml.jackson.jaxrs.base.ProviderBase;
            import java.lang.annotation.Annotation;
            class CustomProvider extends ProviderBase {
                protected Object _configForWriting(ObjectMapper mapper, Annotation[] annotations, Class<?> defaultView1) {
                    Object defaultView = null;
                    return super._configForWriting(mapper, annotations, defaultView1);
                }
            }
            """
        ));
    }

}
