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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseLombokUtilityClass extends ScanningRecipe<UseLombokUtilityClass.UtilityClassAccumulator> {

    private static final AnnotationMatcher UTILITY_CLASS_MATCHER = new AnnotationMatcher("@lombok.experimental.UtilityClass");
    private static final Pattern FLAG_USAGE_PATTERN = Pattern.compile("^\\s*lombok\\.utilityClass\\.flagUsage\\s*=\\s*([^\\s#]+).*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLEAR_FLAG_USAGE_PATTERN = Pattern.compile("^\\s*clear\\s+lombok\\.utilityClass\\.flagUsage\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STOP_BUBBLING_PATTERN = Pattern.compile("^\\s*config\\.stopBubbling\\s*=\\s*(true|false).*$", Pattern.CASE_INSENSITIVE);

    String displayName = "Use Lombok `@UtilityClass` where applicable";

    String description = "Replace static-only utility classes with Lombok's `@UtilityClass` annotation.";

    Set<String> tags = singleton("lombok");

    @Override
    public UtilityClassAccumulator getInitialValue(ExecutionContext ctx) {
        return new UtilityClassAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(UtilityClassAccumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;
                recordLombokConfig(acc, sourceFile);
                if (sourceFile instanceof J.CompilationUnit) {
                    new ReferenceScanner(acc.unsafeUtilityClassTypes).visit((J.CompilationUnit) sourceFile, ctx);
                }
                return sourceFile;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(UtilityClassAccumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final Map<UUID, UtilityClassCandidate> candidates = new HashMap<>();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                UtilityClassCandidate candidate = candidate(classDecl, getCursor(), acc);
                if (candidate == null) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }

                candidates.put(classDecl.getId(), candidate);
                boolean utilityClassNameConflict = hasUtilityClassNameConflict();
                if (!utilityClassNameConflict) {
                    maybeAddImport("lombok.experimental.UtilityClass");
                }
                J.ClassDeclaration cd = JavaTemplate.builder(utilityClassNameConflict ?
                                "@lombok.experimental.UtilityClass" :
                                "@UtilityClass")
                        .imports("lombok.experimental.UtilityClass")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                        .build()
                        .apply(getCursor(), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                return super.visitClassDeclaration(cd, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                UtilityClassCandidate candidate = enclosingCandidate();
                if (candidate == null || !candidate.methodIds.contains(md.getId())) {
                    return md;
                }
                return maybeAutoFormat(method, md.withModifiers(withoutStatic(md.getModifiers())), ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations,
                                                                     ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(variableDeclarations, ctx);
                UtilityClassCandidate candidate = enclosingCandidate();
                if (candidate == null || !candidate.fieldIds.contains(vd.getId())) {
                    return vd;
                }
                return maybeAutoFormat(variableDeclarations, vd.withModifiers(withoutStatic(vd.getModifiers())), ctx);
            }

            private @Nullable UtilityClassCandidate enclosingCandidate() {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return enclosing == null ? null : candidates.get(enclosing.getId());
            }

            private boolean hasUtilityClassNameConflict() {
                J.CompilationUnit compilationUnit = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (compilationUnit != null && compilationUnit.getClasses().stream()
                        .anyMatch(type -> "UtilityClass".equals(type.getSimpleName()))) {
                    return true;
                }

                Cursor parent = getCursor().getParent();
                while (parent != null) {
                    if (parent.getValue() instanceof J.ClassDeclaration) {
                        J.ClassDeclaration enclosing = (J.ClassDeclaration) parent.getValue();
                        if ("UtilityClass".equals(enclosing.getSimpleName()) ||
                                enclosing.getBody().getStatements().stream()
                                        .filter(J.ClassDeclaration.class::isInstance)
                                        .map(J.ClassDeclaration.class::cast)
                                        .anyMatch(type -> "UtilityClass".equals(type.getSimpleName()))) {
                            return true;
                        }
                    }
                    parent = parent.getParent();
                }
                return false;
            }
        };
    }

    private static @Nullable UtilityClassCandidate candidate(J.ClassDeclaration classDecl,
                                                              Cursor cursor,
                                                              UtilityClassAccumulator acc) {
        JavaType.FullyQualified type = classDecl.getType();
        if (type == null ||
                classDecl.getKind() != J.ClassDeclaration.Kind.Type.Class ||
                !isTopLevelOrStaticallyLegalMemberClass(cursor) ||
                classDecl.hasModifier(J.Modifier.Type.Abstract) ||
                hasTypeParameters(classDecl) ||
                classDecl.getExtends() != null ||
                (classDecl.getImplements() != null && !classDecl.getImplements().isEmpty()) ||
                classDecl.getLeadingAnnotations().stream().anyMatch(UTILITY_CLASS_MATCHER::matches) ||
                acc.unsafeUtilityClassTypes.contains(normalizeTypeName(type.getFullyQualifiedName())) ||
                isUtilityClassFlaggedAsError(cursor, acc.lombokConfigs)) {
            return null;
        }

        Set<UUID> methodIds = new HashSet<>();
        Set<UUID> fieldIds = new HashSet<>();
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) statement;
                if (method.isConstructor() ||
                        !method.hasModifier(J.Modifier.Type.Static) ||
                        "main".equalsIgnoreCase(method.getSimpleName())) {
                    return null;
                }
                methodIds.add(method.getId());
            } else if (statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations field = (J.VariableDeclarations) statement;
                if (!field.hasModifier(J.Modifier.Type.Static)) {
                    return null;
                }
                fieldIds.add(field.getId());
            } else {
                return null;
            }
        }

        if (methodIds.isEmpty() && fieldIds.isEmpty()) {
            return null;
        }
        return new UtilityClassCandidate(methodIds, fieldIds);
    }

    private static boolean isTopLevelOrStaticallyLegalMemberClass(Cursor cursor) {
        if (!isTopLevelOrMemberClass(cursor)) {
            return false;
        }

        Cursor parent = cursor.getParent();
        while (parent != null) {
            if (parent.getValue() instanceof J.ClassDeclaration &&
                    !isTopLevelClass(parent) &&
                    !isStaticOrImplicit(parent)) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }

    private static boolean isTopLevelOrMemberClass(Cursor cursor) {
        Cursor parent = cursor.getParent();
        while (parent != null) {
            Object value = parent.getValue();
            if (value instanceof J.CompilationUnit) {
                return true;
            }
            if (value instanceof J.Block) {
                Cursor blockParent = parent.getParent();
                return blockParent != null && blockParent.getValue() instanceof J.ClassDeclaration;
            }
            if (value instanceof J.MethodDeclaration || value instanceof J.NewClass) {
                return false;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static boolean isTopLevelClass(Cursor classCursor) {
        Cursor parent = classCursor.getParent();
        while (parent != null) {
            Object value = parent.getValue();
            if (value instanceof J.CompilationUnit) {
                return true;
            }
            if (value instanceof J.ClassDeclaration || value instanceof J.MethodDeclaration || value instanceof J.NewClass) {
                return false;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static boolean isStaticOrImplicit(Cursor classCursor) {
        J.ClassDeclaration classDecl = (J.ClassDeclaration) classCursor.getValue();
        J.ClassDeclaration.Kind.Type kind = classDecl.getKind();
        if (classDecl.hasModifier(J.Modifier.Type.Static) ||
                kind == J.ClassDeclaration.Kind.Type.Interface ||
                kind == J.ClassDeclaration.Kind.Type.Annotation ||
                kind == J.ClassDeclaration.Kind.Type.Enum ||
                kind == J.ClassDeclaration.Kind.Type.Record) {
            return true;
        }

        Cursor parent = classCursor.getParent();
        while (parent != null) {
            if (parent.getValue() instanceof J.ClassDeclaration) {
                J.ClassDeclaration enclosing = (J.ClassDeclaration) parent.getValue();
                J.ClassDeclaration.Kind.Type enclosingKind = enclosing.getKind();
                return enclosingKind == J.ClassDeclaration.Kind.Type.Interface ||
                        enclosingKind == J.ClassDeclaration.Kind.Type.Annotation ||
                        enclosingKind == J.ClassDeclaration.Kind.Type.Enum ||
                        enclosingKind == J.ClassDeclaration.Kind.Type.Record;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static boolean hasTypeParameters(J.ClassDeclaration classDecl) {
        List<J.TypeParameter> typeParameters = classDecl.getTypeParameters();
        return typeParameters != null && !typeParameters.isEmpty();
    }

    private static List<J.Modifier> withoutStatic(List<J.Modifier> modifiers) {
        List<J.Modifier> result = new ArrayList<>(modifiers);
        result.removeIf(modifier -> modifier.getType() == J.Modifier.Type.Static);
        return result;
    }

    private static void recordType(Set<String> typeNames, @Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        if (fullyQualified != null) {
            typeNames.add(normalizeTypeName(fullyQualified.getFullyQualifiedName()));
        }
    }

    private static void recordDeclaringType(Set<String> typeNames, JavaType.@Nullable Method methodType) {
        if (methodType != null && methodType.getDeclaringType() != null) {
            typeNames.add(normalizeTypeName(methodType.getDeclaringType().getFullyQualifiedName()));
        }
    }

    private static String normalizeTypeName(String typeName) {
        return typeName.replace('$', '.');
    }

    private static void recordLombokConfig(UtilityClassAccumulator acc, SourceFile sourceFile) {
        Path sourcePath = sourceFile.getSourcePath().normalize();
        if (sourcePath.getFileName() == null ||
                (!"lombok.config".equals(sourcePath.getFileName().toString()) &&
                        !sourcePath.getFileName().toString().endsWith(".config"))) {
            return;
        }

        @Nullable String flagUsage = null;
        boolean hasFlagUsage = false;
        boolean stopBubbling = false;
        boolean importsAllowed = true;
        List<String> imports = new ArrayList<>();
        boolean hasUnresolvedImport = false;
        for (String line : sourceFile.printAll().split("\\R")) {
            String uncommented = line.replaceFirst("\\s+#.*$", "").trim();
            if (uncommented.isEmpty()) {
                continue;
            }
            Matcher importMatcher = IMPORT_PATTERN.matcher(uncommented);
            if (importsAllowed && importMatcher.matches()) {
                Path importedConfig = resolveImport(sourcePath, importMatcher.group(1));
                if (importedConfig == null) {
                    hasUnresolvedImport = true;
                } else {
                    imports.add(pathKey(importedConfig));
                }
                continue;
            }

            importsAllowed = false;
            Matcher clearFlagUsageMatcher = CLEAR_FLAG_USAGE_PATTERN.matcher(uncommented);
            if (clearFlagUsageMatcher.matches()) {
                hasFlagUsage = true;
                flagUsage = null;
                continue;
            }
            Matcher flagUsageMatcher = FLAG_USAGE_PATTERN.matcher(uncommented);
            if (flagUsageMatcher.matches()) {
                hasFlagUsage = true;
                flagUsage = flagUsageMatcher.group(1);
                continue;
            }
            Matcher stopBubblingMatcher = STOP_BUBBLING_PATTERN.matcher(uncommented);
            if (stopBubblingMatcher.matches()) {
                stopBubbling = Boolean.parseBoolean(stopBubblingMatcher.group(1));
            }
        }
        acc.lombokConfigs.put(pathKey(sourcePath), new LombokConfig(flagUsage, hasFlagUsage, stopBubbling, imports, hasUnresolvedImport));
    }

    private static boolean isUtilityClassFlaggedAsError(Cursor cursor, Map<String, LombokConfig> lombokConfigs) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return false;
        }

        Path directory = sourceFile.getSourcePath().getParent();
        Set<String> visitedConfigs = new HashSet<>();
        while (true) {
            LombokConfigResolution resolution = resolveConfig(configurationPathKey(directory), lombokConfigs, visitedConfigs, false);
            if (resolution.hasUnresolvedImport) {
                return true;
            }
            if (resolution.hasFlagUsage) {
                return "error".equalsIgnoreCase(resolution.flagUsage);
            }
            if (resolution.stopBubbling || directory == null) {
                return false;
            }
            directory = directory.getParent();
        }
    }

    private static @Nullable Path resolveImport(Path sourcePath, String importPath) {
        if (importPath.contains("!")) {
            return null;
        }
        try {
            Path imported = Paths.get(importPath);
            if (!imported.isAbsolute() && sourcePath.getParent() != null) {
                imported = sourcePath.getParent().resolve(imported);
            }
            return imported.normalize();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static LombokConfigResolution resolveConfig(String configPath,
                                                         Map<String, LombokConfig> lombokConfigs,
                                                         Set<String> visitedConfigs,
                                                         boolean imported) {
        if (!visitedConfigs.add(configPath)) {
            return LombokConfigResolution.NONE;
        }

        LombokConfig config = lombokConfigs.get(configPath);
        if (config == null) {
            return imported ? LombokConfigResolution.UNRESOLVED_IMPORT : LombokConfigResolution.NONE;
        }
        if (config.hasFlagUsage) {
            return new LombokConfigResolution(config.flagUsage, true, config.stopBubbling, config.hasUnresolvedImport);
        }
        if (config.hasUnresolvedImport) {
            return new LombokConfigResolution(null, false, config.stopBubbling, true);
        }

        boolean stopBubbling = config.stopBubbling;
        for (int i = config.imports.size() - 1; i >= 0; i--) {
            LombokConfigResolution importedConfig = resolveConfig(config.imports.get(i), lombokConfigs, visitedConfigs, true);
            stopBubbling |= importedConfig.stopBubbling;
            if (importedConfig.hasUnresolvedImport || importedConfig.hasFlagUsage) {
                return new LombokConfigResolution(importedConfig.flagUsage, importedConfig.hasFlagUsage, stopBubbling, importedConfig.hasUnresolvedImport);
            }
        }
        return new LombokConfigResolution(null, false, stopBubbling, false);
    }

    private static String configurationPathKey(@Nullable Path directory) {
        return pathKey(directory == null ? Paths.get("lombok.config") : directory.resolve("lombok.config"));
    }

    private static String pathKey(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static class ReferenceScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> unsafeUtilityClassTypes;

        private ReferenceScanner(Set<String> unsafeUtilityClassTypes) {
            this.unsafeUtilityClassTypes = unsafeUtilityClassTypes;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass nc = super.visitNewClass(newClass, ctx);
            recordType(unsafeUtilityClassTypes, nc.getType());
            recordDeclaringType(unsafeUtilityClassTypes, nc.getConstructorType());
            return nc;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference mr = super.visitMemberReference(memberRef, ctx);
            JavaType.Method methodType = mr.getMethodType();
            if (methodType != null &&
                    ("<constructor>".equals(methodType.getName()) || "new".equals(mr.getReference().getSimpleName()))) {
                recordDeclaringType(unsafeUtilityClassTypes, methodType);
            }
            return mr;
        }

        @Override
        public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
            J.Import imp = super.visitImport(anImport, ctx);
            if (imp.isStatic() && !"*".equals(imp.getQualid().getSimpleName())) {
                unsafeUtilityClassTypes.add(normalizeTypeName(imp.getTypeName()));
            }
            return imp;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            if (cd.getExtends() != null) {
                recordType(unsafeUtilityClassTypes, cd.getExtends().getType());
            }
            return cd;
        }
    }

    static class UtilityClassAccumulator {
        final Set<String> unsafeUtilityClassTypes = new HashSet<>();
        final Map<String, LombokConfig> lombokConfigs = new HashMap<>();
    }

    private static class LombokConfig {
        private final @Nullable String flagUsage;
        private final boolean hasFlagUsage;
        private final boolean stopBubbling;
        private final List<String> imports;
        private final boolean hasUnresolvedImport;

        private LombokConfig(@Nullable String flagUsage,
                             boolean hasFlagUsage,
                             boolean stopBubbling,
                             List<String> imports,
                             boolean hasUnresolvedImport) {
            this.flagUsage = flagUsage;
            this.hasFlagUsage = hasFlagUsage;
            this.stopBubbling = stopBubbling;
            this.imports = imports;
            this.hasUnresolvedImport = hasUnresolvedImport;
        }
    }

    private static class LombokConfigResolution {
        private static final LombokConfigResolution NONE = new LombokConfigResolution(null, false, false, false);
        private static final LombokConfigResolution UNRESOLVED_IMPORT = new LombokConfigResolution(null, false, false, true);

        private final @Nullable String flagUsage;
        private final boolean hasFlagUsage;
        private final boolean stopBubbling;
        private final boolean hasUnresolvedImport;

        private LombokConfigResolution(@Nullable String flagUsage,
                                       boolean hasFlagUsage,
                                       boolean stopBubbling,
                                       boolean hasUnresolvedImport) {
            this.flagUsage = flagUsage;
            this.hasFlagUsage = hasFlagUsage;
            this.stopBubbling = stopBubbling;
            this.hasUnresolvedImport = hasUnresolvedImport;
        }
    }

    private static final class UtilityClassCandidate {
        private final Set<UUID> methodIds;
        private final Set<UUID> fieldIds;

        private UtilityClassCandidate(Set<UUID> methodIds, Set<UUID> fieldIds) {
            this.methodIds = methodIds;
            this.fieldIds = fieldIds;
        }
    }
}
