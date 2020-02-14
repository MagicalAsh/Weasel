package us.magicalash.weasel.plugin.representation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a Java class.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JavaType extends JavaCodeUnit {
    /**
     * The type of class this represents. Either 'class', 'enum', 'interface', or 'annotation'
     */
    private String type;

    /**
     * A String representing the fully-qualified name of the parent class.
     */
    private String parentClass;

    /**
     * A list of Strings representing the fully-qualified names of all interfaces being implemented.
     */
    private List<String> implementsInterfaces;

    /**
     * A list of all fields within the class.
     */
    private List<JavaVariable> fields;

    /**
     * A list of all methods within the class.
     */
    private List<JavaMethod> methods;

    public List<String> getImplementsInterfaces() {
        if (implementsInterfaces == null) {
            implementsInterfaces = new ArrayList<>(0);
        }

        return implementsInterfaces;
    }

    public List<JavaVariable> getFields() {
        if (fields == null) {
            fields = new ArrayList<>(0);
        }

        return fields;
    }

    public List<JavaMethod> getMethods() {
        if (methods == null) {
            methods = new ArrayList<>(0);
        }

        return methods;
    }
}
