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

public class RemovedUIComponentConstantTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "uicomponent"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedUIComponentConstant"));
    }

    @Test
    void useBothConstants() {
        rewriteRun(
          //language=java
          java("""
              package com.test;
                                    
              import jakarta.faces.component.UIComponent;
                                
              public class UseUIComponentConstants {
                                
              	public static void main(String[] args) {
              		String str = UIComponent.CURRENT_COMPONENT;
              		String str2 = UIComponent.CURRENT_COMPOSITE_COMPONENT;
              		System.out.println(str);
              		System.out.println(str2);
              	}
              }
              """,
            """
              package com.test;
                             
              import jakarta.faces.component.UIComponent;
                             
              public class UseUIComponentConstants {
                             
              	public static void main(String[] args) {
              		String str = UIComponent.getCurrentComponent();
              		String str2 = UIComponent.getCurrentCompositeComponent();
              		System.out.println(str);
              		System.out.println(str2);
              	}
              }
              """));
    }

    @Test
    void useCurrentComponentConstants() {
        rewriteRun(
          //language=java
          java("""
              package com.test;
                                    
              import jakarta.faces.component.UIComponent;
                                
              public class UseUIComponentConstants {
                                
              	public static void main(String[] args) {
              		String str = UIComponent.CURRENT_COMPONENT;
              		System.out.println(str);
              	}
              }
              """,
            """
              package com.test;
                             
              import jakarta.faces.component.UIComponent;
                             
              public class UseUIComponentConstants {
                             
              	public static void main(String[] args) {
              		String str = UIComponent.getCurrentComponent();
              		System.out.println(str);
              	}
              }
              """));
    }

    @Test
    void useCurrentCompositeComponentConstants() {
        rewriteRun(
          //language=java
          java("""
              package com.test;
                                    
              import jakarta.faces.component.UIComponent;
                                
              public class UseUIComponentConstants {
                                
              	public static void main(String[] args) {
              		String str = UIComponent.CURRENT_COMPOSITE_COMPONENT;
              		System.out.println(str);
              	}
              }
              """,
            """
              package com.test;
                             
              import jakarta.faces.component.UIComponent;
                             
              public class UseUIComponentConstants {
                             
              	public static void main(String[] args) {
              		String str = UIComponent.getCurrentCompositeComponent();
              		System.out.println(str);
              	}
              }
              """));
    }

}
