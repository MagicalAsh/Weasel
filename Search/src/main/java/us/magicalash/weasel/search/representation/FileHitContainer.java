package us.magicalash.weasel.search.representation;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class FileHitContainer {

    @SerializedName("file_data")
    JsonObject fileData;

    @SerializedName("hit_contexts")
    List<SearchHitContext> contexts;
}
