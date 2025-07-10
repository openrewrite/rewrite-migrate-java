package org.openrewrite.java.migrate.joda.templates;

import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_SECONDS;

//final MethodMatcher secondsPlus = new MethodMatcher(JODA_SECONDS + " plus(int)");
// ->
//final MethodMatcherBuilder plusInt = from().method("plus").with("int");

/*
private MethodMatcherBuilder from() {
    return from(JODA_SECONDS);
}

//to Templates
private MethodMatcher build(MethodMatcherBuilder builder) {
    return builder.build();
}
private MethodMatcherBuilder from(String className) {
    return MethodMatcherBuilder.builder(className);
}
*/


public class MethodMatcherBuilder {
    private Optional<Boolean> isConstructor = Optional.empty();
    private String className;
    private String methodName;
    private List<String> arguments = new ArrayList<>();

    private MethodMatcherBuilder(String className) {
        this.className = className;
    };

    public static MethodMatcherBuilder builder(String className) {
        return new MethodMatcherBuilder(className);
    }

    public MethodMatcherBuilder contructor() {
        if (!isConstructor.isPresent()) {throw new IllegalStateException("Type can be set only once");}
        if (isConstructor.get() == Boolean.FALSE) {throw new IllegalStateException("Type already set to method");}
        isConstructor = Optional.of(Boolean.TRUE);
        return this;
    }
    public MethodMatcherBuilder method(String methodName) {
        if (!isConstructor.isPresent()) {throw new IllegalStateException("Type can be set only once");}
        if (isConstructor.get() == Boolean.TRUE) {throw new IllegalStateException("Type already set to method");}
        isConstructor = Optional.of(Boolean.FALSE);
        this.methodName = methodName;
        return this;
    }

    public MethodMatcherBuilder with(String argumentType) {
        if (!isConstructor.isPresent()) {throw new IllegalStateException("Type is not set");}
        arguments.add(argumentType);
        return this;
    }

    public MethodMatcher build() {
        return new MethodMatcher(
                className + " " +
                        getCallType() +
                        "(" +
                        getArguments() +
                        ")"
        );

    }

    private String getArguments() {
        return String.join(", ", arguments);
    }

    private String getCallType() {
        if (!isConstructor.isPresent()) throw new IllegalStateException("Type is not set");
        return isConstructor.get() == Boolean.TRUE ? "<constructor>" : methodName;
    }
}
