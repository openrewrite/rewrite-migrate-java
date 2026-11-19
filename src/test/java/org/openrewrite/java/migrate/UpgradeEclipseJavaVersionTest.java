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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.xml.Assertions.xml;

class UpgradeEclipseJavaVersionTest implements RewriteTest {
    private static final String CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER";
    private static final String STANDARD_VM = CONTAINER + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";
    private static final String PREFS = "project/.settings/org.eclipse.jdt.core.prefs";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeEclipseJavaVersion(25));
    }

    @Test
    void changesOnlyStandardExecutionEnvironmentInStructuredClasspath() {
        String before = "<?xml version='1.0'?>\r\n<classpath>\r\n" +
                "\t<!-- " + STANDARD_VM + "J2SE-1.5 -->\r\n" +
                "\t<classpathentry path='" + STANDARD_VM + "J2SE-1.5/' kind='con' />\r\n" +
                "\t<classpathentry kind='con' path='" + STANDARD_VM + "JavaSE-26'/>\r\n" +
                "\t<classpathentry kind='con' path='" + STANDARD_VM + "JavaSE-21 (CI)'/>\r\n" +
                "\t<classpathentry kind='con' path='" + CONTAINER + "'/>\r\n" +
                "\t<classpathentry kind='con' path='org.eclipse.pde.core.requiredPlugins'/>\r\n" +
                "\t<classpathentry kind='lib' path='" + STANDARD_VM + "JavaSE-21'/>\r\n" +
                "</classpath>\r\n";
        rewriteRun(xml(before, before.replace("J2SE-1.5/'", "JavaSE-25/'"),
          spec -> spec.path("project/.classpath").noTrim()));
    }

    @Test
    void honorsOlderTargetVersionSpellingAndDoesNotDowngrade() {
        String before = "# org.eclipse.jdt.core.compiler.source=1.5\r\n" +
                "eclipse.preferences.version=1\r\n" +
                "org.eclipse.jdt.core.compiler.source = 1.5  \r\n" +
                "org.eclipse.jdt.core.compiler.compliance:26\r\n" +
                "org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.8";
        rewriteRun(spec -> spec.recipe(new UpgradeEclipseJavaVersion(8)),
          properties(before, before.replace(" = 1.5  ", " = 1.8  "), spec -> spec.path(PREFS).noTrim()),
          xml("<classpath><classpathentry kind='con' path='" + STANDARD_VM + "J2SE-1.5'/></classpath>",
            "<classpath><classpathentry kind='con' path='" + STANDARD_VM + "JavaSE-1.8'/></classpath>",
            spec -> spec.path("project/.classpath"))
        );
    }

    @Test
    void updatesOnlyManifestsInProjectsWithRaisedEclipseLevels() {
        String manifest = "Manifest-Version: 1.0\r\n" +
                "Bundle-RequiredExecutionEnvironment: J2SE-1.5,\r\n JavaSE-26, OSGi/Minimum-1.2, Team JavaSE-21\r\n" +
                "Require-Capability: osgi.extender;filter:=\"(version>=1.5)\",\r\n" +
                " osgi.ee;filter:=\"(&(osgi.ee=JavaSE)\r\n (version=1.5))\",\r\n" +
                " osgi.ee;filter:=\"(&(version>=26)(osgi.ee=JavaSE))\"\r\n" +
                "Bundle-Name: J2SE-1.5\r\n\r\n";
        String upgraded = manifest.replace("Environment: J2SE-1.5,", "Environment: JavaSE-25,")
                .replace(" (version=1.5))", " (version=25))");
        rewriteRun(
          text("org.eclipse.jdt.core.compiler.source=1.5",
            "org.eclipse.jdt.core.compiler.source=25", spec -> spec.path(PREFS)),
          text(manifest, upgraded, spec -> spec.path("project/META-INF/MANIFEST.MF").noTrim()),
          text(manifest, spec -> spec.path("unrelated/META-INF/MANIFEST.MF").noTrim()),
          text("org.eclipse.jdt.core.compiler.source=26",
            spec -> spec.path("already-new/.settings/org.eclipse.jdt.core.prefs")),
          text(manifest, spec -> spec.path("already-new/META-INF/MANIFEST.MF").noTrim())
        );
    }

    @Test
    void preservesFoldingInsideVersionTokensAndNamedSections() {
        String manifest = "Manifest-Version: 1.0\n" +
                "Bundle-RequiredExecutionEnvironment: J2SE-\n 1.5\n" +
                "Require-Capability: osgi.ee;filter:=\"(&(version>=17.0.0)(osgi.ee=JavaSE))\"\n\n" +
                "Name: other\nBundle-RequiredExecutionEnvironment: J2SE-1.5\n";
        rewriteRun(
          text("org.eclipse.jdt.core.compiler.source=1.5", "org.eclipse.jdt.core.compiler.source=25",
            spec -> spec.path(".settings/org.eclipse.jdt.core.prefs")),
          text(manifest, manifest.replace("J2SE-\n 1.5", "JavaS\n E-25").replace("17.0.0", "25.0.0"),
            spec -> spec.path("META-INF/MANIFEST.MF").noTrim())
        );
    }

    @Test
    void leavesOtherFilesAndUnknownValuesAlone() {
        String oldSetting = "org.eclipse.jdt.core.compiler.source=1.5";
        rewriteRun(
          text(oldSetting, spec -> spec.path("project/org.eclipse.jdt.core.prefs")),
          text(oldSetting, spec -> spec.path("project/.settings/other.prefs")),
          text("org.eclipse.jdt.core.compiler.source=${java.version}\n" +
               "org.eclipse.jdt.core.compiler.release=enabled\n", spec -> spec.path(PREFS)),
          xml("<unrelated><classpathentry kind='con' path='" + STANDARD_VM + "J2SE-1.5'/></unrelated>",
            spec -> spec.path("project/.classpath"))
        );
    }
}
