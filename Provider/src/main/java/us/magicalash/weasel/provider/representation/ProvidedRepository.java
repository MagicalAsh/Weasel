package us.magicalash.weasel.provider.representation;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;

import java.util.List;

@Data
public class ProvidedRepository {
    @SerializedName("source")
    List<ProvidedFile> provided;

    @SerializedName("provided_by")
    String providedBy;
}
