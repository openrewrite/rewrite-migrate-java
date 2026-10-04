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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.dependencies.ChangeDependency;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeJaxbApiDependency extends ScanningRecipe<ChangeJaxbApiDependency.Accumulator> {
    private static final ChangeDependency CHANGE = new ChangeDependency(
            "javax.xml.bind", "jaxb-api", "jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.x", null, null, null);

    String displayName = "Migrate the JAXB API dependency unless Recorder still needs it";
    String description = "Migrate JAXB API coordinates while preserving the legacy API needed alongside Jakarta JAXB " +
                         "by external Arquillian Recorder 1.x binaries. Recorder modules migrated in the same reactor are excluded.";

    static class Accumulator {
        final Set<String> projects = new HashSet<>();
        final ChangeDependency.Accumulator dependency;

        Accumulator(ChangeDependency.Accumulator dependency) {
            this.dependency = dependency;
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(CHANGE.getInitialValue(ctx));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> delegate = CHANGE.getScanner(acc.dependency);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                tree.getMarkers().findFirst(MavenResolutionResult.class)
                        .ifPresent(model -> acc.projects.add(RetainJaxbApiForArquillianRecorder.projectKey(model)));
                if (tree instanceof SourceFile && delegate.isAcceptable((SourceFile) tree, ctx)) {
                    delegate.visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> delegate = CHANGE.getVisitor(acc.dependency);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                MavenResolutionResult model = tree.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (model != null && RetainJaxbApiForArquillianRecorder.hasJakartaApi(model) &&
                    model.getDependencies().values().stream().flatMap(Collection::stream)
                            .anyMatch(d -> RetainJaxbApiForArquillianRecorder.externalRecorder(d, acc.projects))) {
                    return tree;
                }
                if (tree instanceof SourceFile && delegate.isAcceptable((SourceFile) tree, ctx)) {
                    return delegate.visitNonNull(tree, ctx);
                }
                return tree;
            }
        };
    }
}
