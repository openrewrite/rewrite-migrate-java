package org.openrewrite.java.migrate.apache.commons.lang;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.apache.commons.lang3.StringUtils;

public class Equals {
    @BeforeTemplate
    boolean before(String s) {
        return StringUtils.equals(s, "string");
    }

    @AfterTemplate
    boolean after(String s) {
        return s.equals("string");
    }
}
