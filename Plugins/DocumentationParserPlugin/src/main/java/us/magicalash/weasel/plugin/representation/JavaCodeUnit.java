package us.magicalash.weasel.plugin.representation;

import lombok.Data;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;

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
    private List<String> modifiers;

    /**
     * A list of all annotations applied to this code unit. The key is the annotation name, the value is either a map
     * of the annotation element pairs, a list of map of the annotation element pairs (if the annotation is repeated),
     * the string value of the single parameter, or the empty string if no parameters are passed.
     */
    private Map<String, Map<String, String>> annotations;

    public Map<String, Map<String, String>> getAnnotations() {
        if (annotations == null) {
            annotations = new HashMap<>(); // if this gets called we definitely have an annotation
        }
        return annotations;
    }

    public List<String> getModifiers(){
        if (modifiers == null) {
            modifiers = new ArrayList<>(0);
        }

        return modifiers;
    }
}
