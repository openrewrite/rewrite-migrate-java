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
package org.openrewrite.java.migrate.snakeyaml;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.tree.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class AddSnakeYamlOptions extends ScanningRecipe<AddSnakeYamlOptions.Accumulator> {

    private static final String LOADER_OPTIONS = "org.yaml.snakeyaml.LoaderOptions";
    private static final String DUMPER_OPTIONS = "org.yaml.snakeyaml.DumperOptions";

    /**
     * The SnakeYAML types whose constructors all take options in 2.x, and the type of those options.
     */
    private static final Map<String, String> OPTIONS_TYPE = new HashMap<>();

    static {
        OPTIONS_TYPE.put("org.yaml.snakeyaml.constructor.BaseConstructor", LOADER_OPTIONS);
        OPTIONS_TYPE.put("org.yaml.snakeyaml.constructor.SafeConstructor", LOADER_OPTIONS);
        OPTIONS_TYPE.put("org.yaml.snakeyaml.constructor.Constructor", LOADER_OPTIONS);
        OPTIONS_TYPE.put("org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor", LOADER_OPTIONS);
        OPTIONS_TYPE.put("org.yaml.snakeyaml.representer.Representer", DUMPER_OPTIONS);
    }

    /**
     * A SnakeYAML 1.x coordinate written in a Gradle build file, which is still there when the version wasn't raised.
     */
    private static final Pattern SNAKEYAML_1_IN_BUILD_FILE = Pattern.compile(
            "org\\.yaml:snakeyaml:1\\.|name\\s*[:=]\\s*['\"]snakeyaml['\"][^\\n]*version\\s*[:=]\\s*['\"]1\\.");

    @Getter
    final String displayName = "Pass `LoaderOptions` and `DumperOptions` to SnakeYAML constructors";

    @Getter
    final String description = "SnakeYAML 2.0 removed the constructors of `BaseConstructor`, `SafeConstructor`, " +
            "`Constructor` and `CustomClassLoaderConstructor` that take no `LoaderOptions`, and the constructor of " +
            "`Representer` that takes no `DumperOptions`. The removed constructors used a `new LoaderOptions()` or " +
            "`new DumperOptions()` of their own, so this passes one to them. That covers `super(..)` calls in " +
            "subclasses, and subclasses that call `super()` implicitly, which get a constructor of their own if they " +
            "declare none. SnakeYAML 1.26 added most of the constructors this calls, and 2.0 those of " +
            "`CustomClassLoaderConstructor`. A call is only changed where the SnakeYAML a project compiles against " +
            "has the constructor already, or where `UpgradeSnakeYaml_2` raises the version: in a Maven module whose " +
            "pom or a parent pom in the repository declares it, and in a Gradle project that declares it, once a " +
            "version written in its build file is 2.x. A version a BOM such as Spring Boot's manages stays as it is.";

    public static class Accumulator {
        /**
         * The directory of each Maven module, and where its SnakeYAML version comes from.
         */
        final Map<Path, MavenModule> mavenModules = new HashMap<>();

        /**
         * The poms in the repository, which a module may import as a BOM.
         */
        final Set<GroupArtifact> projectPoms = new HashSet<>();

        /**
         * The directory of each Gradle project, and whether it can use SnakeYAML 2.
         */
        final Map<Path, Boolean> gradleProjects = new HashMap<>();

        /**
         * Whether a constructor that only SnakeYAML 2 has can be called in the source file, because the version is
         * raised there or is 2.x already. A Maven module qualifies when it or a parent pom in the repository declares
         * the version, which {@code UpgradeDependencyVersion} raises, since a module whose parent pom changes still
         * shows the version it had. A Gradle project qualifies when its build shows SnakeYAML 2, as it does in the
         * cycle after a version written in its build file is raised, or when it declares the version elsewhere,
         * such as in a version catalog or {@code gradle.properties}, whose raise its build doesn't show.
         */
        boolean canUseSnakeYaml2(Path sourcePath) {
            Path dir = sourcePath.getParent();
            while (true) {
                Path key = dir == null ? Paths.get("") : dir;
                MavenModule maven = mavenModules.get(key);
                if (maven != null) {
                    return maven.declaresVersion || (maven.bom != null && projectPoms.contains(maven.bom));
                }
                Boolean gradle = gradleProjects.get(key);
                if (gradle != null) {
                    return gradle;
                }
                if (dir == null) {
                    return false;
                }
                dir = dir.getParent();
            }
        }
    }

    @Value
    static class MavenModule {
        /**
         * The module or one of its parents in the repository has the SnakeYAML version.
         */
        boolean declaresVersion;

        /**
         * The BOM that manages the SnakeYAML version, if one does.
         */
        @Nullable
        GroupArtifact bom;

        static MavenModule of(MavenResolutionResult module) {
            for (MavenResolutionResult pom = module; pom != null; pom = pom.getParent()) {
                Pom requested = pom.getPom().getRequested();
                for (Dependency dependency : requested.getDependencies()) {
                    if (isSnakeYaml(dependency.getGroupId(), dependency.getArtifactId()) && dependency.getVersion() != null) {
                        return new MavenModule(true, null);
                    }
                }
                for (ManagedDependency managed : requested.getDependencyManagement()) {
                    if (managed instanceof ManagedDependency.Defined &&
                        isSnakeYaml(managed.getGroupId(), managed.getArtifactId()) && managed.getVersion() != null) {
                        return new MavenModule(true, null);
                    }
                }
            }
            for (ResolvedManagedDependency managed : module.getPom().getDependencyManagement()) {
                if (isSnakeYaml(managed.getGroupId(), managed.getArtifactId())) {
                    ResolvedGroupArtifactVersion bom = managed.getBomGav();
                    return new MavenModule(false, bom == null ? null : new GroupArtifact(bom.getGroupId(), bom.getArtifactId()));
                }
            }
            return new MavenModule(false, null);
        }
    }

    private static boolean isSnakeYaml(@Nullable String groupId, @Nullable String artifactId) {
        return "org.yaml".equals(groupId) && "snakeyaml".equals(artifactId);
    }

    private static boolean canUseSnakeYaml2(SourceFile buildFile, GradleProject gradle) {
        if (hasSnakeYaml2(gradle)) {
            return true;
        }
        for (GradleDependencyConfiguration configuration : gradle.getConfigurations()) {
            for (Dependency dependency : configuration.getRequested()) {
                if (isSnakeYaml(dependency.getGroupId(), dependency.getArtifactId()) && dependency.getVersion() != null) {
                    return !SNAKEYAML_1_IN_BUILD_FILE.matcher(buildFile.printAll()).find();
                }
            }
        }
        return false;
    }

    private static boolean hasSnakeYaml2(GradleProject gradle) {
        for (GradleDependencyConfiguration configuration : gradle.getConfigurations()) {
            for (Dependency dependency : configuration.getRequested()) {
                if (isSnakeYaml2(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
                    return true;
                }
            }
            for (ResolvedDependency dependency : configuration.getResolved()) {
                if (isSnakeYaml2(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSnakeYaml2(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
        if (!isSnakeYaml(groupId, artifactId) || version == null) {
            return false;
        }
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return end > 1 || (end == 1 && version.charAt(0) >= '2');
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                Path dir = sourceFile.getSourcePath().getParent();
                Path key = dir == null ? Paths.get("") : dir;
                Optional<MavenResolutionResult> maven = sourceFile.getMarkers().findFirst(MavenResolutionResult.class);
                if (maven.isPresent()) {
                    acc.mavenModules.put(key, MavenModule.of(maven.get()));
                    ResolvedPom pom = maven.get().getPom();
                    acc.projectPoms.add(new GroupArtifact(pom.getGroupId(), pom.getArtifactId()));
                }
                Optional<GradleProject> gradle = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (gradle.isPresent() && sourceFile.getSourcePath().getFileName().toString().startsWith("build.gradle")) {
                    acc.gradleProjects.merge(key, canUseSnakeYaml2(sourceFile, gradle.get()), Boolean::logicalOr);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("org.yaml.snakeyaml.constructor.*", false),
                        new UsesType<>("org.yaml.snakeyaml.representer.Representer", false)),
                new JavaIsoVisitor<ExecutionContext>() {

                    /**
                     * Whether a constructor that the SnakeYAML on the classpath lacks can be called, as SnakeYAML 2
                     * has it.
                     */
                    boolean canUseSnakeYaml2;

                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                        // The arguments are written as Java, so leave Kotlin and Groovy alone
                        return sourceFile instanceof J.CompilationUnit;
                    }

                    @Override
                    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                        canUseSnakeYaml2 = acc.canUseSnakeYaml2(cu.getSourcePath());
                        return super.visitCompilationUnit(cu, ctx);
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = super.visitNewClass(newClass, ctx);
                        if (n.getClazz() == null) {
                            return n;
                        }
                        return addOptions(n, TypeUtils.asFullyQualified(n.getClazz().getType()));
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (m.getSelect() == null && "super".equals(m.getSimpleName()) && m.getMethodType() != null) {
                            return addOptions(m, m.getMethodType().getDeclaringType());
                        }
                        return m;
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                        J.Block body = md.getBody();
                        if (!md.isConstructor() || body == null || callsAnotherConstructor(body)) {
                            return md;
                        }
                        J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        J.MethodInvocation superCall = implicitSuperCall(cd, canUseSnakeYaml2);
                        if (superCall == null) {
                            return md;
                        }
                        maybeAddOptionsImport(superCall);

                        Cursor bodyCursor = new Cursor(new Cursor(getCursor().getParentOrThrow(), md), body);
                        superCall = autoFormat(superCall.withPrefix(Space.format("\n")), ctx, bodyCursor);
                        if (body.getStatements().isEmpty()) {
                            body = body.withEnd(Space.format("\n" + md.getPrefix().getIndent()));
                        }
                        return md.withBody(body.withStatements(ListUtils.insert(body.getStatements(), superCall, 0)));
                    }

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                        if (cd.getType() == null || declaresConstructor(cd) || hasLombokConstructor(cd)) {
                            return cd;
                        }
                        J.MethodInvocation superCall = implicitSuperCall(cd, canUseSnakeYaml2);
                        if (superCall == null) {
                            return cd;
                        }
                        maybeAddOptionsImport(superCall);

                        List<Statement> statements = cd.getBody().getStatements();
                        int index = 0;
                        while (index < statements.size() &&
                               (statements.get(index) instanceof J.VariableDeclarations || statements.get(index) instanceof J.Block)) {
                            index++;
                        }
                        Cursor bodyCursor = new Cursor(new Cursor(getCursor().getParentOrThrow(), cd), cd.getBody());
                        J.MethodDeclaration constructor = autoFormat(defaultConstructor(cd, superCall).withPrefix(Space.format("\n")), ctx, bodyCursor);
                        // A blank line between it and what comes before, unless it comes first
                        constructor = constructor.withPrefix(Space.format((index == 0 ? "\n" : "\n\n") + constructor.getPrefix().getIndent()));

                        List<Statement> newStatements = new ArrayList<>(statements);
                        if (index < newStatements.size()) {
                            Statement next = newStatements.get(index);
                            String whitespace = next.getPrefix().getWhitespace();
                            if (whitespace.indexOf('\n') == whitespace.lastIndexOf('\n')) {
                                newStatements.set(index, next.withPrefix(next.getPrefix().withWhitespace("\n" + whitespace)));
                            }
                        }
                        newStatements.add(index, constructor);
                        return cd.withBody(cd.getBody().withStatements(newStatements));
                    }

                    private void maybeAddOptionsImport(J.MethodInvocation superCall) {
                        if (superCall.getMethodType() != null) {
                            List<JavaType> parameterTypes = superCall.getMethodType().getParameterTypes();
                            JavaType.FullyQualified options = TypeUtils.asFullyQualified(parameterTypes.get(parameterTypes.size() - 1));
                            if (options != null) {
                                maybeAddImport(options.getFullyQualifiedName());
                            }
                        }
                    }

                    /**
                     * Appends the options to a call of a constructor that SnakeYAML 2 removed.
                     */
                    private <M extends MethodCall> M addOptions(M call, JavaType.@Nullable FullyQualified type) {
                        JavaType.Method constructor = call.getMethodType();
                        if (type == null || constructor == null) {
                            return call;
                        }
                        String optionsType = OPTIONS_TYPE.get(type.getFullyQualifiedName());
                        if (optionsType == null) {
                            return call;
                        }
                        for (JavaType parameterType : constructor.getParameterTypes()) {
                            if (TypeUtils.isOfClassType(parameterType, optionsType)) {
                                return call;
                            }
                        }

                        JavaType.Method withOptions = constructorWithOptions(type, constructor, optionsType, canUseSnakeYaml2);
                        if (withOptions == null) {
                            return call;
                        }
                        List<JavaType> parameterTypes = withOptions.getParameterTypes();
                        JavaType.FullyQualified options = TypeUtils.asFullyQualified(parameterTypes.get(parameterTypes.size() - 1));
                        if (options == null) {
                            return call;
                        }

                        List<Expression> args = call.getArguments();
                        boolean noArgs = args.isEmpty() || (args.size() == 1 && args.get(0) instanceof J.Empty);
                        Space prefix = Space.SINGLE_SPACE;
                        if (noArgs) {
                            prefix = Space.EMPTY;
                        } else {
                            String lastArgWhitespace = args.get(args.size() - 1).getPrefix().getLastWhitespace();
                            if (lastArgWhitespace.contains("\n")) {
                                prefix = Space.format(lastArgWhitespace);
                            }
                        }
                        J.NewClass newOptions = newOptions(options, prefix);

                        //noinspection unchecked
                        M updated = (M) call
                                .withArguments(noArgs ? singletonList(newOptions) : ListUtils.concat(args, newOptions))
                                .withMethodType(withOptions);
                        if (updated instanceof J.MethodInvocation) {
                            J.MethodInvocation m = (J.MethodInvocation) updated;
                            //noinspection unchecked
                            updated = (M) m.withName(m.getName().withType(withOptions));
                        }
                        maybeAddImport(optionsType);
                        return updated;
                    }
                });
    }

    private static boolean callsAnotherConstructor(J.Block body) {
        if (body.getStatements().isEmpty() || !(body.getStatements().get(0) instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation first = (J.MethodInvocation) body.getStatements().get(0);
        return first.getSelect() == null && ("super".equals(first.getSimpleName()) || "this".equals(first.getSimpleName()));
    }

    private static boolean declaresConstructor(J.ClassDeclaration cd) {
        for (Statement statement : cd.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration && ((J.MethodDeclaration) statement).isConstructor()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lombok generates the constructors of these classes, which call {@code super()} implicitly.
     */
    private static boolean hasLombokConstructor(J.ClassDeclaration cd) {
        for (J.Annotation annotation : cd.getAllAnnotations()) {
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
            if (type != null && type.getFullyQualifiedName().startsWith("lombok.")) {
                String name = type.getClassName();
                if (name.endsWith("ArgsConstructor") || "Data".equals(name) || "Value".equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The {@code super()} call a constructor of this class makes implicitly, if its superclass is one whose
     * constructors take options in SnakeYAML 2.
     */
    private static J.@Nullable MethodInvocation implicitSuperCall(J.@Nullable ClassDeclaration cd, boolean canUseSnakeYaml2) {
        if (cd == null || cd.getKind() != J.ClassDeclaration.Kind.Type.Class || cd.getType() == null) {
            return null;
        }
        JavaType.FullyQualified supertype = cd.getType().getSupertype();
        if (supertype == null || !OPTIONS_TYPE.containsKey(supertype.getFullyQualifiedName())) {
            return null;
        }
        JavaType.Method noArgs = null;
        for (JavaType.Method method : supertype.getMethods()) {
            if (method.isConstructor() && method.getParameterTypes().isEmpty()) {
                noArgs = method;
                break;
            }
        }
        if (noArgs == null) {
            if (!supertype.getMethods().isEmpty()) {
                // Like CustomClassLoaderConstructor, which has no constructor without arguments
                return null;
            }
            noArgs = new JavaType.Method(null, Flag.Public.getBitMask(), supertype, "<constructor>", supertype,
                    emptyList(), emptyList(), emptyList(), emptyList(), null, null);
        }
        J.MethodInvocation superCall = new J.MethodInvocation(randomId(), Space.EMPTY, Markers.EMPTY, null, null,
                new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "super", noArgs, null),
                JContainer.build(Space.EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                noArgs);
        String optionsType = OPTIONS_TYPE.get(supertype.getFullyQualifiedName());
        JavaType.Method withOptions = constructorWithOptions(supertype, noArgs, optionsType, canUseSnakeYaml2);
        if (withOptions == null) {
            return null;
        }
        List<JavaType> parameterTypes = withOptions.getParameterTypes();
        JavaType.FullyQualified options = TypeUtils.asFullyQualified(parameterTypes.get(parameterTypes.size() - 1));
        if (options == null) {
            return null;
        }
        return superCall
                .withArguments(singletonList(newOptions(options, Space.EMPTY)))
                .withMethodType(withOptions)
                .withName(superCall.getName().withType(withOptions));
    }

    /**
     * The constructor of the type that takes the same parameters followed by the options, as SnakeYAML 2 has it.
     * If the SnakeYAML on the classpath doesn't have it, it is only called where SnakeYAML 2 can be used.
     */
    private static JavaType.@Nullable Method constructorWithOptions(JavaType.FullyQualified type, JavaType.Method constructor,
                                                                    String optionsType, boolean canUseSnakeYaml2) {
        List<JavaType> parameterTypes = constructor.getParameterTypes();
        for (JavaType.Method method : type.getMethods()) {
            List<JavaType> candidate = method.getParameterTypes();
            if (!method.isConstructor() || candidate.size() != parameterTypes.size() + 1 ||
                !TypeUtils.isOfClassType(candidate.get(candidate.size() - 1), optionsType)) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (!TypeUtils.isOfType(parameterTypes.get(i), candidate.get(i))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        if (!canUseSnakeYaml2) {
            return null;
        }
        // Not in the SnakeYAML on the classpath, as the CustomClassLoaderConstructor ones are new in 2.0
        return constructor
                .withParameterNames(ListUtils.concat(constructor.getParameterNames(),
                        LOADER_OPTIONS.equals(optionsType) ? "loadingConfig" : "options"))
                .withParameterTypes(ListUtils.concat(parameterTypes, JavaType.ShallowClass.build(optionsType)));
    }

    private static J.NewClass newOptions(JavaType.FullyQualified options, Space prefix) {
        JavaType.Method constructor = null;
        for (JavaType.Method method : options.getMethods()) {
            if (method.isConstructor() && method.getParameterTypes().isEmpty()) {
                constructor = method;
                break;
            }
        }
        if (constructor == null) {
            constructor = new JavaType.Method(null, Flag.Public.getBitMask(), options, "<constructor>", options,
                    emptyList(), emptyList(), emptyList(), emptyList(), null, null);
        }
        return new J.NewClass(randomId(), prefix, Markers.EMPTY, null, Space.EMPTY,
                new J.Identifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), options.getClassName(), options, null),
                JContainer.build(Space.EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                null, constructor);
    }

    private static J.MethodDeclaration defaultConstructor(J.ClassDeclaration cd, J.MethodInvocation superCall) {
        JavaType.FullyQualified type = cd.getType();
        List<J.Modifier> modifiers = new ArrayList<>();
        long flags = 0;
        for (J.Modifier modifier : cd.getModifiers()) {
            J.Modifier.Type visibility = modifier.getType();
            if (visibility == J.Modifier.Type.Public || visibility == J.Modifier.Type.Protected || visibility == J.Modifier.Type.Private) {
                modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, null, visibility, emptyList()));
                flags = visibility == J.Modifier.Type.Public ? Flag.Public.getBitMask() :
                        visibility == J.Modifier.Type.Protected ? Flag.Protected.getBitMask() : Flag.Private.getBitMask();
            }
        }
        JavaType.Method methodType = new JavaType.Method(null, flags, type, "<constructor>", type,
                emptyList(), emptyList(), emptyList(), emptyList(), null, null);
        J.Block body = new J.Block(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, JRightPadded.build(false),
                singletonList(JRightPadded.build(superCall.withPrefix(Space.format("\n")))), Space.format("\n"));
        return new J.MethodDeclaration(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), modifiers, null, null,
                new J.MethodDeclaration.IdentifierWithAnnotations(
                        new J.Identifier(randomId(), modifiers.isEmpty() ? Space.EMPTY : Space.SINGLE_SPACE, Markers.EMPTY,
                                emptyList(), cd.getSimpleName(), methodType, null),
                        emptyList()),
                JContainer.build(Space.EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                null, body, null, methodType);
    }
}
