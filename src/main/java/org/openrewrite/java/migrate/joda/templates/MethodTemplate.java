package org.openrewrite.java.migrate.joda.templates;

import lombok.Value;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;

import java.util.function.Function;

@Value
public class MethodTemplate {
  MethodMatcher matcher;
  JavaTemplate template;
  Function<MethodCall, Expression[]> templateArgsFunc;

  public MethodTemplate(MethodMatcher matcher, JavaTemplate template) {
    this(matcher, template, m -> {
      Expression select = isInstanceCall(m) ? ((J.MethodInvocation) m).getSelect() : null;

      if (m.getArguments().isEmpty() || m.getArguments().get(0) instanceof J.Empty) {
        return select != null ? new Expression[]{select} : new Expression[0];
      }

      Expression[] args = m.getArguments().toArray(new Expression[0]);
      if (select != null) {
        Expression[] newArgs = new Expression[args.length + 1];
        newArgs[0] = select;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
      }
      return args;
    });
  }

  public MethodTemplate(MethodMatcher matcher, JavaTemplate template, Function<MethodCall, Expression[]> templateArgsFunc) {
    this.matcher = matcher;
    this.template = template;
    this.templateArgsFunc = templateArgsFunc;
  }

  private static boolean isInstanceCall(MethodCall m) {
    if (!(m instanceof J.MethodInvocation)) {
      return false;
    }
    J.MethodInvocation mi = (J.MethodInvocation) m;
    if (mi.getSelect() instanceof J.FieldAccess) {
      return ((J.FieldAccess) mi.getSelect()).getName().getFieldType() != null;
    }
    if (mi.getSelect() instanceof J.Identifier) {
      return ((J.Identifier) mi.getSelect()).getFieldType() != null;
    }
    return true;
  }
}
