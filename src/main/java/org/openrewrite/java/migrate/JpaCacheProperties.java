/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = true)
public class JpaCacheProperties extends Recipe {

    private static final XPathMatcher PERSISTANCE_MATCHER = new XPathMatcher("/persistence");
    private static final String SHARED_CACHE_MODE_VALUE_UNSPECIFIED = "UNSPECIFIED";

    @Override
    public String getDisplayName() {
        return "Disable the persistence unit second-level cache";
    }

    @Override
    public String getDescription() {
        return "Sets an explicit value for the shared cache mode.";
    }

    private static class SharedDataHolder {
        boolean sharedCacheModePropertyUnspecified;
        boolean sharedCacheModeElementUnspecified;
        @Nullable Xml.Tag sharedCacheModeElement;
        @Nullable Xml.Tag propertiesElement;
        @Nullable Xml.Tag sharedCacheModeProperty;
        @Nullable Xml.Tag openJPACacheProperty;
        @Nullable Xml.Tag eclipselinkCacheProperty;

        // Flag in the following conditions:
        //   an openjpa.DataCache property is present
        //   either shared-cache-mode or javax.persistence.sharedCache.mode is set to UNSPECIFIED
        //   both shared-cache-mode and javax.persistence.sharedCache.mode are present
        //   None of the properties/elements are present
        public boolean shouldFlag() {
            return (openJPACacheProperty != null ||
                    ((sharedCacheModeElement != null && sharedCacheModeElementUnspecified) || (sharedCacheModeProperty != null && sharedCacheModePropertyUnspecified)) ||
                    (sharedCacheModeElement != null && sharedCacheModeProperty != null) ||
                    (sharedCacheModeElement == null && sharedCacheModeProperty == null && eclipselinkCacheProperty == null));
        }
    }

    /**
     * Search for the child element <shared-cache-mode>.
     * <p>
     * Return Node <shared-cache-mode> or null
     *
     * @return Node
     */
    @Nullable
    private Xml.Tag getSharedCacheModeNode(Xml.Tag puNode) {
        Optional<Xml.Tag> cacheModes = puNode.getChild("shared-cache-mode");
        return cacheModes.orElse(null);
    }

