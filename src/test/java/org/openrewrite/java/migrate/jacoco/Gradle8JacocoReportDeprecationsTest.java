package org.openrewrite.java.migrate.jacoco;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class Gradle8JacocoReportDeprecationsTest implements RewriteTest {

    @Test
    void enabledDeprecatedInCollapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.enabled = false
              jacocoTestReport.reports.csv.enabled = true
              jacocoTestReport.reports.html.enabled = false

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.required = false
              jacocoTestReport.reports.csv.required = true
              jacocoTestReport.reports.html.required = false

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInNormalSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.enabled = false
                      csv.enabled = true
                      html.enabled = false
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.required = false
                      csv.required = true
                      html.required = false
                  }
              }

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInElapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          enabled = false
                      }
                      csv {
                          enabled = false
                      }
                      html {
                          enabled = false
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          required = false
                      }
                      csv {
                          required = false
                      }
                      html {
                          required = false
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInMixedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.enabled = false
                      csv.enabled = false
                      html {
                          enabled = false
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.required = false
                      csv.required = false
                      html {
                          required = false
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInSemiCollapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.enabled = false
                  reports.csv.enabled = false
                  reports.html.enabled = false
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.required = false
                  reports.csv.required = false
                  reports.html.required = false
              }

              """
          )
        );
    }

    @Test
    void enabledInAnotherExtensionNotTouched() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              tasks.register("example", JavaCompile) {
                   xml.enabled = false
                   jacocoTestReport.reports.html.enabled = false
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInCollapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.destination = layout.buildDirectory.dir('jacocoXml')
              jacocoTestReport.reports.csv.destination = layout.buildDirectory.dir('jacocoCsv')
              jacocoTestReport.reports.html.destination = layout.buildDirectory.dir('jacocoHtml')

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
              jacocoTestReport.reports.csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
              jacocoTestReport.reports.html.outputLocation = layout.buildDirectory.dir('jacocoHtml')

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInNormalSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.destination = layout.buildDirectory.dir('jacocoXml')
                      csv.destination = layout.buildDirectory.dir('jacocoCsv')
                      html.destination = layout.buildDirectory.dir('jacocoHtml')
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                      csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
                  }
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInElapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          destination = layout.buildDirectory.dir('jacocoXml')
                      }
                      csv {
                          destination = layout.buildDirectory.dir('jacocoCsv')
                      }
                      html {
                          destination = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          outputLocation = layout.buildDirectory.dir('jacocoXml')
                      }
                      csv {
                          outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      }
                      html {
                          outputLocation = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInMixedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.destination = layout.buildDirectory.dir('jacocoXml')
                      csv.destination = layout.buildDirectory.dir('jacocoCsv')
                      html {
                          destination = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                      csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      html {
                          outputLocation = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInSemiCollapsedSyntax() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.destination = layout.buildDirectory.dir('jacocoXml')
                  reports.csv.destination = layout.buildDirectory.dir('jacocoCsv')
                  reports.html.destination = layout.buildDirectory.dir('jacocoHtml')
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                  reports.csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                  reports.html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
              }

              """
          )
        );
    }

    @Test
    void destinationInAnotherExtensionNotTouched() {
        rewriteRun(
          spec -> spec.recipe(new Gradle8JacocoReportDeprecations()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              tasks.register("example", JavaCompile) {
                   xml.destination = false
                   jacocoTestReport.reports.html.destination = layout.buildDirectory.dir('jacocoHtml')
              }

              """
          )
        );
    }
}
