package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.java.Assertions.java;

public class DeleteDeprecatedFinalizeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-21.yml",
          "org.openrewrite.java.migrate.DeleteDeprecatedFinalize");
    }

    @Test
    void deleteDeprecatedFinalize() {
        rewriteRun(
          //language=java
          java(
            """
               package java.awt.color;
               
               import java.awt.color.ICC_Profile;
               import java.awt.image.ColorModel;
               import java.awt.image.IndexColorModel;
               
               public class Test {
               	public static void main(String[] args) {
               		// flag
               		byte ff = (byte) 0xff;
               		byte[] r = { ff, 0, 0, ff, 0 };
               		byte[] g = { 0, ff, 0, ff, 0 };
               		byte[] b = { 0, 0, ff, ff, 0 };
               
               		ICC_Profile profile = ICC_Profile.getInstance(ICC_Profile.CLASS_COLORSPACECONVERSION);
               		// flag
               		profile.finalize();
               
               		ColorModel cm = new IndexColorModel(3, 5, r, g, b);
               
               		// flag
               		cm.finalize();
               
               		IndexColorModel icm = new IndexColorModel(3, 5, r, g, b);
               		// flag
               		icm.finalize();
               
               	}
               }
               """,
            """
               package java.awt.color;
               
               import java.awt.color.ICC_Profile;
               import java.awt.image.ColorModel;
               import java.awt.image.IndexColorModel;
               
               public class Test {
               	public static void main(String[] args) {
               		// flag
               		byte ff = (byte) 0xff;
               		byte[] r = { ff, 0, 0, ff, 0 };
               		byte[] g = { 0, ff, 0, ff, 0 };
               		byte[] b = { 0, 0, ff, ff, 0 };
               
               		ICC_Profile profile = ICC_Profile.getInstance(ICC_Profile.CLASS_COLORSPACECONVERSION);
               
               		ColorModel cm = new IndexColorModel(3, 5, r, g, b);
               
               		IndexColorModel icm = new IndexColorModel(3, 5, r, g, b);
               
               	}
               }
               """));
    }
}
