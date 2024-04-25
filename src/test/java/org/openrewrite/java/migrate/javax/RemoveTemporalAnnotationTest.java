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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveTemporalAnnotationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new RemoveTemporalAnnotation());
    }

    @Test
    @DocumentExample
    void removeTemporalAnnotation() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Temporal;
              import javax.persistence.TemporalType;
              import java.sql.Date;
              import java.sql.Time;
              import java.sql.Timestamp;

              public class TemporalDates {
                  @Temporal(TemporalType.DATE)
                  private Date dateDate;

                  @Temporal(TemporalType.TIME)
                  private Date dateTime;

                  @Temporal(TemporalType.DATE)
                  private Time timeDate;

                  @Temporal(TemporalType.TIME)
                  private java.sql.Time timeTime;

                  @Temporal(TemporalType.TIMESTAMP)
                  private java.sql.Time timeTimestamp;

                  @Temporal(TemporalType.TIMESTAMP)
                  private java.sql.Timestamp timestampTimestamp;
              }
              """,
            """
              import javax.persistence.Temporal;
              import javax.persistence.TemporalType;
              import java.sql.Date;
              import java.sql.Time;
              import java.sql.Timestamp;

              public class TemporalDates {
                  private Date dateDate;

                  private Date dateTime;

                  private Time timeDate;

                  private java.sql.Time timeTime;

                  private java.sql.Time timeTimestamp;

                  private java.sql.Timestamp timestampTimestamp;
              }
              """
          )
        );
    }

    @Test
    void dontChangeOtherCombinations() {
        // These combinations require a converter
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Temporal;
              import javax.persistence.TemporalType;
              import java.sql.Date;
              import java.sql.Time;
              import java.sql.Timestamp;

              public class TemporalDates {
                  @Temporal(TemporalType.TIMESTAMP)
                  private Date dateDate;

                  @Temporal(TemporalType.DATE)
                  private java.sql.Timestamp timestampTimestamp;
                  
                  @Temporal(TemporalType.TIME)
                  private java.sql.Timestamp timestampTimestamp;
              }
              """
          )
        );
    }

    @Test
    void allowTemporalOnValidClasses() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Temporal;
              import javax.persistence.TemporalType;
              import java.util.Date;
              import java.util.Calendar;

              public class TemporalDates {
                  @Temporal(TemporalType.TIMESTAMP)
                  private Date dateDate;

                  @Temporal(TemporalType.DATE)
                  private Calendar timestampTimestamp;
              }
              """
          )
        );
    }
}
