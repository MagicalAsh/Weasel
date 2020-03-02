package us.magicalash.weasel.provider.plugin.representations;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProvidedFile {
    @SerializedName("metadata")
    private Map<String, String> metadata;

    @SerializedName("obtained_by")
    private String obtainedBy;

    @SerializedName("accessed")
    private String accessedAt;

    @SerializedName("content_location")
    private String fileLocation;

    @SerializedName("file_contents")
    private List<String> lines;
}
