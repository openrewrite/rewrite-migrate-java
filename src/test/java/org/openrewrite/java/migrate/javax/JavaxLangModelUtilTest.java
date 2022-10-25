/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaxLangModelUtilTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.javax.JavaxLangModelUtil"));
    }

    @Test
    void abstractAnnotationValueVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.AbstractAnnotationValueVisitor6;

                abstract class Test extends AbstractAnnotationValueVisitor6 {}
            """,
          """
                import javax.lang.model.util.AbstractAnnotationValueVisitor9;

                abstract class Test extends AbstractAnnotationValueVisitor9 {}
            """
        ));
    }

    @Test
    void abstractElementVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.AbstractElementVisitor6;

                abstract class Test extends AbstractElementVisitor6 {}
            """,
          """
                import javax.lang.model.util.AbstractElementVisitor9;

                abstract class Test extends AbstractElementVisitor9 {}
            """
        ));
    }

    @Test
    void abstractTypeVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.AbstractTypeVisitor6;

                abstract class Test extends AbstractTypeVisitor6 {}
            """,
          """
                import javax.lang.model.util.AbstractTypeVisitor9;

                abstract class Test extends AbstractTypeVisitor9 {}
            """
        ));
    }

    @Test
    void elementKindVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.ElementKindVisitor6;

                public class Test extends ElementKindVisitor6 {}
            """,
          """
                import javax.lang.model.util.ElementKindVisitor9;

                public class Test extends ElementKindVisitor9 {}
            """
        ));
    }

    @Test
    void elementScanner6() {
        rewriteRun(java("""
                import javax.lang.model.util.ElementScanner6;

                public class Test extends ElementScanner6 {}
            """,
          """
                import javax.lang.model.util.ElementScanner9;

                public class Test extends ElementScanner9 {}
            """
        ));
    }

    @Test
    void simpleAnnotationValueVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.SimpleAnnotationValueVisitor6;

                public class Test extends SimpleAnnotationValueVisitor6 {}
            """,
          """
                import javax.lang.model.util.SimpleAnnotationValueVisitor9;

                public class Test extends SimpleAnnotationValueVisitor9 {}
            """
        ));
    }

    @Test
    void simpleElementVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.SimpleElementVisitor6;

                public class Test extends SimpleElementVisitor6 {}
            """,
          """
                import javax.lang.model.util.SimpleElementVisitor9;

                public class Test extends SimpleElementVisitor9 {}
            """
        ));
    }

    @Test
    void simpleTypeVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.SimpleTypeVisitor6;

                public class Test extends SimpleTypeVisitor6 {}
            """,
          """
                import javax.lang.model.util.SimpleTypeVisitor9;

                public class Test extends SimpleTypeVisitor9 {}
            """
        ));
    }

    @Test
    void typeKindVisitor6() {
        rewriteRun(java("""
                import javax.lang.model.util.TypeKindVisitor6;

                public class Test extends TypeKindVisitor6 {}
            """,
          """
                import javax.lang.model.util.TypeKindVisitor9;

                public class Test extends TypeKindVisitor9 {}
            """
        ));
    }

}
