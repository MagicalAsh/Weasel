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
    private List<String> implementsInterfaces = new ArrayList<>(0);

    /**
     * A list of all fields within the class.
     */
    private List<JavaVariable> fields = new ArrayList<>(0);

    /**
     * A list of all methods within the class.
     */
    private List<JavaMethod> methods = new ArrayList<>(0);
}
