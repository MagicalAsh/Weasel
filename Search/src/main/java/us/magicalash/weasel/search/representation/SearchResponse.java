package us.magicalash.weasel.search.representation;


import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import us.magicalash.weasel.representation.AbstractResponse;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResponse extends AbstractResponse {

    @SerializedName("took")
    private long took;

    @SerializedName("hit_count")
    private int hitCount;

}
