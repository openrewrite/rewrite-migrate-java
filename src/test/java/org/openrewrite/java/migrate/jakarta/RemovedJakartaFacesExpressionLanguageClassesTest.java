/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class RemovedJakartaFacesExpressionLanguageClassesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jakarta.el-api-4.0.0","jakarta.faces-2.3.19", "jakarta.faces-3.0.3")).recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build().activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedJakartaFacesExpressionLanguageClasses"));
    }

    @Test
    void removedExpressionLanguageTestJakarta_1() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
             
            import jakarta.faces.el.MethodBinding;
            import jakarta.faces.el.PropertyResolver;
            import jakarta.faces.el.ValueBinding;
                        
            public class Test {
             
                 public void testJakarta() {
                      MethodBinding methodBinding = null;
                      PropertyResolver propertyResolver = null;
                      ValueBinding valueBinding = null;           
                 }
            }        
            """, """
            package com.test;
                        
            import jakarta.el.ELResolver;
            import jakarta.el.MethodExpression;
            import jakarta.el.ValueExpression;
                 
            public class Test {
             
                 public void testJakarta() {
                      MethodExpression methodBinding = null;
                      ELResolver propertyResolver = null;
                      ValueExpression valueBinding = null;
                 }
            }   
            """));
    }

    @Test
    void removedExpressionLanguageTestJakarta_2() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
              
            import jakarta.faces.el.VariableResolver;
            import jakarta.faces.el.EvaluationException;
            import jakarta.faces.el.MethodNotFoundException;
            import jakarta.faces.el.PropertyNotFoundException;
            import jakarta.faces.el.ReferenceSyntaxException;
              
            public class Test {
              
                public void testJakarta_1() {            
                    VariableResolver variableResolver = null;
                    EvaluationException evaluationException = null;
                    MethodNotFoundException methodNotFoundException = null;
                    PropertyNotFoundException propertyNotFoundException = null;
                    ReferenceSyntaxException referenceSyntaxException = null; 
                }
            }         
             """, """
            package com.test;
              
            import jakarta.el.ELException;
            import jakarta.el.ELResolver;
            import jakarta.el.MethodNotFoundException;
            import jakarta.el.PropertyNotFoundException;
              
            public class Test {
                      
                public void testJakarta_1() {
                    ELResolver variableResolver = null;
                    ELException evaluationException = null;
                    MethodNotFoundException methodNotFoundException = null;
                    PropertyNotFoundException propertyNotFoundException = null;
                    ELException referenceSyntaxException = null;
                }
            }
            """));
    }
    @Test
    void removedExpressionLanguageTestJavax_1() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
             
            import javax.faces.el.MethodBinding;
            import javax.faces.el.PropertyResolver;
            import javax.faces.el.ValueBinding;
                        
            public class Test {
             
                 public void testJavax() {
                      MethodBinding methodBinding = null;
                      PropertyResolver propertyResolver = null;
                      ValueBinding valueBinding = null;           
                 }
            }        
            """, """
            package com.test;
                        
            import jakarta.el.ELResolver;
            import jakarta.el.MethodExpression;
            import jakarta.el.ValueExpression;
                 
            public class Test {
             
                 public void testJavax() {
                      MethodExpression methodBinding = null;
                      ELResolver propertyResolver = null;
                      ValueExpression valueBinding = null;
                 }
            }   
            """));
    }

    @Test
    void removedExpressionLanguageTestJavax_2() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
              
            import javax.faces.el.VariableResolver;
            import javax.faces.el.EvaluationException;
            import javax.faces.el.MethodNotFoundException;
            import javax.faces.el.PropertyNotFoundException;
            import javax.faces.el.ReferenceSyntaxException;
              
            public class Test {
              
                public void testJavax_1() {            
                    VariableResolver variableResolver = null;
                    EvaluationException evaluationException = null;
                    MethodNotFoundException methodNotFoundException = null;
                    PropertyNotFoundException propertyNotFoundException = null;
                    ReferenceSyntaxException referenceSyntaxException = null; 
                }
            }         
             """, """
            package com.test;
              
            import jakarta.el.ELException;
            import jakarta.el.ELResolver;
            import jakarta.el.MethodNotFoundException;
            import jakarta.el.PropertyNotFoundException;
              
            public class Test {
                      
                public void testJavax_1() {
                    ELResolver variableResolver = null;
                    ELException evaluationException = null;
                    MethodNotFoundException methodNotFoundException = null;
                    PropertyNotFoundException propertyNotFoundException = null;
                    ELException referenceSyntaxException = null;
                }
            }
            """));
    }

}
