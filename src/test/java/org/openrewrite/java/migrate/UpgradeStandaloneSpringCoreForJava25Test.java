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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

class UpgradeStandaloneSpringCoreForJava25Test implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeStandaloneSpringCoreForJava25())
            .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "spring-core"));
    }
    private static final String USE = """
        import org.springframework.core.type.classreading.MetadataReader;
        import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
        class Use {
            private String read(String name) throws java.io.IOException {
                MetadataReader reader = new SimpleMetadataReaderFactory().getMetadataReader(name);
                return reader.getClassMetadata().getClassName();
            }
        }
        """;
    private static String dependency(String artifact, String version) {
        return "<dependency><groupId>org.springframework</groupId><artifactId>" + artifact +
               "</artifactId><version>" + version + "</version></dependency>";
    }
    private static String pom(String version, String extra) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>reader</artifactId>" +
               "<version>1</version><dependencies>" + dependency("spring-core", version) + extra + "</dependencies></project>";
    }
    @ParameterizedTest @ValueSource(strings = {"5.3.0", "5.3.19", "5.3.39"})
    void upgradesOnlyStandaloneOssCore(String version) {
        rewriteRun(pomXml(pom(version, ""), spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>"); return after;
        })), java(USE));
    }
    @Test void alignsDirectJcl() {
        rewriteRun(pomXml(pom("5.3.19", dependency("spring-jcl", "5.3.19")), spec -> spec.after(after -> {
            assertThat(after.split("<version>6.2.11</version>", -1)).hasSize(3); return after;
        })), java(USE));
    }
    @Test void preservesSharedProperty() {
        String before = pom("${spring.version}", "").replace("<dependencies>", "<properties><spring.version>5.3.19</spring.version></properties><dependencies>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<spring.version>5.3.19</spring.version>", "<version>6.2.11</version>"); return after;
        })), java(USE));
    }
    @Test void allowsProvenMojoParent() {
        String parent = "<parent><groupId>org.codehaus.mojo</groupId><artifactId>mojo-parent</artifactId><version>87</version></parent>";
        rewriteRun(pomXml(pom("5.3.19", "").replace("<groupId>example", parent + "<groupId>example"), spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>"); return after;
        })), java(USE));
    }
    @ParameterizedTest @ValueSource(strings = {"5.2.25.RELEASE", "6.2.11", "6.2.12", "7.0.0"})
    void preservesOtherLinesAndNewerVersions(String version) { rewriteRun(pomXml(pom(version, "")), java(USE)); }
    @ParameterizedTest @ValueSource(strings = {"spring-context", "spring-beans", "spring-web"})
    void refusesOtherFrameworkModules(String artifact) { rewriteRun(pomXml(pom("5.3.19", dependency(artifact, "5.3.19"))), java(USE)); }
    @Test void refusesTransitiveFrameworkFamilyInTestScope() {
        rewriteRun(pomXml(pom("5.3.19", dependency("spring-context", "5.3.19").replace("</dependency>", "<scope>test</scope></dependency>"))), java(USE));
    }
    @Test void refusesMixedVersions() { rewriteRun(pomXml(pom("5.3.19", dependency("spring-jcl", "5.3.39"))), java(USE)); }
    @Test void refusesBom() {
        String before = pom("5.3.19", "").replace("<dependencies>", "<dependencyManagement><dependencies>" + dependency("spring-framework-bom", "5.3.19").replace("</dependency>", "<type>pom</type><scope>import</scope></dependency>") + "</dependencies></dependencyManagement><dependencies>");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesManagedFramework() {
        String before = pom("5.3.19", "").replace("<dependencies>", "<dependencyManagement><dependencies>" + dependency("spring-context", "5.3.19") + "</dependencies></dependencyManagement><dependencies>");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesFrameworkParent() {
        String before = pom("5.3.19", "").replace("<groupId>example", "<parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>2.7.18</version></parent><groupId>example");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesUnknownParent() {
        String before = pom("5.3.19", "").replace("<groupId>example", "<parent><groupId>org.apache</groupId><artifactId>apache</artifactId><version>33</version></parent><groupId>example");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesProfileGraph() {
        rewriteRun(pomXml(pom("5.3.19", "").replace("</project>", "<profiles><profile><id>other</id><dependencies>" + dependency("spring-context", "5.3.19") + "</dependencies></profile></profiles></project>")), java(USE));
    }
    @Test void refusesPublicReturnType() {
        rewriteRun(pomXml(pom("5.3.19", "")), java(USE.replace("private String read", "public MetadataReader read").replace("return reader.getClassMetadata().getClassName();", "return reader;")));
    }
    @Test void refusesProtectedParameterType() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { protected void accept(MetadataReader reader) {} }
            """));
    }
    @Test void refusesPublicGenericExposure() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { public java.util.List<MetadataReader> readers() { return null; } }
            """));
    }
    @Test void refusesPublicField() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { public MetadataReader reader; }
            """));
    }
    @Test void refusesSpringInheritance() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
            class Use extends SimpleMetadataReaderFactory {}
            """));
    }
    @Test void refusesOtherCoreApi() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.SpringVersion;
            class Use { private String version() { return SpringVersion.getVersion(); } }
            """));
    }
    @Test void refusesNoTypedUsage() { rewriteRun(pomXml(pom("5.3.19", ""))); }
    @Test void refusesUnknownSourceKind() { rewriteRun(pomXml(pom("5.3.19", "")), java(USE), text("class Unknown", spec -> spec.path("src/main/kotlin/Unknown.kt"))); }
    @Test void refusesUnknownGraph() { rewriteRun(text(pom("5.3.19", ""), spec -> spec.path("pom.xml")), java(USE)); }
    @Test void doesNotChangeOtherModule() {
        rewriteRun(pomXml(pom("5.3.19", ""), spec -> spec.path("first/pom.xml")), java(USE, spec -> spec.path("second/src/main/java/Use.java")));
    }
    @Test void refusesTransitiveSpringFromNonSpringDependency() {
        String extra = "<dependency><groupId>org.apache.camel</groupId><artifactId>camel-spring</artifactId><version>3.14.0</version><scope>test</scope></dependency>";
        rewriteRun(pomXml(pom("5.3.19", extra)), java(USE));
    }
    @Test void refusesOtherMethodOnAllowedType() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.util.ClassUtils;
            class Use { private Class<?> load() throws ClassNotFoundException {
                return ClassUtils.forName("example.Type", getClass().getClassLoader());
            } }
            """));
    }
    @Test void refusesOpaqueRequestedVersion() {
        rewriteRun(pomXml(pom("5.3.19", ""), spec -> spec.mapBeforeRecipe(doc -> {
            org.openrewrite.maven.tree.MavenResolutionResult model = doc.getMarkers()
                .findFirst(org.openrewrite.maven.tree.MavenResolutionResult.class).orElseThrow();
            org.openrewrite.maven.tree.Pom requested = model.getPom().getRequested();
            return doc.withMarkers(doc.getMarkers().setByType(model.withPom(model.getPom().withRequested(
                requested.withDependencies(requested.getDependencies().stream()
                    .map(dependency -> dependency.withGav(dependency.getGav().withVersion("${unknown}")))
                    .collect(java.util.stream.Collectors.toList()))))));
        })), java(USE));
    }
    @Test void refusesUnparsedChildGraph() {
        rewriteRun(pomXml(pom("5.3.19", "")),
            text(pom("5.3.19", ""), spec -> spec.path("child/pom.xml")),
            java(USE, spec -> spec.path("child/src/main/java/Use.java")));
    }
    @Test void allowsBuildOnlyProfile() {
        String before = pom("5.3.19", "").replace("</project>", "<profiles><profile><id>release</id><properties><plugin.version>1</plugin.version></properties><build><plugins><plugin><artifactId>maven-site-plugin</artifactId><version>${plugin.version}</version></plugin></plugins></build></profile></profiles></project>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>", "<plugin.version>1</plugin.version>"); return after;
        })), java(USE));
    }
    @Test void refusesProfileVersionOverride() {
        String before = pom("${spring.version}", "").replace("<dependencies>", "<properties><spring.version>5.3.19</spring.version></properties><dependencies>")
            .replace("</project>", "<profiles><profile><id>future</id><properties><spring.version>6.2.12</spring.version></properties></profile></profiles></project>");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void supportsObservedResourceMetadataCalls() {
        rewriteRun(pomXml(pom("5.3.19", ""), spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>"); return after;
        })), java("""
            import org.springframework.core.io.Resource;
            import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
            import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
            class Use {
                private void read(ClassLoader loader) throws java.io.IOException {
                    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(loader);
                    Resource[] resources = resolver.getResources("classpath*:example/*.class");
                    Resource single = resolver.getResource("example/One.class");
                    if (single.exists()) {
                        String name = single.getFilename();
                        new SimpleMetadataReaderFactory().getMetadataReader(single).getClassMetadata().getClassName();
                    }
                }
            }
            """));
    }
    @Test void refusesReflectiveSpringUsage() {
        rewriteRun(pomXml(pom("5.3.19", "")), java(USE.replace("return reader.getClassMetadata().getClassName();", "String implementation = \"org.springframework.core.CustomReader\"; return reader.getClassMetadata().getClassName();")));
    }
    @Test void refusesUnprovenSpringField() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.util.ClassUtils;
            class Use { private String suffix() { return ClassUtils.ARRAY_SUFFIX; } }
            """));
    }
    @Test void handlesRecursiveNonSpringGenericSignature() {
        rewriteRun(pomXml(pom("5.3.19", ""), spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>"); return after;
        })), java(USE.replace("class Use {", "class Use { public <T extends Comparable<T>> T identity(T input) { return input; }")));
    }
    @Test void refusesAggregatorGraph() {
        String before = pom("5.3.19", "").replace("<dependencies>", "<packaging>pom</packaging><modules><module>child</module></modules><dependencies>");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesIndirectProfileVersionOverride() {
        String before = pom("${spring.version}", "")
            .replace("<dependencies>", "<properties><base.version>5.3.19</base.version><spring.version>${base.version}</spring.version></properties><dependencies>")
            .replace("</project>", "<profiles><profile><id>future</id><properties><base.version>6.2.12</base.version></properties></profile></profiles></project>");
        rewriteRun(pomXml(before), java(USE));
    }
    @Test void refusesBootBuildPlugin() {
        String plugin = "<build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>2.7.18</version></plugin></plugins></build>";
        rewriteRun(pomXml(pom("5.3.19", "").replace("</project>", plugin + "</project>")), java(USE));
    }
    @Test void refusesBootPluginInInactiveProfile() {
        String profile = "<profiles><profile><id>boot</id><build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>2.7.18</version></plugin></plugins></build></profile></profiles>";
        rewriteRun(pomXml(pom("5.3.19", "").replace("</project>", profile + "</project>")), java(USE));
    }
    @Test void refusesPublicMethodTypeParameterBound() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { public <T extends MetadataReader> void accept() {} }
            """));
    }
    @Test void refusesClassTypeParameterBound() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use<T extends MetadataReader> {}
            """));
    }
    @Test void refusesRecordComponentApi() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            record Use(MetadataReader reader) {}
            """));
    }
    @Test void refusesSpringFieldWithPotentialGeneratedAccessors() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { private MetadataReader reader; }
            """));
    }
    @Test void refusesAnnotatedPrivateSpringSignature() {
        rewriteRun(pomXml(pom("5.3.19", "")), java("""
            import org.springframework.core.type.classreading.MetadataReader;
            class Use { @Deprecated private MetadataReader reader() { return null; } }
            """));
    }
    @Test void refreshesDependencyModelAfterLocalPin() {
        rewriteRun(pomXml(pom("5.3.19", ""), spec -> spec.after(after -> {
            assertThat(after).contains("<version>6.2.11</version>"); return after;
        }).afterRecipe(doc -> {
            org.openrewrite.maven.tree.MavenResolutionResult model = doc.getMarkers()
                .findFirst(org.openrewrite.maven.tree.MavenResolutionResult.class).orElseThrow();
            assertThat(model.findDependencies("org.springframework", "spring-core", null))
                .isNotEmpty().allSatisfy(dependency -> assertThat(dependency.getVersion()).isEqualTo("6.2.11"));
            assertThat(model.findDependencies("org.springframework", "spring-jcl", null))
                .isNotEmpty().allSatisfy(dependency -> assertThat(dependency.getVersion()).isEqualTo("6.2.11"));
        })), java(USE));
    }
}
