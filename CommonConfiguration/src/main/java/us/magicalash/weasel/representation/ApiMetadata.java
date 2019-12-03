package us.magicalash.weasel.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ApiMetadata {
    @SerializedName("status")
    private int responseCode = 200; // 200 OK

    @SerializedName("status_type")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("path")
    private String endpoint;
}
