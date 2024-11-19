package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.List;

public class HandleTypeNotPresentExceptionInsteadOfArrayStoreException extends Recipe {
    @Override
    public String getDisplayName() {
        return "Handle TypeNotPresentException instead of ArrayStoreException";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces catch blocks for ArrayStoreException around getAnnotation() with TypeNotPresentException or both exceptions, to ensure compatibility with Java 11+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitTry(J.Try tryStmt, ExecutionContext ctx ) {
                if(containsGetAnnotation(tryStmt.getBody()))
                {
                    for (J.Try.Catch catchClause : tryStmt.getCatches()) {
                        if(catchClause.getParameter().getType() != null && "java.lang.ArrayStoreException".equals(catchClause.getParameter().getType().toString())) {
                            JavaType.Class typeNotPresentExceptionType = new JavaType.Class(null,0,"java.lang.TypeNotPresentException", null, null,null, null,null,null,null,null);
                            J.Try.Catch updatedCatch = catchClause.withParameter(catchClause.getParameter().withType(typeNotPresentExceptionType));
//                                String multiCatchType = "java.lang.ArrayStoreException | java.lang.TypeNotPresentException";
//                                updatedCatch = updatedCatch.withParameter(catchClause.getParameter().withType(JavaType.Class.build(multicatchType)));
                                tryStmt = tryStmt.withCatches((List<J.Try.Catch>) Collections.singleton(updatedCatch));
                        }
                    }
                }
                return super.visitTry(tryStmt,ctx);
        }

        private boolean containsGetAnnotation(J.Block body) {
                if(body == null) return false;
                for( J node : body.getStatements()) {
                if (node instanceof J.MethodInvocation) {
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) node;
                    if ("getAnnotation".equals(methodInvocation.getSimpleName())) {
                        return true;
                    }
                }
            }
            return false;
}
        };
    }
}

