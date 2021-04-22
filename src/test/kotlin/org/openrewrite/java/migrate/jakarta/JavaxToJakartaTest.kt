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
package org.openrewrite.java.migrate.jakarta

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.ChangeType
import org.openrewrite.java.JavaRecipeTest

class JavaxToJakartaTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")

    @Test
    fun dontAddImportWhenNoChangesWereMade() = assertUnchanged(
        before = "public class B {}"
    )

    private val source: String = "javax.xml.bind."
    private val target: String = "jakarta.xml.bind."

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun changeImport(pkg: String, className: String) {
        assertChanged(
            before = """
                package org.A;

                import $source$pkg$className;

                public class B {
                }
            """,
            after = """
                package org.A;

                import $target$pkg$className;

                public class B {
                }
            """
        )
    }

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun changeQualifiedFieldAccess(pkg: String, className: String) {
        assertChanged(
            before = """
                package org.A;

                public class B {
                    $source$pkg$className name =  new $source$pkg$className();
                }
            """,
            after = """
                package org.A;

                public class B {
                    $target$pkg$className name =  new $target$pkg$className();
                }
            """
        )
    }

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun fullyQualifiedName(pkg: String, className: String) = assertChanged(
        before = "public class B extends $source$pkg$className {}",
        after = "public class B extends $target$pkg$className {}"
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun annotation(pkg: String, className: String) = assertChanged(
        before = "@$source$pkg$className public class B {}",
        after = "@$target$pkg$className public class B {}"
    )

    // array types and new arrays
    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun array(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className[] a = new $className[0];
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className[] a = new $className[0];
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun classDecl(pkg: String, className: String) = assertChanged(
        dependsOn = arrayOf(
            "public interface I1 {}",
            "public interface I2 {}"
        ),
        recipe = recipe.doNext(
            ChangeType("I1", "I2")
        ),
        before = """
            import $source$pkg$className;
            public class B extends $className implements I1 {}
        """,
        after = """
            import $target$pkg$className;
            public class B extends $className implements I2 {}
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun method(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public $className foo() throws $className { return null; }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public $className foo() throws $className { return null; }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun methodInvocationTypeParametersAndWildcard(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public <T extends $className> T generic(T n, List<? super $className> in);
               public void test() {
                   $className.stat();
                   this.<$className>generic(null, null);
               }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public <T extends $className> T generic(T n, List<? super $className> in);
               public void test() {
                   $className.stat();
                   this.<$className>generic(null, null);
               }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun multiCatch(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public void test() {
                   try {}
                   catch($className | RuntimeException e) {}
               }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public void test() {
                   try {}
                   catch($className | RuntimeException e) {}
               }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun multiVariable(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className f1, f2;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className f1, f2;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun newClass(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = new $className();
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = new $className();
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun parameterizedType(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               Map<$className, $className> m;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               Map<$className, $className> m;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun typeCast(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = ($className) null;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = ($className) null;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun classReference(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class A {
                Class<?> clazz = $className.class;
            }
        """,
        after = """
            import $target$pkg$className;
            public class A {
                Class<?> clazz = $className.class;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun methodSelect(pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = null;
               public void test() { a.foo(); }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = null;
               public void test() { a.foo(); }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeImportTest")
    fun staticImport(pkg: String, className: String) = assertChanged(
        before = """
            import static $source$pkg$className.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        after = """
            import static $target$pkg$className.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeType(null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(2)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType(null, "java.lang.String")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("oldFullyQualifiedTypeName")

        recipe = ChangeType("java.lang.String", null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newFullyQualifiedTypeName")
    }

    // TODO: reduce test brevity. Currently, exhaustive to test all the recipes together once they're written.
    companion object{
        @JvmStatic
        fun changeImportTest() = listOf(
            // xml.bind package
            Arguments.of("", "Binder"),
            Arguments.of("", "ContextFinder"),
            Arguments.of("", "DataBindingException"),
            Arguments.of("", "DatatypeConverter"),
            Arguments.of("", "DatatypeConverterImpl"),
            Arguments.of("", "DatatypeConverterInterface"),
            Arguments.of("", "Element"),
            Arguments.of("", "GetPropertyAction"),
            Arguments.of("", "JAXB"),
            Arguments.of("", "JAXBContext"),
            Arguments.of("", "JAXBContextFactory"),
            Arguments.of("", "JAXBElement"),
            Arguments.of("", "JAXBException"),
            Arguments.of("", "JAXBIntrospector"),
            Arguments.of("", "JAXBPermission"),
            Arguments.of("", "MarshalException"),
            Arguments.of("", "Marshaller"),
            Arguments.of("", "Messages"),
            Arguments.of("", "ModuleUtil"),
            Arguments.of("", "NotIdentifiableEvent"),
            Arguments.of("", "ParseConversionEvent"),
            Arguments.of("", "PrintConversionEvent"),
            Arguments.of("", "PropertyException"),
            Arguments.of("", "SchemaOutputResolver"),
            Arguments.of("", "ServiceLoaderUtil"),
            Arguments.of("", "TypeConstraintException"),
            Arguments.of("", "UnmarshalException"),
            Arguments.of("", "Unmarshaller"),
            Arguments.of("", "ValidationEvent"),
            Arguments.of("", "ValidationEventHandler"),
            Arguments.of("", "ValidationEventLocator"),
            Arguments.of("", "ValidationException"),
            Arguments.of("", "Validator"),
            Arguments.of("", "WhiteSpaceProcessor"),

            // xml.bind.annotation package
            Arguments.of("annotation", "DomHandler"),
            Arguments.of("annotation", "W3CDomHandler"),
            Arguments.of("annotation", "XmlAccessOrder"),
            Arguments.of("annotation", "XmlAccessorOrder"),
            Arguments.of("annotation", "XmlAccessorType"),
            Arguments.of("annotation", "XmlAccessType"),
            Arguments.of("annotation", "XmlAnyAttribute"),
            Arguments.of("annotation", "XmlAnyElement"),
            Arguments.of("annotation", "XmlAttachmentRef"),
            Arguments.of("annotation", "XmlAttribute"),
            Arguments.of("annotation", "XmlElement"),
            Arguments.of("annotation", "XmlElementDecl"),
            Arguments.of("annotation", "XmlElementRef"),
            Arguments.of("annotation", "XmlElementRefs"),
            Arguments.of("annotation", "XmlElements"),
            Arguments.of("annotation", "XmlElementWrapper"),
            Arguments.of("annotation", "XmlEnum"),
            Arguments.of("annotation", "XmlEnumValue"),
            Arguments.of("annotation", "XmlID"),
            Arguments.of("annotation", "XmlIDREF"),
            Arguments.of("annotation", "XmlInlineBinaryData"),
            Arguments.of("annotation", "XmlList"),
            Arguments.of("annotation", "XmlMimeType"),
            Arguments.of("annotation", "XmlMixed"),
            Arguments.of("annotation", "XmlNs"),
            Arguments.of("annotation", "XmlNsForm"),
            Arguments.of("annotation", "XmlRegistry"),
            Arguments.of("annotation", "XmlRootElement"),
            Arguments.of("annotation", "XmlSchema"),
            Arguments.of("annotation", "XmlSchemaType"),
            Arguments.of("annotation", "XmlSchemaTypes"),
            Arguments.of("annotation", "XmlSeeAlso"),
            Arguments.of("annotation", "XmlTransient"),
            Arguments.of("annotation", "XmlType"),
            Arguments.of("annotation", "XmlValue"),
        )
    }
}
