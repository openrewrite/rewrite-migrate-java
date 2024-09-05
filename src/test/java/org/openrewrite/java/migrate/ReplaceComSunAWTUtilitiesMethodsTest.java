/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

 class ReplaceComSunAWTUtilitiesMethodsTest  implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceComSunAWTUtilitiesMethods("com.test.AWTUtilitiesTest isTranslucencySupported1(com.test.AWTUtilitiesTest.Translucency)",
            "com.test.AWTUtilitiesTest isWindowOpaque(java.awt.Window)",
            "com.test.AWTUtilitiesTest isTranslucencyCapable(java.awt.GraphicsConfiguration)",
            "com.test.AWTUtilitiesTest setWindowOpacity(java.awt.Window,float)",
            "com.test.AWTUtilitiesTest getWindowOpacity(java.awt.Window)",
            "com.test.AWTUtilitiesTest getWindowShape(java.awt.Window)",
            "com.test.AWTUtilitiesTest setComponentMixingCutoutShape(java.awt.Component,java.awt.Shape)"))
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
              """
                package com.test;
                
                import java.awt.Window;
                import java.awt.GraphicsConfiguration;
                import java.awt.Shape;
                import java.awt.Component;

                public class AWTUtilitiesTest {                
                    private static final String TRANSLUCENT = "test";
                    public static enum Translucency {           
                            PERPIXEL_TRANSPARENT,
                            TRANSLUCENT,
                            PERPIXEL_TRANSLUCENT;
                    }                   
                    public static boolean isTranslucencySupported1(Translucency translucencyKind) {
                        return true;
                    }  
                    public static boolean isWindowOpaque(Window win) {
                        return true;        
                    } 
                    public static boolean isTranslucencyCapable(GraphicsConfiguration gc) {
                        return true;        
                    } 
                    public static void setWindowOpacity(Window win,float f) {
                         
                    } 
                    public static float getWindowOpacity(Window win) {
                         return 1;           
                    } 
                    public static Shape getWindowShape(Window win) {
                         Shape sh = null; 
                         return sh;           
                    } 
                    public static void setComponentMixingCutoutShape(Component c, Shape sh){
        
                    }           
                }    
                """
            )
          );
    }

    @Test
    @DocumentExample
    void replaceComSunAWTUtilitiesClassesIsTranslucencySupported() {
        rewriteRun(
          //language=java
          java(
            """
              import com.test.AWTUtilitiesTest;
             
              class Test {
                  void foo() {                
                      boolean f = AWTUtilitiesTest.isTranslucencySupported1(AWTUtilitiesTest.Translucency.TRANSLUCENT);
                      boolean j = AWTUtilitiesTest.isTranslucencySupported1(AWTUtilitiesTest.Translucency.PERPIXEL_TRANSPARENT);
                      boolean k = AWTUtilitiesTest.isTranslucencySupported1(AWTUtilitiesTest.Translucency.PERPIXEL_TRANSLUCENT);             
                  }
              }
              """,
            """
              import java.awt.GraphicsDevice;
              import java.awt.GraphicsDevice.WindowTranslucency;
              import java.awt.GraphicsEnvironment;
              import java.awt.Window;

              class Test {
                  void foo() {
                      boolean f = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isWindowTranslucencySupported(WindowTranslucency.TRANSLUCENT);
                      boolean j = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSPARENT);
                      boolean k = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSLUCENT);
                  }
              }
              """
          )
        );
    }
     @Test
     @DocumentExample
     void replaceComSunAWTUtilitiesClassesRemaining() {
         rewriteRun(
           //language=java
           java(
             """
               package com.test;
               import com.test.AWTUtilitiesTest;
               import java.awt.Window;
               import java.awt.*;
               import javax.swing.*;
               import java.awt.geom.Ellipse2D;
              
               class Test {
                   void foo() {
                          Window win = new Window(new JFrame("test"));
                          boolean f = AWTUtilitiesTest.isWindowOpaque(win); 
                          AWTUtilitiesTest.setWindowOpacity(win,1);
                          float l = AWTUtilitiesTest.getWindowOpacity(win);
                          Shape sh = AWTUtilitiesTest.getWindowShape(win);
                          GraphicsConfiguration gc = null;
                          boolean f = AWTUtilitiesTest.isTranslucencyCapable(gc);           
                          Component c = null;
                          Shape sh = new Ellipse2D.Double(0, 0, c.getWidth(), c.getHeight());
                          AWTUtilitiesTest.setComponentMixingCutoutShape(c, sh);
                   }
               }
               """,
             """
               package com.test;
               import java.awt.Window;
               import java.awt.*;
               import javax.swing.*;
               import java.awt.geom.Ellipse2D;
              
               class Test {
                   void foo() {
                          Window win = new Window(new JFrame("test"));
                          boolean f = win.isOpaque();  
                          win.setOpacity(1);
                          float l = win.getOpacity();
                          Shape sh = win.getShape();
                          GraphicsConfiguration gc = null;
                          boolean f = gc.isTranslucencyCapable();              
                          Component c = null;
                          Shape sh = new Ellipse2D.Double(0, 0, c.getWidth(), c.getHeight());
                          c.setMixingCutoutShape(sh);
                   }
               }
               """
           )
         );
     }
}
