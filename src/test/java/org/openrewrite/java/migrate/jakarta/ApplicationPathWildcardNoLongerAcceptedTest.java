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
import org.openrewrite.java.AddOrUpdateAnnotationAttribute;

public class ApplicationPathWildcardNoLongerAcceptedTest  implements RewriteTest{
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().
            classpathFromResources(new InMemoryExecutionContext(), "jakarta.ws.rs-api-2.1.3","jakarta.ws.rs-api-3.1.0")).
          recipe(Environment.builder().
            scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.ApplicationPathWildcardNoLongerAccepted"));
    }

    @Test
    void updateAnnotation() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
                    
            import jakarta.ws.rs.ApplicationPath;
            import jakarta.ws.rs.core.Application;
            
                       
            @ApplicationPath("should-flag/*")
            public class ApplicationPathWithWildcard extends Application {             
                
            }
             """, """
            
            package com.test;
            
            import jakarta.ws.rs.ApplicationPath;
            import jakarta.ws.rs.core.Application;
          
            
            @ApplicationPath("should-flag")    
            public class ApplicationPathWithWildcard extends Application { 
               
            }
            """));
    }
    @Test
    void updateAnnotationValue() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
                    
            import jakarta.ws.rs.ApplicationPath;
            import jakarta.ws.rs.core.Application;
            
                       
            @ApplicationPath(value="should-flag/*")
            public class ApplicationPathWithWildcard extends Application {             
                
            }
             """, """
            
            package com.test;
            
            import jakarta.ws.rs.ApplicationPath;
            import jakarta.ws.rs.core.Application;
          
            
            @ApplicationPath(value="should-flag")    
            public class ApplicationPathWithWildcard extends Application { 
               
            }
            """));
    }
    @Test
    void noUpdateAnnotation() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
            import jakarta.ws.rs.ApplicationPath;
            import jakarta.ws.rs.core.Application;
                      
            @ApplicationPath("should-not-flag*")
            public class TestAnnotate extends Application { }
             
            """));
    }
}
