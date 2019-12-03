package us.magicalash.weasel.search.representation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileRequestResponse extends SearchResponse {
    @SerializedName("file")
    JsonObject object;
}
