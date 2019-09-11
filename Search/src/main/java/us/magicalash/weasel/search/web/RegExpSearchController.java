package us.magicalash.weasel.search.web;

import com.google.gson.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RegexpFlag;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/regex")
public class RegExpSearchController {
    private final Environment environment;
    private final RestHighLevelClient client;
    private final Gson gson;

    public RegExpSearchController(RestHighLevelClient client, Environment env, Gson gson){
        this.client = client;
        this.environment = env;
        this.gson = gson;
    }

    @CrossOrigin
    @PostMapping("/search")
    public JsonObject search(@RequestBody JsonObject body) {
        JsonObject response = new JsonObject();

        String regex = body.get("regex").getAsString();
        response.addProperty("regex", regex);
        SearchRequest request = searchRequest(regex);

        try {
            int hitCount = 0;
            SearchResponse search = client.search(request, RequestOptions.DEFAULT);
            JsonArray array = new JsonArray();
            for(SearchHit hit : search.getHits()) {
                String json = hit.getSourceAsString();
                JsonElement source = new JsonParser().parse(json).getAsJsonObject();
                array.add(source);
                hitCount++;
            }
            response.add("hits", array);
            response.addProperty("hit_count", hitCount);
        } catch (IOException e) {
            response.addProperty("status", "failed");
            response.addProperty("response", e.getMessage());
        }

        response.addProperty("status", "success");
        return response;
    }

    private SearchRequest searchRequest(String regex) {
        // .keyword because we need the whole line, not each word
        RegexpQueryBuilder builder = QueryBuilders.regexpQuery("file_contents.keyword", regex);
        builder.flags(RegexpFlag.NONE);
        return new SearchRequest().indices("raw_file_index").source(new SearchSourceBuilder().query(builder).size(50));
    }

}
