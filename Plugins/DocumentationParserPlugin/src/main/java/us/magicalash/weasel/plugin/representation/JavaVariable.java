package us.magicalash.weasel.plugin.representation;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A code unit representing a variable type, like a field, parameter, or enum member.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JavaVariable extends JavaCodeUnit {
    /**
     * The type of the field. If this is an enum member, the type is always the enclosing enum.
     */
    private String type;
}