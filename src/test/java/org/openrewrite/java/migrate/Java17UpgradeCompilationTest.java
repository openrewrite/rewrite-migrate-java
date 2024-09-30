//Run the Java 17 upgrade recipe on this code to migrate from Java 8 to Java 17. After applying the upgrade, the code will be modified, which leads to compilation errors when building with JDK 17
//It will do such change:
/* if (obj1.getClass().equals(obj2.getClass()) &&
                obj1 instanceof Comparable<?> comparable) {
            int diff = comparable.compareTo(obj2);
            assertEquals(-3, diff); // String comparison: "abc" vs "def"
        }
    }
    ***solution: revert back to original code***
*/
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Java17UpgradeCompilationTest {

    @Test
    void testWithoutUpgrade() {
        Object obj1 = "abc";
        Object obj2 = "def";

        if (obj1.getClass().equals(obj2.getClass()) &&
          obj1 instanceof Comparable) {
            int diff = ((Comparable)obj1).compareTo(obj2);
            assertEquals(-3, diff); // String comparison: "abc" vs "def"
        }
    }
}
