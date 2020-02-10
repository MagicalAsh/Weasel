package us.magicalash.weasel.plugin.representation;

import lombok.Data;

import java.util.*;

/**
 * A parent for commonalities between different code units.
 */
@Data
public class JavaCodeUnit {
    /**
     * The documentation associated with this code unit.
     */
    private JavaDocumentation documentation;


    /**
     * The name of this code unit.
     */
    private String name;

    /**
     * A list of all language-level modifiers that apply to this code unit.
     */
    private List<String> modifiers = new ArrayList<>(0);

    /**
     * A list of all annotations applied to this code unit. The key is the annotation name, the value is either a map
     * of the annotation element pairs, a list of map of the annotation element pairs (if the annotation is repeated),
     * the string value of the single parameter, or the empty string if no parameters are passed.
     */
    private Map<String, Object> annotations = new HashMap<>(0);
}
