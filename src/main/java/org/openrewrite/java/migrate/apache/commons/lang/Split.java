package org.openrewrite.java.migrate.apache.commons.lang;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.apache.commons.lang3.StringUtils;

public class Split {
    @BeforeTemplate
    String[] before(String s) {
        return StringUtils.split(s);
    }

    @AfterTemplate
    String[] after(String s) {
        return s.split(" ");
    }
}
