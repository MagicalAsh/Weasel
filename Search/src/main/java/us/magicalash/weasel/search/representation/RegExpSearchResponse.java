package us.magicalash.weasel.search.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegExpSearchResponse extends SearchResponse {
    @SerializedName("regex")
    private String regex;


    @SerializedName("hits")
    private List<FileHitContainer> hits;
}
