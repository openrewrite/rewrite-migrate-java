package org.openrewrite.java.migrate;

import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;
import java.util.Set;

public class AnnotateTypesVisitor extends JavaIsoVisitor<Set<JavaType.FullyQualified>> {
    private final String annotationToBeAdded;
    private final AnnotationMatcher annotationMatcher;
    private final JavaTemplate template;

    public AnnotateTypesVisitor(String annotationToBeAdded) {
        this.annotationToBeAdded = annotationToBeAdded;
        String[] split = this.annotationToBeAdded.split("\\.");
        String className = split[split.length - 1];
        String packageName = this.annotationToBeAdded.substring(0, this.annotationToBeAdded.lastIndexOf("."));
        this.annotationMatcher = new AnnotationMatcher("@" + this.annotationToBeAdded);
        String interfaceAsString = String.format("package %s; public @interface %s {}", packageName, className);
        this.template = JavaTemplate.builder("@" + className)
                .imports(this.annotationToBeAdded)
                .javaParser(JavaParser.fromJavaVersion().dependsOn(interfaceAsString))
                .build();
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.FullyQualified> injectedTypes) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, injectedTypes);
        if (injectedTypes.contains(TypeUtils.asFullyQualified(cd.getType()))
            && cd.getLeadingAnnotations().stream().noneMatch(annotationMatcher::matches)) {
            maybeAddImport(annotationToBeAdded);
            return template.apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
        }
        return cd;
    }
}
