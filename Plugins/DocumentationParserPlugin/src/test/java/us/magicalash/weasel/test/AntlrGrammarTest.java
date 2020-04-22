package us.magicalash.weasel.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.docparser.DocumentationParserPlugin;
import us.magicalash.weasel.plugin.docparser.representation.JavaMethod;
import us.magicalash.weasel.plugin.docparser.representation.JavaType;
import us.magicalash.weasel.plugin.docparser.representation.JavaVariable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AntlrGrammarTest {
    private DocumentationParserPlugin pl = new DocumentationParserPlugin();


    @Test
    public void testAnnotationType() {
        List<ParsedCodeUnit> codeUnits = parseFile("Annotation1.java");

        assertEquals(1, codeUnits.size());
        ParsedCodeUnit annotationData = codeUnits.get(0);

        assertTrue(annotationData.getParsedObject() instanceof JavaType);
        JavaType annotation = (JavaType) annotationData.getParsedObject();

        // check class level attributes
        assertEquals("Annotation1", annotation.getName());
        assertEquals("@interface", annotation.getType());

        assertEquals(1, annotation.getModifiers().size());
        assertEquals("public", annotation.getModifiers().get(0));

        assertEquals(1, annotation.getImplementsInterfaces().size());
        assertEquals("java/lang/annotation/Annotation", annotation.getImplementsInterfaces().get(0));

        assertEquals("java/lang/Object", annotation.getParentClass());

        assertEquals(1, annotation.getStartLine());
        assertEquals(3, annotation.getEndLine());

        // check that there are no fields
        assertEquals(0, annotation.getFields().size());

        // check that there is one method, called "a" returning a string
        assertEquals(1, annotation.getMethods().size());
        JavaMethod a = annotation.getMethods().get(0);
        assertEquals("String", a.getReturnType());
        assertEquals("a", a.getName());
        assertEquals(0, a.getParameters().size());
        assertEquals(0, a.getAnnotations().size());
    }


    // this test is MASSIVE. It should probably be split up,
    // but I'm not comfortable splitting it up at the moment.
    // Especially since it relies on ordering.
    @Test
    public void testClassType() {
        List<ParsedCodeUnit> codeUnits = parseFile("Test1.java");

        assertEquals(1, codeUnits.size());
        ParsedCodeUnit annotationData = codeUnits.get(0);

        assertTrue(annotationData.getParsedObject() instanceof JavaType);
        JavaType classType = (JavaType) annotationData.getParsedObject();

        // check class level attributes
        assertEquals("test/Test1", classType.getName());
        assertEquals("class", classType.getType());

        assertModifiers(classType.getModifiers(), "public");

        assertEquals(0, classType.getImplementsInterfaces().size());

        assertEquals("java/lang/Object", classType.getParentClass());

        assertEquals(7, classType.getStartLine());
        assertEquals(32, classType.getEndLine());

        // check the annotation
        assertAnnotations(classType.getAnnotations(), "baz/Bar");

        // check that there's enough methods
        assertEquals(4, classType.getMethods().size());

        // checking the constructor
        JavaMethod method = classType.getMethods().get(0);
        assertEquals("#constructor", method.getName());
        assertModifiers(method.getModifiers(), "public");
        assertEquals("test/Test1", method.getReturnType());
        assertEquals(9, method.getStartLine());
        assertEquals(11, method.getEndLine());
        assertEquals(1, method.getParameters().size());
        JavaVariable parameter = method.getParameters().get(0);
        assertEquals("test/Test1", parameter.getType());
        assertEquals("foo", parameter.getName());

        // checking the bar() method
        method = classType.getMethods().get(1);
        assertEquals("bar", method.getName());
        assertModifiers(method.getModifiers(), "public", "strictfp");
        assertEquals("test/Test1", method.getReturnType());
        assertEquals(17, method.getStartLine());
        assertEquals(19, method.getEndLine());
        assertEquals(0, method.getParameters().size());

        // checking the baz() method
        method = classType.getMethods().get(2);
        assertEquals("baz", method.getName());
        assertModifiers(method.getModifiers(), "private");
        assertEquals("void", method.getReturnType());
        assertEquals(22, method.getStartLine());
        assertEquals(24, method.getEndLine());
        assertEquals(2, method.getParameters().size());

        parameter = method.getParameters().get(0);
        assertEquals("foo/Bar", parameter.getType());
        assertEquals("b1", parameter.getName());
        assertModifiers(parameter.getModifiers(), "final");

        parameter = method.getParameters().get(1);
        assertEquals("foo/Bar", parameter.getType());
        assertEquals("b2", parameter.getName());
        assertModifiers(parameter.getModifiers(), "final");
        assertAnnotations(parameter.getAnnotations(), "NotNull");

        // checking the baz2() method
        method = classType.getMethods().get(3);
        assertEquals("baz2", method.getName());
        assertModifiers(method.getModifiers(), "private");
        assertEquals("void", method.getReturnType());
        assertEquals(26, method.getStartLine());
        assertEquals(31, method.getEndLine());
        assertEquals(1, method.getParameters().size());

        parameter = method.getParameters().get(0);
        assertEquals("Bar2", parameter.getType());
        assertEquals("g", parameter.getName());

        // check the foo field
        assertEquals(1, classType.getFields().size());
        JavaVariable field = classType.getFields().get(0);
        assertEquals("foo", field.getName());
        assertEquals("Foo", field.getType());
        assertModifiers(field.getModifiers(), "private");
        assertAnnotations(field.getAnnotations(), "lombok/Setter", "lombok/Getter");
    }

    @Test
    public void testEnumType() {
        List<ParsedCodeUnit> codeUnits = parseFile("Enum1.java");

        assertEquals(1, codeUnits.size());
        ParsedCodeUnit annotationData = codeUnits.get(0);

        assertTrue(annotationData.getParsedObject() instanceof JavaType);
        JavaType annotation = (JavaType) annotationData.getParsedObject();

        // check class level attributes
        assertEquals("Enum1", annotation.getName());
        assertEquals("enum", annotation.getType());

        assertEquals(1, annotation.getModifiers().size());
        assertEquals("public", annotation.getModifiers().get(0));

        assertEquals(0, annotation.getImplementsInterfaces().size());

        assertEquals("java/lang/Enum", annotation.getParentClass());

        assertEquals(1, annotation.getStartLine());
        assertEquals(9, annotation.getEndLine());

        // check that there are 3 fields, the enum values
        assertEquals(3, annotation.getFields().size());
        JavaVariable field = annotation.getFields().get(0);
        assertEquals("A", field.getName());
        assertEquals("Enum1", field.getType());
        assertAnnotations(field.getAnnotations(), "Fart");

        field = annotation.getFields().get(1);
        assertEquals("B", field.getName());
        assertEquals("Enum1", field.getType());

        field = annotation.getFields().get(2);
        assertEquals("C", field.getName());
        assertEquals("Enum1", field.getType());

        // check that there is one method, called "A"
        assertEquals(1, annotation.getMethods().size());
        JavaMethod a = annotation.getMethods().get(0);
        assertEquals("void", a.getReturnType());
        assertEquals("A", a.getName());
        assertEquals(0, a.getParameters().size());
        assertEquals(0, a.getAnnotations().size());
    }

    @Test
    public void testInterfaceDeclaration() {
        List<ParsedCodeUnit> codeUnits = parseFile("Interface1.java");

        assertEquals(1, codeUnits.size());
        ParsedCodeUnit annotationData = codeUnits.get(0);

        assertTrue(annotationData.getParsedObject() instanceof JavaType);
        JavaType annotation = (JavaType) annotationData.getParsedObject();

        // check class level attributes
        assertEquals("test/Interface1", annotation.getName());
        assertEquals("interface", annotation.getType());

        assertEquals(1, annotation.getModifiers().size());
        assertEquals("public", annotation.getModifiers().get(0));

        assertEquals(1, annotation.getImplementsInterfaces().size());
        assertEquals("bar/Bar", annotation.getImplementsInterfaces().get(0));

        assertNull(annotation.getParentClass());

        assertEquals(6, annotation.getStartLine());
        assertEquals(23, annotation.getEndLine());

        // check that there are 3 fields, the enum values
        assertEquals(1, annotation.getFields().size());
        JavaVariable field = annotation.getFields().get(0);
        assertEquals("NUM", field.getName());
        assertEquals("int", field.getType());
        assertModifiers(field.getModifiers(), "public", "final");

        // check that there is one method, called "A"
        assertEquals(3, annotation.getMethods().size());
        JavaMethod method = annotation.getMethods().get(0);
        assertEquals("foo/oof/Foo", method.getReturnType());
        assertEquals("foo", method.getName());
        assertEquals(0, method.getParameters().size());
        assertModifiers(method.getModifiers(), "public");

        method = annotation.getMethods().get(1);
        assertEquals("void", method.getReturnType());
        assertEquals("bar", method.getName());
        assertEquals(0, method.getParameters().size());
        assertAnnotations(method.getAnnotations(), "Override");
        assertModifiers(method.getModifiers(), "public");

        method = annotation.getMethods().get(2);
        assertEquals("void", method.getReturnType());
        assertEquals("bar2", method.getName());
        assertEquals(0, method.getParameters().size());
        assertModifiers(method.getModifiers(), "default", "public");
    }

    @Test
    public void testMultipleDeclarations() {
        List<ParsedCodeUnit> codeUnits = parseFile("InnerClass.java");

        assertEquals(3, codeUnits.size());
        assertTrue(codeUnits.get(0).getParsedObject() instanceof JavaType);

        JavaType type = (JavaType) codeUnits.get(0).getParsedObject();
        assertEquals(1, type.getMethods().size());

        type = (JavaType) codeUnits.get(1).getParsedObject();
        assertEquals(1, type.getMethods().size());

        type = (JavaType) codeUnits.get(2).getParsedObject();
        assertEquals(1, type.getFields().size());
    }

    @Test
    public void testMultipleJavadoc() {
        // We're testing that it doesn't fail to parse here.
        // todo: might want to test that the results here are correct
        List<ParsedCodeUnit> codeUnits = parseFile("Weird.java");
        assertTrue(true);
    }

    private void assertAnnotations(Map<String, Map<String, String>> annotations, String... names) {
        assertEquals(names.length, annotations.size());
        for (String name : names) {
            assertNotNull(annotations.get(name));
        }
    }

    private void assertModifiers(List<String> actualModifiers, String... expectedModifiers) {
        assertEquals(expectedModifiers.length, actualModifiers.size());
        for (int i = 0; i < actualModifiers.size(); i++) {
            assertEquals(expectedModifiers[i], actualModifiers.get(i));
        }
    }

    private List<ParsedCodeUnit> parseFile(String fileName) {
        JsonObject obj = new JsonObject();
        JsonArray array = new JsonArray();

        InputStream file = this.getClass().getClassLoader().getResourceAsStream(fileName);
        Scanner s;
        try {
            s = new Scanner(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while (s.hasNext()) {
            array.add(s.nextLine());
        }

        obj.add("file_contents", array);

        return pl.index(obj);
    }
}
