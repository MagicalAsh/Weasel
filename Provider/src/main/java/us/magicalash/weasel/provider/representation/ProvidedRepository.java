package us.magicalash.weasel.provider.representation;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ProvidedRepository {
    @SerializedName("source")
    JsonArray provided;

    @SerializedName("provided_by")
    String providedBy;
}
