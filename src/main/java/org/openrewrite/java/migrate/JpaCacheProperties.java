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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;

@EqualsAndHashCode(callSuper = false)
@Value
public class JpaCacheProperties extends Recipe {
    String displayName = "Disable the persistence unit second-level cache";

    String description = "Sets an explicit shared cache mode on OpenJPA persistence units, so they keep OpenJPA's caching " +
            "behaviour once they run on EclipseLink. Units that name another provider, set Hibernate or EclipseLink " +
            "properties, or declare JPA 2.1 or later without a provider are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/persistence.xml"), new PersistenceXmlVisitor());
    }
}

class SharedDataHolder {
    boolean sharedCacheModePropertyUnspecified;
    boolean sharedCacheModeElementUnspecified;

    Xml.@Nullable Tag sharedCacheModeElement;

    Xml.@Nullable Tag propertiesElement;

    Xml.@Nullable Tag sharedCacheModeProperty;

    Xml.@Nullable Tag openJPACacheProperty;

    Xml.@Nullable Tag eclipselinkCacheProperty;

    // Flag in the following conditions:
    //   an openjpa.DataCache property is present
    //   either shared-cache-mode or javax.persistence.sharedCache.mode is set to UNSPECIFIED
    //   both shared-cache-mode and javax.persistence.sharedCache.mode are present
    //   None of the properties/elements are present
    public boolean shouldFlag() {
        return openJPACacheProperty != null ||
                ((sharedCacheModeElement != null && sharedCacheModeElementUnspecified) || (sharedCacheModeProperty != null && sharedCacheModePropertyUnspecified)) ||
                (sharedCacheModeElement != null && sharedCacheModeProperty != null) ||
                (sharedCacheModeElement == null && sharedCacheModeProperty == null && eclipselinkCacheProperty == null);
    }
}

class PersistenceXmlVisitor extends XmlVisitor<ExecutionContext> {

    private static final XPathMatcher PERSISTENCE_MATCHER = new XPathMatcher("/persistence");
    private static final String SHARED_CACHE_MODE_VALUE_UNSPECIFIED = "UNSPECIFIED";
    private static final Set<String> OPENJPA_PROVIDERS = new HashSet<>(Arrays.asList(
            "org.apache.openjpa.persistence.PersistenceProviderImpl",
            "com.ibm.websphere.persistence.PersistenceProviderImpl"));

    @Override
    public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
        if (!"persistence-unit".equals(t.getName()) || usesAnotherProvider(t)) {
            return t;
        }

        SharedDataHolder sdh = extractData(t);
        if (!sdh.shouldFlag()) {
            return t;
        }

        String version = null;
        if (PERSISTENCE_MATCHER.matches(getCursor())) {
            for (Xml.Attribute attribute : t.getAttributes()) {
                if ("version".equals(attribute.getKeyAsString())) {
                    version = attribute.getValue().getValue();
                }
            }
        }

