/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceComSunAWTUtilitiesMethods extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.sun.awt.AWTUtilities isTranslucencySupported(com.sun.awt.AWTUtilities.Translucency)")
    String getAWTIsWindowsTransclucencyPattern;


    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities isWindowOpaque(java.awt.Window)")
    String isWindowOpaquePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities isTranslucencyCapable(java.awt.GraphicsConfiguration)")
    String isTranslucencyCapablePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities setWindowOpacity(java.awt.Window, float)")
    String setWindowOpacityPattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities getWindowOpacity(java.awt.Window)")
    String getWindowOpacityPattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilitiesTest getWindowShape(java.awt.Window)")
    String getWindowShapePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities setComponentMixingCutoutShape(java.awt.Component,java.awt.Shape)")
    String setComponentMixingCutoutShapePattern;

    public ReplaceComSunAWTUtilitiesMethods() {
        getAWTIsWindowsTransclucencyPattern = "com.sun.awt.AWTUtilities isTranslucencySupported(com.sun.awt.AWTUtilities.Translucency)";
        isWindowOpaquePattern = "com.sun.awt.AWTUtilities isWindowOpaque(java.awt.Window)";
        isTranslucencyCapablePattern = "com.sun.awt.AWTUtilities isTranslucencyCapable(java.awt.GraphicsConfiguration)";
        setWindowOpacityPattern = "com.sun.awt.AWTUtilities setWindowOpacity(java.awt.Window, float)";
        getWindowOpacityPattern = "com.sun.awt.AWTUtilities getWindowOpacity(java.awt.Window)";
        getWindowShapePattern = "com.sun.awt.AWTUtilities getWindowShape(java.awt.Window)";
        setComponentMixingCutoutShapePattern = "com.sun.awt.AWTUtilities setComponentMixingCutoutShape(java.awt.Component,java.awt.Shape)";
    }

    public ReplaceComSunAWTUtilitiesMethods(String getAWTIsWindowsTransclucencyPattern, String isWindowOpaquePattern, String isTranslucencyCapablePattern,
                                            String setWindowOpacityPattern, String getWindowOpacityPattern, String getWindowShapePattern, String setComponentMixingCutoutShapePattern) {
        this.getAWTIsWindowsTransclucencyPattern = getAWTIsWindowsTransclucencyPattern;
        this.isWindowOpaquePattern = isWindowOpaquePattern;
        this.isTranslucencyCapablePattern = isTranslucencyCapablePattern;
        this.setWindowOpacityPattern = setWindowOpacityPattern;
        this.getWindowOpacityPattern = getWindowOpacityPattern;
        this.getWindowShapePattern = getWindowShapePattern;
        this.setComponentMixingCutoutShapePattern = setComponentMixingCutoutShapePattern;
    }

    @Override
    public String getDisplayName() {
        return "Replace `com.sun.awt.AWTUtilities` static method invocations";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces several static calls  in `com.sun.awt.AWTUtilities` with the JavaSE 11 equivalent." +
                "The methods replaced are `AWTUtilities.isTranslucencySupported()`, `AWTUtilities.setWindowOpacity()`, `AWTUtilities.getWindowOpacity()`," +
                "`AWTUtilities.getWindowShape()`, `AWTUtilities.isWindowOpaque()`, `AWTUtilities.isTranslucencyCapable()` and `AWTUtilities.setComponentMixingCutoutShape()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher getAWTIsWindowsTransclucencyMethod = new MethodMatcher(getAWTIsWindowsTransclucencyPattern);
        MethodMatcher isWindowOpaquePatternMethod = new MethodMatcher(isWindowOpaquePattern);
        MethodMatcher isTranslucencyCapablePatternMethod = new MethodMatcher(isTranslucencyCapablePattern);
        MethodMatcher setWindowOpacityPatternMethod = new MethodMatcher(setWindowOpacityPattern);
        MethodMatcher getWindowOpacityPatternMethod = new MethodMatcher(getWindowOpacityPattern);
        MethodMatcher getWindowShapePatternMethod = new MethodMatcher(getWindowShapePattern);
        MethodMatcher setComponentMixingCutoutShapePatternMethod = new MethodMatcher(setComponentMixingCutoutShapePattern);

        String IMPORT_GRAPHIC_DEVICE = "java.awt.GraphicsDevice";
        String IMPORT_GRAPHIC_ENV = "java.awt.GraphicsEnvironment";
        String IMPORT_AWT_WINDOW = "java.awt.Window";
        String IMPORT_WINDOWS_DEVICE_TRANSLUCENCY = "java.awt.GraphicsDevice.WindowTranslucency";
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                super.visitMethodInvocation(mi, ctx);
                Space temp = mi.getPrefix();
                if (getAWTIsWindowsTransclucencyMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    maybeAddImport(IMPORT_GRAPHIC_DEVICE, false);
                    maybeAddImport(IMPORT_GRAPHIC_ENV, false);
                    maybeAddImport(IMPORT_AWT_WINDOW, false);
                    maybeAddImport(IMPORT_WINDOWS_DEVICE_TRANSLUCENCY, false);
                    String templateString = "GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isWindowTranslucencySupported(WindowTranslucency."
                            + ((J.FieldAccess) mi.getArguments().get(0)).getSimpleName();
                    mi = JavaTemplate.builder(templateString).
                            imports(IMPORT_GRAPHIC_DEVICE, IMPORT_GRAPHIC_ENV, IMPORT_AWT_WINDOW, IMPORT_WINDOWS_DEVICE_TRANSLUCENCY)
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace());
                }
                if (isWindowOpaquePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".isOpaque()";
                    mi = JavaTemplate.builder(templateString)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace());
                }
                if (isTranslucencyCapablePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".isTranslucencyCapable()";
                    mi = JavaTemplate.builder(templateString)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace());
                }
                if (setWindowOpacityPatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".setOpacity(" + ((J.Literal) mi.getArguments().get(1)).getValue() + ")";
                    mi = JavaTemplate.builder(templateString)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace()).withPrefix(temp);
                }
                if (getWindowOpacityPatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".getOpacity()";
                    mi = JavaTemplate.builder(templateString).contextSensitive().build()
                            .apply(getCursor(), mi.getCoordinates().replace()).withPrefix(temp);
                }
                if (getWindowShapePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".getShape()";
                    mi = JavaTemplate.builder(templateString)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace()).withPrefix(temp);
                }
                if (setComponentMixingCutoutShapePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    String templateString = ((J.Identifier) mi.getArguments().get(0)).getSimpleName() + ".setMixingCutoutShape(" + ((J.Identifier) mi.getArguments().get(1)).getSimpleName() + ")";
                    mi = JavaTemplate.builder(templateString)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace()).withPrefix(temp);
                }
                return mi;
            }
        };
    }
}
