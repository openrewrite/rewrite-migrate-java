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
}