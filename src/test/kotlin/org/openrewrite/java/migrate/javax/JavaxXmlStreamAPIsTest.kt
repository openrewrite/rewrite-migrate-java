/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.migrate.javax

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaRecipeTest

class JavaxXmlStreamAPIsTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.javax.JavaxXmlStreamAPIs")

    @Test
    fun xmlEventFactoryNewInstance() = assertChanged(
        before = """
            import javax.xml.stream.XMLEventFactory;

            public class Test {
                public void method() {
                    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
                }
            }
        """,
        after = """
            import javax.xml.stream.XMLEventFactory;

            public class Test {
                public void method() {
                    XMLEventFactory eventFactory = XMLEventFactory.newFactory();
                }
            }
        """
    )

    @Test
    fun xmlInputFactoryNewInstance() = assertChanged(
        before = """
            import javax.xml.stream.XMLInputFactory;

            public class Test {
                public void method() {
                    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                }
            }
        """,
        after = """
            import javax.xml.stream.XMLInputFactory;

            public class Test {
                public void method() {
                    XMLInputFactory inputFactory = XMLInputFactory.newFactory();
                }
            }
        """
    )

    @Test
    fun xmlOutputFactoryNewInstance() = assertChanged(
        before = """
            import javax.xml.stream.XMLOutputFactory;

            public class Test {
                public void method() {
                    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
                }
            }
        """,
        after = """
            import javax.xml.stream.XMLOutputFactory;

            public class Test {
                public void method() {
                    XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
                }
            }
        """
    )

}
