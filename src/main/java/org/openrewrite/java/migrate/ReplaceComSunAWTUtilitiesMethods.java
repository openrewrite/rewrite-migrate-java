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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
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

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceComSunAWTUtilitiesMethods extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.sun.awt.AWTUtilities isTranslucencySupported(com.sun.awt.AWTUtilities.Translucency)",
            required = false)
    String getAWTIsWindowsTranslucencyPattern;


    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities isWindowOpaque(java.awt.Window)",
            required = false)
    String isWindowOpaquePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities isTranslucencyCapable(java.awt.GraphicsConfiguration)",
            required = false)
    String isTranslucencyCapablePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities setWindowOpacity(java.awt.Window, float)",
            required = false)
    String setWindowOpacityPattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities getWindowOpacity(java.awt.Window)",
            required = false)
    String getWindowOpacityPattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilitiesTest getWindowShape(java.awt.Window)",
            required = false)
    String getWindowShapePattern;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "com.test.AWTUtilities setComponentMixingCutoutShape(java.awt.Component,java.awt.Shape)",
            required = false)
    String setComponentMixingCutoutShapePattern;

    @JsonCreator
    public ReplaceComSunAWTUtilitiesMethods() {
        getAWTIsWindowsTranslucencyPattern = "com.sun.awt.AWTUtilities isTranslucencySupported(com.sun.awt.AWTUtilities.Translucency)";
        getWindowOpacityPattern = "com.sun.awt.AWTUtilities getWindowOpacity(java.awt.Window)";
        getWindowShapePattern = "com.sun.awt.AWTUtilities getWindowShape(java.awt.Window)";
        isWindowOpaquePattern = "com.sun.awt.AWTUtilities isWindowOpaque(java.awt.Window)";
        isTranslucencyCapablePattern = "com.sun.awt.AWTUtilities isTranslucencyCapable(java.awt.GraphicsConfiguration)";
        setComponentMixingCutoutShapePattern = "com.sun.awt.AWTUtilities setComponentMixingCutoutShape(java.awt.Component,java.awt.Shape)";
        setWindowOpacityPattern = "com.sun.awt.AWTUtilities setWindowOpacity(java.awt.Window, float)";
    }

    @Override
    public String getDisplayName() {
        return "Replace `com.sun.awt.AWTUtilities` static method invocations";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces several static calls  in `com.sun.awt.AWTUtilities` with the JavaSE 11 equivalent. " +
               "The methods replaced are `AWTUtilities.isTranslucencySupported()`, `AWTUtilities.setWindowOpacity()`, `AWTUtilities.getWindowOpacity()`, " +
               "`AWTUtilities.getWindowShape()`, `AWTUtilities.isWindowOpaque()`, `AWTUtilities.isTranslucencyCapable()` and `AWTUtilities.setComponentMixingCutoutShape()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher getAWTIsWindowsTranslucencyMethod = new MethodMatcher(getAWTIsWindowsTranslucencyPattern);
        MethodMatcher getWindowOpacityPatternMethod = new MethodMatcher(getWindowOpacityPattern);
        MethodMatcher getWindowShapePatternMethod = new MethodMatcher(getWindowShapePattern);
        MethodMatcher isWindowOpaquePatternMethod = new MethodMatcher(isWindowOpaquePattern);
        MethodMatcher isTranslucencyCapablePatternMethod = new MethodMatcher(isTranslucencyCapablePattern);
        MethodMatcher setComponentMixingCutoutShapePatternMethod = new MethodMatcher(setComponentMixingCutoutShapePattern);
        MethodMatcher setWindowOpacityPatternMethod = new MethodMatcher(setWindowOpacityPattern);

        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                super.visitMethodInvocation(mi, ctx);
                if (getAWTIsWindowsTranslucencyMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    maybeAddImport("java.awt.GraphicsDevice", false);
                    maybeAddImport("java.awt.GraphicsEnvironment", false);
                    maybeAddImport("java.awt.Window", false);
                    maybeAddImport("java.awt.GraphicsDevice.WindowTranslucency", false);
                    String templateString = "GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().isWindowTranslucencySupported(WindowTranslucency." +
                                            ((J.FieldAccess) mi.getArguments().get(0)).getSimpleName();
                    return JavaTemplate.builder(templateString).
                            imports("java.awt.GraphicsDevice",
                                    "java.awt.GraphicsEnvironment",
                                    "java.awt.Window",
                                    "java.awt.GraphicsDevice.WindowTranslucency")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace())
                            .withPrefix(mi.getPrefix());
                }
                if (isWindowOpaquePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.isOpaque()")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getArguments().get(0))
                            .withPrefix(mi.getPrefix());
                }
                if (isTranslucencyCapablePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.isTranslucencyCapable()")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getArguments().get(0))
                            .withPrefix(mi.getPrefix());
                }
                if (setWindowOpacityPatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.setOpacity(#{any()})")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(),
                                    mi.getArguments().get(0),
                                    mi.getArguments().get(1))
                            .withPrefix(mi.getPrefix());
                }
                if (getWindowOpacityPatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.getOpacity()")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getArguments().get(0))
                            .withPrefix(mi.getPrefix());
                }
                if (getWindowShapePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.getShape()")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(), mi.getArguments().get(0))
                            .withPrefix(mi.getPrefix());
                }
                if (setComponentMixingCutoutShapePatternMethod.matches(mi)) {
                    maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                    return JavaTemplate.builder("#{any()}.setMixingCutoutShape(#{any()})")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replace(),
                                    mi.getArguments().get(0),
                                    mi.getArguments().get(1))
                            .withPrefix(mi.getPrefix());
                }
                return mi;
            }
        };
    }
}
