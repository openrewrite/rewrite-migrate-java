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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class ResolveLombokMavenPluginVersion extends Recipe {
    String displayName = "Separate Lombok Maven plugin and dependency versions";

    String description = "Resolve composite Lombok Maven plugin versions such as `${lombok.version}.0` before " +
            "upgrading the Lombok dependency. The plugin and dependency have separate release versions, so changing " +
            "a shared property must not produce a nonexistent plugin version. Whole property references and " +
            "expressions Maven cannot resolve are left unchanged.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final Map<String, ResolvedPom> scopedPoms = new HashMap<>();

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isPluginTag("org.projectlombok", "lombok-maven-plugin")) {
                    return t;
                }
                Optional<Xml.Tag> versionTag = t.getChild("version");
                String value = versionTag.flatMap(Xml.Tag::getValue).orElse("").trim();
                if (!value.contains("${") || value.matches("\\$\\{[^}]+}")) {
                    return t;
                }
                try {
                    String resolved = declarationPom(ctx).getValue(value);
                    if (resolved != null && !resolved.contains("${") && !resolved.equals(value)) {
                        doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), resolved));
                        maybeUpdateModel();
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(t);
                }
                return t;
            }

            private ResolvedPom declarationPom(ExecutionContext ctx) throws MavenDownloadingException {
                MavenResolutionResult mrr = getResolutionResult();
                Optional<String> profileId = getCursor().getPathAsStream()
                        .filter(it -> it instanceof Xml.Tag && "profile".equals(((Xml.Tag) it).getName()))
                        .map(Xml.Tag.class::cast)
                        .findFirst().flatMap(it -> it.getChildValue("id"));
                if (!profileId.isPresent()) {
                    return mrr.getPom();
                }
                String key = mrr.getId() + ":" + profileId.get();
                ResolvedPom scoped = scopedPoms.get(key);
                if (scoped == null) {
                    List<String> activeProfiles = new ArrayList<>(mrr.getActiveProfiles());
                    activeProfiles.removeIf(it -> {
                        String id = it.trim();
                        return (id.startsWith("!") || id.startsWith("-")) && profileId.get().equals(id.substring(1).trim());
                    });
                    if (!activeProfiles.contains(profileId.get())) {
                        activeProfiles.add(profileId.get());
                    }
                    MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), ctx,
                            MavenExecutionContextView.view(ctx).effectiveSettings(mrr), activeProfiles);
                    scoped = mrr.getPom().getRequested().resolve(activeProfiles, downloader, ctx);
                    scopedPoms.put(key, scoped);
                }
                return scoped;
            }
        };
    }
}
