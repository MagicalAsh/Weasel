package us.magicalash.weasel.plugin.representation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * A code unit representing a method-like object, including methods and constructors.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JavaMethod extends JavaCodeUnit {
    /**
     * The return type of this method.
     */
    private String returnType;

    /**
     * A list of the parameters of the method.
     */
    private List<JavaVariable> parameters = new ArrayList<>(0);
}
