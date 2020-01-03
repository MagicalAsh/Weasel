package us.magicalash.weasel.index.representation;


import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParsedIndexResponse extends IndexingResponse {
    @SerializedName("parsed_results")
    List<JsonObject> parsedResults;
}
