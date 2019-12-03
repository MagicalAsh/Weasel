package us.magicalash.weasel.search.representation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SearchHitContext {
    @SerializedName("lines")
    List<String> lines;

    @SerializedName("matches")
    List<Integer> matches;

    @SerializedName("line_start")
    int startingLine;

    @SerializedName("line_end")
    int endingLine;
}
