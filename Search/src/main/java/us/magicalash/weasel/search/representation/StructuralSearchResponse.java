package us.magicalash.weasel.search.representation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StructuralSearchResponse extends SearchResponse {
    @SerializedName("query")
    private JsonObject query;

    @SerializedName("hits")
    private List<FileHitContainer> hits;
}
