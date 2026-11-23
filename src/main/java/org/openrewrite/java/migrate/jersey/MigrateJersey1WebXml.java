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

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateJersey1WebXml extends Recipe {

    private static final String JERSEY1_SERVLET_CONTAINER = "com.sun.jersey.spi.container.servlet.ServletContainer";
    private static final String JERSEY2_SERVLET_CONTAINER = "org.glassfish.jersey.servlet.ServletContainer";

    private static final String RESOURCE_CONFIG_CLASS = "com.sun.jersey.config.property.resourceConfigClass";
    private static final String APPLICATION = "javax.ws.rs.Application";
    private static final String PACKAGES = "com.sun.jersey.config.property.packages";
    private static final String CLASSPATH = "com.sun.jersey.config.property.classpath";
    private static final String TRACE = "com.sun.jersey.config.feature.Trace";
    private static final String TRACE_PER_REQUEST = "com.sun.jersey.config.feature.TracePerRequest";
    private static final String POJO_MAPPING = "com.sun.jersey.api.json.POJOMappingFeature";

    private static final String PROVIDER_CLASSNAMES = "jersey.config.server.provider.classnames";
    private static final String PROVIDER_WEB_APP = "jersey.config.servlet.provider.webapp";
    private static final String TRACING = "jersey.config.server.tracing.type";
    private static final String JACKSON_PROVIDER = "org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider";

    /**
     * Jersey 1 initialization parameters, as {@code ServletContainer}, {@code ResourceConfig} and
     * {@code FeaturesAndProperties} 1.19.4 name them, and the Jersey 2.48 properties that mean the same.
     */
    static final Map<String, String> RENAMED_PARAMETERS = new LinkedHashMap<>();

    /**
     * Features, whose values Jersey 1 read as booleans after trimming them, and Jersey 2 reads without trimming.
     */
    private static final Set<String> FEATURES = new HashSet<>();

    static {
        RENAMED_PARAMETERS.put(PACKAGES, "jersey.config.server.provider.packages");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.classnames", PROVIDER_CLASSNAMES);
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.JSPTemplatesBasePath", "jersey.config.server.mvc.templateBasePath.jsp");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.WebPageContentRegex", "jersey.config.servlet.filter.staticContentRegex");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.FilterForwardOn404", "jersey.config.servlet.filter.forwardOn404");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.FilterContextPath", "jersey.config.servlet.filter.contextPath");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.DisableWADL", "jersey.config.server.wadl.disableWadl");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.WadlGeneratorConfig", "jersey.config.server.wadl.generatorConfig");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.MediaTypeMappings", "jersey.config.server.mediaTypeMappings");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.property.LanguageMappings", "jersey.config.server.languageMappings");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.Formatted", "jersey.config.xml.formatOutput");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.XmlRootElementProcessing", "jersey.config.jaxb.collections.processXmlRootElement");
        RENAMED_PARAMETERS.put("com.sun.jersey.config.feature.DisableXmlSecurity", "jersey.config.xml.security.disable");

        FEATURES.add("com.sun.jersey.config.feature.FilterForwardOn404");
        FEATURES.add("com.sun.jersey.config.feature.DisableWADL");
        FEATURES.add("com.sun.jersey.config.feature.Formatted");
        FEATURES.add("com.sun.jersey.config.feature.XmlRootElementProcessing");
        FEATURES.add("com.sun.jersey.config.feature.DisableXmlSecurity");
    }

    /**
     * Resource configuration classes of Jersey 1 that only read the parameters that are migrated next to them.
     */
    private static final Set<String> PARAMETER_DRIVEN_RESOURCE_CONFIGS = new HashSet<>(Arrays.asList(
            "com.sun.jersey.api.core.PackagesResourceConfig",
            "com.sun.jersey.api.core.ClassNamesResourceConfig",
            "com.sun.jersey.api.core.DefaultResourceConfig"));

    private static final String CLASSPATH_RESOURCE_CONFIG = "com.sun.jersey.api.core.ClasspathResourceConfig";

    String displayName = "Migrate Jersey 1 servlets and filters in `web.xml` to Jersey 2";

    String description = "Replaces Jersey 1's `ServletContainer` with Jersey 2's as `servlet-class` or `filter-class` in " +
            "`web.xml` and `web-fragment.xml`, and renames its initialization parameters to their Jersey 2 " +
            "counterparts: packages, class names, the JSP template base path, the static content regular expression, " +
            "forwarding on 404, the filter context path, WADL, media type and language mappings, XML formatting, " +
            "`@XmlRootElement` processing and XML security. A user's resource configuration class becomes the " +
            "`javax.ws.rs.Application` parameter, and Jersey 1's own resource configuration classes are dropped. " +
            "Tracing becomes `jersey.config.server.tracing.type` set to `ALL` or `ON_DEMAND`. POJO mapping switched " +
            "on adds Jackson 1's `JacksonJaxbJsonProvider` to the provider class names. A servlet that Jersey 1 let " +
            "scan `WEB-INF/lib` and `WEB-INF/classes`, because it named no application, resource configuration or " +
            "packages, gets `jersey.config.servlet.provider.webapp` so that Jersey 2 scans them too. Parameters " +
            "without a Jersey 2 counterpart, such as container request and response filters, resource filter " +
            "factories, the class path, redirection, URI normalization and implicit viewables, are left alone, as " +
            "are Guice's and Spring's Jersey 1 servlets.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                        new FindSourceFiles("**/web.xml").getVisitor(), new FindSourceFiles("**/web-fragment.xml").getVisitor()),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);
                        String classTag = "servlet".equals(t.getName()) ? "servlet-class" :
                                "filter".equals(t.getName()) ? "filter-class" : null;
                        if (classTag == null ||
                                !JERSEY1_SERVLET_CONTAINER.equals(t.getChildValue(classTag).map(String::trim).orElse(null))) {
                            return t;
                        }
                        t = t.withChildValue(classTag, JERSEY2_SERVLET_CONTAINER);
                        return migrateInitParams(t, classTag, ctx);
                    }

                    private Xml.Tag migrateInitParams(Xml.Tag t, String classTag, ExecutionContext ctx) {
                        Map<String, String> params = new HashMap<>();
                        for (Xml.Tag param : t.getChildren("init-param")) {
                            String name = name(param);
                            if (name != null) {
                                params.put(name, value(param));
                            }
                        }
                        // Jersey 1 took the resource configuration class, else the application class, else the packages
                        String resourceConfigClass = params.get(RESOURCE_CONFIG_CLASS);
                        String configClass = resourceConfigClass != null ? resourceConfigClass : params.get(APPLICATION);
                        boolean classpathConfigWithPaths = CLASSPATH_RESOURCE_CONFIG.equals(configClass) && params.containsKey(CLASSPATH);
                        boolean scansWebApp = (configClass == null && !params.containsKey(PACKAGES)) ||
                                (CLASSPATH_RESOURCE_CONFIG.equals(configClass) && !classpathConfigWithPaths);
                        boolean trace = isTrue(params.get(TRACE));
                        boolean pojoMapping = isTrue(params.get(POJO_MAPPING));

                        List<Content> content = new ArrayList<>();
                        for (Content c : t.getContent() == null ? Collections.<Content>emptyList() : t.getContent()) {
                            if (!(c instanceof Xml.Tag) || !"init-param".equals(((Xml.Tag) c).getName())) {
                                content.add(c);
                                continue;
                            }
                            Xml.Tag param = (Xml.Tag) c;
                            String name = name(param);
                            String value = value(param);
                            Xml.Tag migrated = param;
                            if (name == null) {
                                content.add(param);
                                continue;
                            }
                            if (RENAMED_PARAMETERS.containsKey(name)) {
                                migrated = param.withChildValue("param-name", RENAMED_PARAMETERS.get(name));
                                if (FEATURES.contains(name) && isBoolean(value) && param.getChild("param-value").isPresent()) {
                                    migrated = migrated.withChildValue("param-value", value.toLowerCase(Locale.ROOT));
                                }
                            } else if (RESOURCE_CONFIG_CLASS.equals(name) || APPLICATION.equals(name)) {
                                if (classpathConfigWithPaths) {
                                    // Jersey 2 scans no web application paths but WEB-INF/lib and WEB-INF/classes
                                    migrated = param;
                                } else if (APPLICATION.equals(name) && resourceConfigClass != null) {
                                    // Jersey 1 ignored it, as it took the resource configuration class
                                    migrated = null;
                                } else if (PARAMETER_DRIVEN_RESOURCE_CONFIGS.contains(value) || CLASSPATH_RESOURCE_CONFIG.equals(value)) {
                                    migrated = null;
                                } else if (RESOURCE_CONFIG_CLASS.equals(name)) {
                                    migrated = param.withChildValue("param-name", APPLICATION);
                                }
                            } else if (TRACE.equals(name) && isBoolean(value)) {
                                migrated = trace ? param.withChildValue("param-name", TRACING).withChildValue("param-value", "ALL") : null;
                            } else if (TRACE_PER_REQUEST.equals(name) && isBoolean(value)) {
                                migrated = isTrue(value) && !trace ?
                                        param.withChildValue("param-name", TRACING).withChildValue("param-value", "ON_DEMAND") : null;
                            } else if (POJO_MAPPING.equals(name) && isBoolean(value)) {
                                migrated = null;
                            }
                            if (migrated != null) {
                                content.add(migrated);
                            }
                        }
                        t = t.withContent(content);

                        if (pojoMapping) {
                            t = addProviderClassName(t, classTag, ctx);
                        }
                        if (scansWebApp && !t.getChildren("init-param").stream()
                                .anyMatch(p -> PROVIDER_WEB_APP.equals(name(p)))) {
                            t = addInitParam(t, classTag, PROVIDER_WEB_APP, "true", ctx);
                        }
                        return t;
                    }

                    private Xml.Tag addProviderClassName(Xml.Tag t, String classTag, ExecutionContext ctx) {
                        for (Xml.Tag param : t.getChildren("init-param")) {
                            if (!PROVIDER_CLASSNAMES.equals(name(param))) {
                                continue;
                            }
                            String classNames = value(param);
                            if (Arrays.asList(classNames.split("[\\s,;]+")).contains(JACKSON_PROVIDER)) {
                                return t;
                            }
                            String merged = classNames.isEmpty() ? JACKSON_PROVIDER :
                                    classNames + (classNames.contains(";") && !classNames.contains(",") ? ";" : ",") + JACKSON_PROVIDER;
                            Xml.Tag updated = param.withChildValue("param-value", merged);
                            List<Content> content = new ArrayList<>(t.getContent());
                            content.set(content.indexOf(param), updated);
                            return t.withContent(content);
                        }
                        return addInitParam(t, classTag, PROVIDER_CLASSNAMES, JACKSON_PROVIDER, ctx);
                    }

                    /**
                     * Adds an initialization parameter after the last one, or else where the schema puts them.
                     */
                    private Xml.Tag addInitParam(Xml.Tag t, String classTag, String name, String value, ExecutionContext ctx) {
                        List<Content> content = new ArrayList<>(t.getContent());
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
                        Xml.Tag param = Xml.Tag.build("<init-param>\n<param-name>" + name + "</param-name>\n<param-value>" +
                                value + "</param-value>\n</init-param>");
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

    private static @Nullable String name(Xml.Tag param) {
        return param.getChildValue("param-name").map(String::trim).orElse(null);
    }

    private static String value(Xml.Tag param) {
        return param.getChildValue("param-value").map(String::trim).orElse("");
    }

    private static boolean isBoolean(@Nullable String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isTrue(@Nullable String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }
}
