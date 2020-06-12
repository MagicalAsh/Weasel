package us.magicalash.weasel.test.plugin;


import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.PackageHierarchy;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class PackageHierarchyTest {
    private PackageHierarchy hierarchy;

    @Before
    public void before() {
        hierarchy = new PackageHierarchy();
        hierarchy.addType("foo/Bar");
        hierarchy.addType("foo/Bar/Baz");
        hierarchy.addType("foo/Bar/Bar2");
        hierarchy.addType("java/lang/String");
        hierarchy.addType("Weasel");
    }

    @Test
    public void testToString() {
        String expected = "Package Root\n" +
                "|-java\n" +
                "| |-lang\n" +
                "| | |-String\n" +
                "|-Weasel\n" +
                "|-foo\n" +
                "| |-Bar\n" +
                "| | |-Bar2\n" +
                "| | |-Baz\n";

        assertEquals(expected, hierarchy.toString());
    }

    @Test
    public void testContains() {
        assertTrue(hierarchy.containsType("foo/Bar"));
        assertTrue(hierarchy.containsType("foo/Bar/Baz"));
        assertTrue(hierarchy.containsType("foo/Bar/Bar2"));
        assertTrue(hierarchy.containsType("java/lang/String"));
        assertTrue(hierarchy.containsType("Weasel"));

        assertFalse(hierarchy.containsType("im/not/here"));
    }

    @Test
    public void testRemove() {
        assertTrue(hierarchy.containsType("foo/Bar"));
        assertTrue(hierarchy.containsType("foo/Bar/Baz"));
        assertTrue(hierarchy.containsType("foo/Bar/Bar2"));

        assertTrue(hierarchy.remove("java/lang/String"));
        assertFalse(hierarchy.remove("java"));

        assertFalse(hierarchy.containsType("java/lang/String"));
        assertTrue(hierarchy.containsType("foo/Bar"));

        assertTrue(hierarchy.remove("foo/Bar/Bar2"));

        assertTrue(hierarchy.containsType("foo/Bar"));
        assertTrue(hierarchy.containsType("foo/Bar/Baz"));
        assertFalse(hierarchy.containsType("foo/Bar/Bar2"));

        hierarchy.addType("foo/Bar/Bar2");

        assertTrue(hierarchy.remove("foo/Bar"));

        assertFalse(hierarchy.containsType("foo/Bar"));
        assertFalse(hierarchy.containsType("foo/Bar/Baz"));
        assertFalse(hierarchy.containsType("foo/Bar/Bar2"));

        assertTrue(hierarchy.containsType("Weasel"));

        assertFalse(hierarchy.remove("im/not/here"));
    }

    @Test
    public void testAllChildren() {
        Set<String> expected = Set.of(new String[] {
                "foo",
                "foo/Bar",
                "foo/Bar/Baz",
                "foo/Bar/Bar2",
                "Weasel",
                "java",
                "java/lang",
                "java/lang/String"
        });

        assertEquals(expected, hierarchy.getAllChildren(null));

        expected = Set.of(new String[] {
                "foo/Bar",
                "foo/Bar/Baz",
                "foo/Bar/Bar2"
        });

        assertEquals(expected, hierarchy.getAllChildren("foo"));

        expected = Set.of(new String[] {
                "java/lang",
                "java/lang/String"
        });

        assertEquals(expected, hierarchy.getAllChildren("java"));
    }

    @Test
    public void testPackageTypes() {
        Set<String> expected = Set.of(new String[] {
                "foo/Bar",
                "foo/Bar/Baz",
                "foo/Bar/Bar2",
                "Weasel",
                "java/lang/String"
        });

        assertEquals(expected, hierarchy.getPackageTypes(null));
    }

    @Test
    public void testImmediateChildren() {
        Set<String> expected = Set.of(new String[] {
                "foo",
                "Weasel",
                "java"
        });

        assertEquals(expected, hierarchy.getImmediateChildren(null));

        expected = Set.of(new String[] {
                "foo/Bar"
        });

        assertEquals(expected, hierarchy.getImmediateChildren("foo"));

        expected = Set.of(new String[] {
                "java/lang"
        });

        assertEquals(expected, hierarchy.getImmediateChildren("java"));
    }
}
