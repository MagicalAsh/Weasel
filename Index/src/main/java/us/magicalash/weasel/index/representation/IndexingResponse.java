package us.magicalash.weasel.index.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import us.magicalash.weasel.representation.AbstractResponse;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IndexingResponse extends AbstractResponse {
    @SerializedName("processed_by")
    List<String> processedBy;
}