    @Nullable
    private String getAttributeValue(String attrName, Xml.Tag node) {
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
                        new Xml.Attribute.Value(attribute.getId(),
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

    @Nullable
    private String getTextContent(@Nullable Xml.Tag node) {
        if (node != null) {
            String textContent = null;
            Optional<String> optionalValue = node.getValue();
            if (optionalValue.isPresent()) {
                textContent = optionalValue.get();
            }
            // returns true if shared-cache-mode set to UNSPECIFIED.
            return textContent;
        }
        return null;
    }

    /**
     * Indicates if the value of the give node UNSPECIFIED
     *
     * @param node node for <shared-cache-mode>
     * @return boolean
     */
    private boolean isSharedCacheModeElementUnspecified(@Nullable Xml.Tag node) {
        if (node != null) {
            // returns true if shared-cache-mode set to UNSPECIFIED.
            return (SHARED_CACHE_MODE_VALUE_UNSPECIFIED.equals(getTextContent(node)));
        } else {
            return false;
        }
    }

    /**
     * Indicates if the value of the give property is UNSPECIFIED
     *
     * @param node Xml.Tag
     * @return boolean
     */
    private boolean isSharedCacheModePropertyUnspecified(@Nullable Xml.Tag node) {
        if (node != null) {
            String value = getAttributeValue("value", node);

            // returns true if shared-cache-mode set to UNSPECIFIED.
            return (SHARED_CACHE_MODE_VALUE_UNSPECIFIED.equals(value));
        }
        return false;

    }

    private SharedDataHolder extractData(Xml.Tag puNode) {
        SharedDataHolder sdh = new SharedDataHolder();

        // Determine if data cache is enabled
        sdh.sharedCacheModeElement = getSharedCacheModeNode(puNode);
        getDataCacheProps(puNode, sdh);

        sdh.sharedCacheModeElementUnspecified = isSharedCacheModeElementUnspecified(sdh.sharedCacheModeElement);
        sdh.sharedCacheModePropertyUnspecified = isSharedCacheModePropertyUnspecified(sdh.sharedCacheModeProperty);

        return sdh;
    }

    private Xml.Tag updatePropertyValue(Xml.Tag parent, Xml.Tag orginalProp, Xml.Tag updatedProp) {
        //noinspection unchecked
        return parent.withContent(ListUtils.map((List<Content>)parent.getContent(), content ->
                content == orginalProp ? updatedProp : content
        ));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new HasSourcePath<>("**/persistence.xml"), new XmlVisitor<ExecutionContext>() {
            @Nullable
            private String version = null;

            @Nullable
            private String interpretOpenJPAPropertyValue(@Nullable String propVal) {
                if (propVal != null) {
                    if ("false".equalsIgnoreCase(propVal)) {
                        return "NONE";
                    } else if ("true".equalsIgnoreCase(propVal)) {
                        return "ALL";
                    } else if (propVal.matches("(?i:true)\\(ExcludedTypes=.*")) {
                        return "DISABLE_SELECTIVE";
                    } else if (propVal.matches("(?i:true)\\(Types=.*")) {
                        return "ENABLE_SELECTIVE";
                    }
                }
                return null;
            }

            // return boolean true if version is 1.0
            private boolean isVersion10(@Nullable String version) {
                // If the version is missing the spec is assumed to be at
                // the latest version, thus not V1.0.
                return "1.0".equals(version);
            }

            // shared-cache-mode should go before <validation-mode> and <properties> if present.
            // If not present, it should go at the end of the persistence-unit element.
            @Nullable
            public Xml.Tag getARefNode(Xml.Tag puNode) {
                for (Xml.Tag node : puNode.getChildren()) {
                    if ("validation-mode".equals(node.getName())) {
                        return node;
                    } else if ("properties".equals(node.getName())) {
                        return node;
                    }
                }
                return null;
            }

            // convert the scmValue to either true or false.
            // return null for complex values.
            @Nullable
            private String convertScmValue(String scmValue) {
                if ("NONE".equals(scmValue)) {
                    return "false";
                } else if ("ALL".equals(scmValue)) {
                    return "true";
                }
                // otherwise, don't process it
                return null;
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (PERSISTANCE_MATCHER.matches(getCursor())) {
                    version = null;
                    for (Xml.Attribute attribute : tag.getAttributes()) {
                        if (attribute.getKeyAsString().equals("version")) {
                            version = attribute.getValue().getValue();
                        }
                    }
                }

                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (!t.getName().equals("persistence-unit")) {
                    return t;
                }

                SharedDataHolder sdh = extractData(t);
                if (sdh.shouldFlag()) {
                    boolean v1 = isVersion10(version);
                    // Do we need to edit a shared cache mode property
                    if ((sdh.sharedCacheModeElement != null || sdh.sharedCacheModeProperty != null)) {
                        // if UNSPECIFIED, defaults to NONE but if present, use
                        // OpenJpa property to decide value
                        if (sdh.sharedCacheModeElement != null &&
                            sdh.sharedCacheModeElementUnspecified) {
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
                                sdh.propertiesElement = updatePropertyValue(sdh.propertiesElement, sdh.sharedCacheModeProperty, updatedProp);
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
                                Xml.Tag newNode = Xml.Tag.build("<shared-cache-mode>" + scmValue + "</shared-cache-mode>");
                                // Ideally we would insert <shared-cache-mode> before the <validation-mode> and <properties> nodes
                                Cursor parent = getCursor().getParentOrThrow();
                                t = autoFormat(addOrUpdateChild(t, newNode, parent), ctx, parent);
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
                            sdh.propertiesElement = filterTagChildren(sdh.propertiesElement, child -> child != sdh.openJPACacheProperty);

                            t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
                        }
                    }

                    // if both shared-cache-mode and javax cache property are set, delete the
                    // javax cache property
                    if (sdh.sharedCacheModeElement != null && sdh.sharedCacheModeProperty != null) {
                        sdh.propertiesElement = filterTagChildren(sdh.propertiesElement, child -> child != sdh.sharedCacheModeProperty);
                        t = addOrUpdateChild(t, sdh.propertiesElement, getCursor().getParentOrThrow());
                    }
                }
                return t;
            }
        });
    }
}
