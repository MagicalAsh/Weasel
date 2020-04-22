package us.magicalash.weasel.plugin.docparser.representation;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing the documentation associated with a Java code unit.
 */
@Data
public class JavaDocumentation {
    /**
     * The main body of the documentation.
     */
    private String body;

    /**
     * A list of all tags within the Javadoc comment.
     */
    private Map<String, List<?>> tags;

    public Map<String, List<?>> getTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }

        return tags;
    }
}
