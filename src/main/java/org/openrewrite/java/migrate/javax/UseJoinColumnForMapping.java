/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationAttribute;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
public class UseJoinColumnForMapping extends Recipe {
    private final String JOIN_COLUMN = "javax.persistence.JoinColumn";
    private final String COLUMN = "javax.persistence.Column";

    @Override
    public String getDisplayName() {
        return "`@JoinColumn` annotations must be used with relationship mappings";
    }

    @Override
    public String getDescription() {
        return "In OpenJPA, when a relationship attribute has either a `@OneToOne` or a `@ManyToOne` annotation with a " +
               "`@Column` annotation, the `@Column` annotation is treated as a `@JoinColumn` annotation. EclipseLink " +
               "throws an exception that indicates that the entity class must use `@JoinColumn` instead of `@Column` " +
               "to map a relationship attribute.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("javax.persistence.Entity", true),
                        new UsesType<>(COLUMN, true),
                        Preconditions.or(
                                new UsesType<>("javax.persistence.OneToOne", true),
                                new UsesType<>("javax.persistence.ManyToOne", true)
                        )
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if not annotated with @Column and a relationship mapping annotation
                        if (FindAnnotations.find(multiVariable, COLUMN).isEmpty() ||
                            (FindAnnotations.find(multiVariable, "javax.persistence.OneToOne").isEmpty() &&
                                FindAnnotations.find(multiVariable, "javax.persistence.ManyToOne").isEmpty())) {
                            return multiVariable;
                        }

                        // Change @Column to @JoinColumn
                        // The javax.persistence.Column attributes length, precision, and scale are not kept.
                        maybeRemoveImport(COLUMN);
                        maybeAddImport(JOIN_COLUMN);
                        J.VariableDeclarations joinColumn = (J.VariableDeclarations) new ChangeType(COLUMN, JOIN_COLUMN, false).getVisitor().visit(multiVariable, ctx, getCursor().getParentOrThrow());
                        joinColumn = (J.VariableDeclarations) new RemoveAnnotationAttribute(JOIN_COLUMN, "length").getVisitor().visit(joinColumn, ctx, getCursor().getParentOrThrow());
                        joinColumn = (J.VariableDeclarations) new RemoveAnnotationAttribute(JOIN_COLUMN, "precision").getVisitor().visit(joinColumn, ctx, getCursor().getParentOrThrow());
                        joinColumn = (J.VariableDeclarations) new RemoveAnnotationAttribute(JOIN_COLUMN, "scale").getVisitor().visit(joinColumn, ctx, getCursor().getParentOrThrow());

                        return joinColumn;
                    }
                }
        );
    }
}
