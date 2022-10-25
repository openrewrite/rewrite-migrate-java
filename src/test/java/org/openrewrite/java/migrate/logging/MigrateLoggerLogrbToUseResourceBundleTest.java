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
package org.openrewrite.java.migrate.logging;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateLoggerLogrbToUseResourceBundleTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLoggerLogrbToUseResourceBundle());
    }

    @Test
    void logrbToLogrbResourceBundle() {
        rewriteRun(java("""
                package org.openrewrite.example;

                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", "bundleName", "msg");
                    }
                }
            """,
          """
                package org.openrewrite.example;

                import java.util.ResourceBundle;
                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", ResourceBundle.getBundle("bundleName"), "msg");
                    }
                }
            """
        ));
    }

    @Test
    void logrbToLogrbResourceBundleWithTrailingObject() {
        rewriteRun(java("""
                package org.openrewrite.example;

                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", "bundleName", "msg", new Object());
                    }
                }
            """,
          """
                package org.openrewrite.example;

                import java.util.ResourceBundle;
                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", ResourceBundle.getBundle("bundleName"), "msg", new Object());
                    }
                }
            """
        ));
    }

    @Test
    void logrbToLogrbResourceBundleWithTrailingObjectVarargs() {
        rewriteRun(java("""
                package org.openrewrite.example;

                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        Object[] objects = new Object[]{};
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", "bundleName", "msg", objects);
                    }
                }
            """,
          """
                package org.openrewrite.example;

                import java.util.ResourceBundle;
                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        Object[] objects = new Object[]{};
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", ResourceBundle.getBundle("bundleName"), "msg", objects);
                    }
                }
            """
        ));
    }

    @Test
    void logrbToLogrbResourceBundleWithTrailingThrowable() {
        rewriteRun(java("""
                package org.openrewrite.example;

                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", "bundleName", "msg", new Exception());
                    }
                }
            """,
          """
                package org.openrewrite.example;

                import java.util.ResourceBundle;
                import java.util.logging.Level;
                import java.util.logging.Logger;

                public class Test {
                    Logger logger = Logger.getLogger("myLogger");

                    public void method() {
                        logger.logrb(Level.parse("0"), "sourceClass", "sourceMethod", ResourceBundle.getBundle("bundleName"), "msg", new Exception());
                    }
                }
            """
        ));
    }

}
