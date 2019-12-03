package us.magicalash.weasel.provider.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import us.magicalash.weasel.representation.AbstractResponse;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderResponse extends AbstractResponse {
    @SerializedName("provided_by")
    List<String> by;
}
