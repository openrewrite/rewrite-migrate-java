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
package org.openrewrite.java.migrate.awt;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("deprecation")
class JavaAwtAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.awt.JavaAwtAPIs");
    }

    @DocumentExample
    @Test
    void showAndHideWindows() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.swing.JDialog;
              import javax.swing.JFrame;

              class Test {
                  void toggle(JFrame frame, JDialog dialog, boolean visible) {
                      frame.show();
                      dialog.show();
                      frame.show(visible);
                      dialog.hide();
                  }
              }
              """,
            """
              import javax.swing.JDialog;
              import javax.swing.JFrame;

              class Test {
                  void toggle(JFrame frame, JDialog dialog, boolean visible) {
                      frame.setVisible(true);
                      dialog.setVisible(true);
                      frame.setVisible(visible);
                      dialog.setVisible(false);
                  }
              }
              """
          )
        );
    }

    @Test
    void showFromSubclassThatDoesNotOverrideIt() {
        rewriteRun(
          //language=java
          java(
            """
              import java.awt.Frame;
              import java.awt.Graphics;

              class MainWindow extends Frame {
                  MainWindow() {
                      pack();
                      show();
                  }

                  @Override
                  public void paint(Graphics g) {
                  }

                  void reopen() {
                      super.show();
                  }
              }
              """,
            """
              import java.awt.Frame;
              import java.awt.Graphics;

              class MainWindow extends Frame {
                  MainWindow() {
                      pack();
                      setVisible(true);
                  }

                  @Override
                  public void paint(Graphics g) {
                  }

                  void reopen() {
                      super.setVisible(true);
                  }
              }
              """
          )
        );
    }

    @Test
    void callsReachingAnOverrideAreReplacedButSuperCallsFromItsClassAreKept() {
        rewriteRun(
          //language=java
          java(
            """
              import java.awt.Dialog;
              import java.awt.Frame;

              class CenteredDialog extends Dialog {
                  CenteredDialog(Frame owner) {
                      super(owner);
                  }

                  @Override
                  public void show() {
                      setLocationRelativeTo(getOwner());
                      super.show();
                  }

                  void showUncentered() {
                      super.show();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Test {
                  void open(CenteredDialog dialog) {
                      dialog.show();
                  }
              }
              """,
            """
              class Test {
                  void open(CenteredDialog dialog) {
                      dialog.setVisible(true);
                  }
              }
              """
          )
        );
    }

    @Test
    void keepSuperCallsFromAnonymousClassThatOverridesAnotherDeprecatedMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import java.awt.Frame;

              class Test {
                  Frame frame = new Frame() {
                      @Override
                      public void hide() {
                          dispose();
                          super.show(false);
                      }
                  };
              }
              """
          )
        );
    }

    @Test
    void keepShowOutsideOfAwt() {
        rewriteRun(
          //language=java
          java(
            """
              class Popup {
                  void show() {
                  }

                  void hide() {
                  }

                  void toggle() {
                      show();
                      hide();
                  }
              }
              """
          )
        );
    }
}