        boolean v1 = "1.0".equals(version);
        // Do we need to edit a shared cache mode property
        if (sdh.sharedCacheModeElement != null || sdh.sharedCacheModeProperty != null) {
            // if UNSPECIFIED, defaults to NONE but if present, use
            // OpenJpa property to decide value
            if (sdh.sharedCacheModeElement != null && sdh.sharedCacheModeElementUnspecified) {
                String scmValue = "NONE";
                if (sdh.openJPACacheProperty != null) {
                    String propVal = getAttributeValue("value", sdh.openJPACacheProperty);
                    scmValue = interpretOpenJPAPropertyValue(propVal);
                }

                String sharedCacheModeElementOriginal = getTextContent(sdh.sharedCacheModeElement);
                String newValue = sharedCacheModeElementOriginal.replaceFirst("UNSPECIFIED", scmValue);
                sdh.sharedCacheModeElement = sdh.sharedCacheModeElement.withValue(newValue);
                t = addOrUpdateChild(t, sdh.sharedCacheModeElement, getCursor().getParentOrThrow());
            } else {
                // There is no shared-cache-mode, so process javax if present.
                // javax property is deleted below if shared-cache-mode is set.
                if (sdh.sharedCacheModeProperty != null &&
                        sdh.sharedCacheModePropertyUnspecified) {

                    String scmValue = "NONE";
                    if (sdh.openJPACacheProperty != null) {
                        String propVal = getAttributeValue("value", sdh.openJPACacheProperty);
                        scmValue = interpretOpenJPAPropertyValue(propVal);
                    }

                    Xml.Tag updatedProp = updateAttributeValue("value", scmValue, sdh.sharedCacheModeProperty);
                    sdh.propertiesElement = sdh.propertiesElement.withContent(ListUtils.map((List<Content>) sdh.propertiesElement.getContent(), content ->
                            content == sdh.sharedCacheModeProperty ? updatedProp : content));
                    sdh.sharedCacheModeProperty = updatedProp;
                    t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
                }
            }
        } else {
            // or create a new one
            // Figure out what the element value should contain.
            String scmValue;
            if (sdh.openJPACacheProperty == null) {
                scmValue = "NONE";
            } else {
                String propVal = getAttributeValue("value", sdh.openJPACacheProperty);
                scmValue = interpretOpenJPAPropertyValue(propVal);
            }

            // if we could determine an appropriate value, create the element.
            if (scmValue != null) {
                if (!v1) {
                    // Ideally we would insert <shared-cache-mode> before the <validation-mode> and <properties> nodes
                    boolean emptyUnit = t.getContent() == null || t.getContent().isEmpty();
                    Content sharedCacheTag = Xml.Tag.build("<shared-cache-mode>" + scmValue + "</shared-cache-mode>");
                    if (!emptyUnit) {
                        sharedCacheTag = autoFormat(sharedCacheTag, ctx, new Cursor(getCursor().getParentOrThrow(), t));
                    }
                    t = t.withContent(ListUtils.insertInOrder((List<Content>) t.getContent(), sharedCacheTag, Comparator.comparing(c1 ->
                            c1 instanceof Xml.Tag && ("validation-mode".equals(((Xml.Tag) c1).getName()) || "properties".equals(((Xml.Tag) c1).getName())) ? 1 : 0)));
                    if (emptyUnit) {
                        t = autoFormat(t, ctx, getCursor().getParentTreeCursor());
                    }
                } else {
                    // version="1.0"
                    // add a property for eclipselink
                    // <property name="eclipselink.cache.shared.default" value="false"/>
                    // The value depends on SCM value
                    // NONE > false, All > true.  Don't change anything else.

                    String eclipseLinkPropValue = convertScmValue(scmValue);
                    if (eclipseLinkPropValue != null) {

                        // If not found the properties element, we need to create it
                        if (sdh.propertiesElement == null) {
                            sdh.propertiesElement = Xml.Tag.build("<properties></properties>");
                        }

                        // add a property element to the end of the properties list.
                        Xml.Tag newElement = Xml.Tag.build("<property name=\"eclipselink.cache.shared.default\" value=\"" + eclipseLinkPropValue + "\"></property>");

                        sdh.propertiesElement = addOrUpdateChild(sdh.propertiesElement, newElement, getCursor().getParentOrThrow());

                        t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
                    }
                }
            }
        }

        // delete any openjpa.DataCache property that has a value of a simple "true" or
        // "false".  Leave more complex values for the user to consider.
        if (sdh.openJPACacheProperty != null) {
            String attrValue = getAttributeValue("value", sdh.openJPACacheProperty);
            if ("true".equalsIgnoreCase(attrValue) || "false".equalsIgnoreCase(attrValue)) {
                sdh.propertiesElement = removeContent(sdh.propertiesElement, sdh.openJPACacheProperty, ctx);
                t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
            }
        }

