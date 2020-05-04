package us.magicalash.weasel.index.plugin.representations;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class ParsedCodeUnit {
    /**
     * The index to store this unit within.
     */
    private transient String destinationIndex;

    /**
     * The ID of the file within the index.
     */
    private transient String indexId;

    /**
     * The location the file was obtained from that this unit was parsed from.
     */
    @SerializedName("file_location")
    private String location;

    /**
     * The tool this unit was parsed by.
     */
    @SerializedName("indexed_by")
    private String indexedBy;

    /**
     * The parsed object result.
     */
    @SerializedName("parsed_result")
    private Object parsedObject;

    /**
     * Additional metadata about this code unit, or the file it was
     * created from.
     */
    @SerializedName("metadata")
    private Map<String, String> metadata;
}
