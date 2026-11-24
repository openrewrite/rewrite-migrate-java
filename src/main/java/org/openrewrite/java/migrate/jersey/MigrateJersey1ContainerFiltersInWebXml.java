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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.migrate.jersey.Jersey1ContainerFilters.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateJersey1ContainerFiltersInWebXml extends ScanningRecipe<Accumulator> {

    private static final String JERSEY1_SERVLET_CONTAINER = "com.sun.jersey.spi.container.servlet.ServletContainer";
    private static final String JERSEY2_SERVLET_CONTAINER = "org.glassfish.jersey.servlet.ServletContainer";
    private static final String PROVIDER_CLASSNAMES = "jersey.config.server.provider.classnames";
    private static final String JERSEY1_CLASSNAMES = "com.sun.jersey.config.property.classnames";

    private static final String GZIP_FILTER = "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter";
    private static final String CSRF_FILTER = "com.sun.jersey.api.container.filter.CsrfProtectionFilter";
    private static final String POST_REPLACE_FILTER = "com.sun.jersey.api.container.filter.PostReplaceFilter";
    private static final String POST_REPLACE_FILTER_CONFIG = "com.sun.jersey.api.container.filter.PostReplaceFilterConfig";

    private static final String ENCODING_FILTER = "org.glassfish.jersey.server.filter.EncodingFilter";
    private static final String GZIP_ENCODER = "org.glassfish.jersey.message.GZipEncoder";
    private static final String JERSEY2_CSRF_FILTER = "org.glassfish.jersey.server.filter.CsrfProtectionFilter";
    private static final String METHOD_OVERRIDE_FILTER = "org.glassfish.jersey.server.filter.HttpMethodOverrideFilter";

    String displayName = "Register Jersey 1 container filters in `web.xml` as Jersey 2 providers";

    String description = "Moves the classes that the `com.sun.jersey.spi.container.ContainerRequestFilters` and " +
            "`com.sun.jersey.spi.container.ContainerResponseFilters` initialization parameters of a Jersey " +
            "`ServletContainer` list in `web.xml` and `web-fragment.xml` to `jersey.config.server.provider.classnames`, " +
            "merged with the class names already there, which is how Jersey 2 registers them as providers. A class " +
            "moves when it becomes a JAX-RS 2 filter of that kind with `Jersey1ContainerFilterToJaxRsFilter`. Of Jersey " +
            "1's own filters, `GZIPContentEncodingFilter` becomes Jersey 2's `GZipEncoder`, and as a response filter " +
            "`EncodingFilter` too, `CsrfProtectionFilter` becomes Jersey 2's `CsrfProtectionFilter`, and " +
            "`PostReplaceFilter` becomes `HttpMethodOverrideFilter` unless it is configured. Other class names, such " +
            "as Jersey 1's `LoggingFilter`, whose Jersey 2 `LoggingFeature` logs at a finer level, and filters from " +
            "libraries, stay where they are, and a parameter is dropped once it lists none.";

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return Jersey1ContainerFilters.scanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(Preconditions.or(
                        new FindSourceFiles("**/web.xml").getVisitor(), new FindSourceFiles("**/web-fragment.xml").getVisitor()),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);
                        String classTag = "servlet".equals(t.getName()) ? "servlet-class" :
                                "filter".equals(t.getName()) ? "filter-class" : null;
                        if (classTag == null) {
                            return t;
                        }
                        String servletClass = t.getChildValue(classTag).map(String::trim).orElse(null);
                        if (!JERSEY1_SERVLET_CONTAINER.equals(servletClass) && !JERSEY2_SERVLET_CONTAINER.equals(servletClass)) {
                            return t;
                        }

                        Map<Phase, Xml.Tag> filterParams = new EnumMap<>(Phase.class);
                        Xml.Tag classNamesParam = null;
                        boolean postReplaceConfigured = false;
                        for (Xml.Tag param : t.getChildren("init-param")) {
                            String name = paramName(param);
                            Phase phase = Phase.ofProperty(name);
                            if (phase != null) {
                                filterParams.putIfAbsent(phase, param);
                            } else if (PROVIDER_CLASSNAMES.equals(name) ||
                                    (JERSEY1_CLASSNAMES.equals(name) && classNamesParam == null)) {
                                classNamesParam = param;
                            } else if (POST_REPLACE_FILTER_CONFIG.equals(name)) {
                                postReplaceConfigured = true;
                            }
                        }

                        Set<String> moved = new LinkedHashSet<>();
                        Map<Phase, List<String>> kept = new EnumMap<>(Phase.class);
                        for (Map.Entry<Phase, Xml.Tag> entry : filterParams.entrySet()) {
                            List<String> keep = new ArrayList<>();
                            for (String className : classNames(paramValue(entry.getValue()))) {
                                List<String> providers = providers(className, entry.getKey(), postReplaceConfigured);
                                if (providers == null) {
                                    keep.add(className);
                                } else {
                                    moved.addAll(providers);
                                }
                            }
                            kept.put(entry.getKey(), keep);
                        }
                        if (moved.isEmpty()) {
                            return t;
                        }

                        boolean registered = classNamesParam != null;
                        List<Content> content = new ArrayList<>();
                        for (Content c : t.getContent() == null ? Collections.<Content>emptyList() : t.getContent()) {
                            Phase phase = c instanceof Xml.Tag ? Phase.ofProperty(paramName((Xml.Tag) c)) : null;
                            if (phase != null && filterParams.get(phase) == c) {
                                Xml.Tag param = (Xml.Tag) c;
                                List<String> keep = kept.get(phase);
                                if (!keep.isEmpty()) {
                                    content.add(keep.equals(classNames(paramValue(param))) ? param :
                                            param.withChildValue("param-value", String.join(",", keep)));
                                } else if (!registered) {
                                    // the parameter becomes the one that registers the providers
                                    content.add(param.withChildValue("param-name", PROVIDER_CLASSNAMES)
                                            .withChildValue("param-value", String.join(",", moved)));
                                    registered = true;
                                }
                            } else if (c == classNamesParam) {
                                content.add(withClassNames(classNamesParam, moved));
                            } else {
                                content.add(c);
                            }
                        }
                        t = t.withContent(content);
                        if (!registered) {
                            t = addInitParam(t, classTag, String.join(",", moved), ctx);
                        }
                        return t;
                    }

                    /**
                     * @return the class names that register in Jersey 2 what the class did as a filter of the
                     * phase in Jersey 1, or {@code null} to leave the class where it is.
                     */
                    private @Nullable List<String> providers(String className, Phase phase, boolean postReplaceConfigured) {
                        Set<Phase> phases = acc.converting(className);
                        if (phases != null) {
                            // Jersey 1 ignored a filter listed as the kind it is not
                            return phases.contains(phase) ? singletonList(className) : null;
                        }
                        if (GZIP_FILTER.equals(className)) {
                            // GZipEncoder decodes requests and encodes responses that EncodingFilter marks as gzip
                            return phase == Phase.REQUEST ? singletonList(GZIP_ENCODER) : asList(ENCODING_FILTER, GZIP_ENCODER);
                        }
                        if (CSRF_FILTER.equals(className) && phase == Phase.REQUEST) {
                            return singletonList(JERSEY2_CSRF_FILTER);
                        }
                        if (POST_REPLACE_FILTER.equals(className) && phase == Phase.REQUEST && !postReplaceConfigured) {
                            return singletonList(METHOD_OVERRIDE_FILTER);
                        }
                        return null;
                    }

                    private Xml.Tag withClassNames(Xml.Tag param, Set<String> added) {
                        String classNames = paramValue(param);
                        List<String> present = Jersey1ContainerFilters.classNames(classNames);
                        StringBuilder merged = new StringBuilder(classNames);
                        String separator = classNames.contains(";") && !classNames.contains(",") ? ";" : ",";
                        for (String className : added) {
                            if (!present.contains(className)) {
                                if (merged.length() > 0) {
                                    merged.append(separator);
                                }
                                merged.append(className);
                            }
                        }
                        return merged.toString().equals(classNames) ? param : param.withChildValue("param-value", merged.toString());
                    }

                    /**
                     * Adds the provider class names after the last initialization parameter, or else where the
                     * schema puts them.
                     */
                    private Xml.Tag addInitParam(Xml.Tag t, String classTag, String value, ExecutionContext ctx) {
                        List<Content> content = new ArrayList<>(t.getContent() == null ? Collections.<Content>emptyList() : t.getContent());
                        int insertAt = -1;
                        for (int i = 0; i < content.size(); i++) {
                            Content c = content.get(i);
                            if (c instanceof Xml.Tag) {
                                String tagName = ((Xml.Tag) c).getName();
                                if ("init-param".equals(tagName) || classTag.equals(tagName) ||
                                        ("filter-class".equals(classTag) && "async-supported".equals(tagName))) {
                                    insertAt = i + 1;
                                }
                            }
                        }
                        Xml.Tag param = Xml.Tag.build("<init-param>\n<param-name>" + PROVIDER_CLASSNAMES +
                                "</param-name>\n<param-value>" + value + "</param-value>\n</init-param>");
                        content.add(insertAt < 0 ? content.size() : insertAt, param);
                        Xml.Tag withParam = t.withContent(content);
                        return (Xml.Tag) new XmlIsoVisitor<ExecutionContext>() {
                            @Override
                            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                                if (tag == param) {
                                    return (Xml.Tag) autoFormat(tag, ctx, getCursor().getParentOrThrow());
                                }
                                return super.visitTag(tag, ctx);
                            }
                        }.visitNonNull(withParam, ctx, getCursor().getParentOrThrow());
                    }
                });
    }
}