        // if both shared-cache-mode and javax cache property are set, delete the
        // javax cache property
        if (sdh.sharedCacheModeElement != null && sdh.sharedCacheModeProperty != null) {
            sdh.propertiesElement = removeContent(sdh.propertiesElement, sdh.sharedCacheModeProperty, ctx);
            t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
        }
        return t;
    }

    /**
     * Unlike {@code filterTagChildren}, this hands the removed element's prefix to a comment that trailed it,
     * so the comment keeps its own line rather than collapsing onto the preceding sibling.
     */
    private static Xml.Tag removeContent(Xml.Tag parent, Xml.Tag child, ExecutionContext ctx) {
        return (Xml.Tag) new RemoveContentVisitor<ExecutionContext>(child, false, false).visitNonNull(parent, ctx);
    }

    /**
     * The explicit cache mode keeps OpenJPA's default (no shared cache) after the move to EclipseLink. A unit that names
     * another provider, configures Hibernate or EclipseLink, or declares JPA 2.1 or later without a provider (OpenJPA was
     * the default provider only up to JPA 2.0) keeps its provider's own default.
     */
    private boolean usesAnotherProvider(Xml.Tag puNode) {
        Optional<String> provider = puNode.getChildValue("provider").map(String::trim);
        if (provider.isPresent()) {
            return !OPENJPA_PROVIDERS.contains(provider.get());
        }
        List<String> propertyNames = puNode.getChild("properties")
                .map(properties -> properties.getChildren("property").stream()
                        .map(property -> getAttributeValue("name", property))
                        .filter(Objects::nonNull)
                        .collect(toList()))
                .orElse(emptyList());
        if (propertyNames.stream().anyMatch(name -> name.startsWith("openjpa."))) {
            return false;
        }
        if (propertyNames.stream().anyMatch(name -> name.startsWith("hibernate.") || name.startsWith("eclipselink."))) {
            return true;
        }
        Object parent = getCursor().getParentTreeCursor().getValue();
        String persistenceVersion = parent instanceof Xml.Tag ? getAttributeValue("version", (Xml.Tag) parent) : null;
        return persistenceVersion != null && !persistenceVersion.trim().matches("1(\\..*)?|2(\\.0(\\..*)?)?");
    }

    private SharedDataHolder extractData(Xml.Tag puNode) {
        SharedDataHolder sdh = new SharedDataHolder();

        // Determine if data cache is enabled
        sdh.sharedCacheModeElement = puNode.getChild("shared-cache-mode").orElse(null);
        getDataCacheProps(puNode, sdh);

        // true if shared-cache-mode set to UNSPECIFIED.
        sdh.sharedCacheModeElementUnspecified = sdh.sharedCacheModeElement != null && SHARED_CACHE_MODE_VALUE_UNSPECIFIED.equals(getTextContent(sdh.sharedCacheModeElement));
        // true if shared-cache-mode set to UNSPECIFIED.
        sdh.sharedCacheModePropertyUnspecified = sdh.sharedCacheModeProperty != null && SHARED_CACHE_MODE_VALUE_UNSPECIFIED.equals(getAttributeValue("value", sdh.sharedCacheModeProperty));

        return sdh;
    }

    private @Nullable String getAttributeValue(String attrName, Xml.Tag node) {
        for (Xml.Attribute attribute : node.getAttributes()) {
            if (attribute.getKeyAsString().equals(attrName)) {
                return attribute.getValue().getValue();
            }
        }
        return null;
    }

    private Xml.Tag updateAttributeValue(String attrName, String newValue, Xml.Tag node) {
        List<Xml.Attribute> updatedAttributes = new ArrayList<>();
        for (Xml.Attribute attribute : node.getAttributes()) {
            if (attribute.getKeyAsString().equals(attrName)) {
                attribute = attribute.withValue(
                        new Xml.Attribute.Value(Tree.randomId(),
                                "",
                                attribute.getMarkers(),
                                attribute.getValue().getQuote(),
                                newValue));
                updatedAttributes.add(attribute);
            } else {
                updatedAttributes.add(attribute);
            }
        }
        return node.withAttributes(updatedAttributes);
    }

    /**
     * Loop through all the properties and gather openjpa.DataCache,
     * javax.persistence.sharedCache.mode or eclipselink.cache.shared.default properties
     *
     * @param sdh Data holder for the properties.
     */
    private void getDataCacheProps(Xml.Tag puNode, SharedDataHolder sdh) {
        Optional<Xml.Tag> propertiesTag = puNode.getChild("properties");
        if (propertiesTag.isPresent()) {
            sdh.propertiesElement = propertiesTag.get();
            List<Xml.Tag> properties = sdh.propertiesElement.getChildren("property");
            for (Xml.Tag prop : properties) {
                String name = getAttributeValue("name", prop);
                if (name != null) {
                    if ("openjpa.DataCache".equals(name)) {
                        sdh.openJPACacheProperty = prop;
                    } else if ("javax.persistence.sharedCache.mode".equals(name)) {
                        sdh.sharedCacheModeProperty = prop;
                    } else if ("eclipselink.cache.shared.default".equals(name)) {
                        sdh.eclipselinkCacheProperty = prop;
                    }
                }
            }
        }
    }

    private @Nullable String getTextContent(Xml.@Nullable Tag node) {
        if (node != null) {
            String textContent = null;
            Optional<String> optionalValue = node.getValue();
            if (optionalValue.isPresent()) {
                textContent = optionalValue.get();
            }
            return textContent;
        }
        return null;
    }

    private @Nullable String interpretOpenJPAPropertyValue(@Nullable String propVal) {
        if (propVal != null) {
            if ("false".equalsIgnoreCase(propVal)) {
                return "NONE";
            }
            if ("true".equalsIgnoreCase(propVal)) {
                return "ALL";
            }
            if (propVal.matches("(?i:true)\\(ExcludedTypes=.*")) {
                return "DISABLE_SELECTIVE";
            }
            if (propVal.matches("(?i:true)\\(Types=.*")) {
                return "ENABLE_SELECTIVE";
            }
        }
        return null;
    }

    // convert the scmValue to either true or false.
    // return null for complex values.
    private @Nullable String convertScmValue(String scmValue) {
        if ("NONE".equals(scmValue)) {
            return "false";
        }
        if ("ALL".equals(scmValue)) {
            return "true";
        }
        // otherwise, don't process it
        return null;
    }
}
