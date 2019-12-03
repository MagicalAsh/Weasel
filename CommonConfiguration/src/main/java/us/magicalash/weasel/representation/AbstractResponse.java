package us.magicalash.weasel.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AbstractResponse {
    @SerializedName("metadata")
    protected ApiMetadata metadata = new ApiMetadata();
}
