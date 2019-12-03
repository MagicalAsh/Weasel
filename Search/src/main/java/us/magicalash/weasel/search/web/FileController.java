package us.magicalash.weasel.search.web;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import us.magicalash.weasel.search.representation.FileRequestResponse;

import java.io.IOException;

@RestController
@RequestMapping("/file")
public class FileController {
    private final String CONTENT_LOCATION = "content_location";
    private final String BRANCH = "branch_name";

    private final RestHighLevelClient client;

    public FileController(RestHighLevelClient client, Environment env, Gson gson){
        this.client = client;
    }

    @PostMapping("/request")
    public FileRequestResponse getFullFile(@RequestBody JsonObject requestBody) {
        FileRequestResponse response = new FileRequestResponse();
        String id = requestBody.get(CONTENT_LOCATION).getAsString();
        String branch = null;
        if (requestBody.get(BRANCH) != null)
            branch = requestBody.get(BRANCH).getAsString();

        SearchRequest request = makeRequest(id, branch);

        SearchResponse searchResponse;
        try {
            searchResponse = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            response.getMetadata().setResponseCode(500);
            response.getMetadata().setMessage(e.getMessage());
            return response;
        }


        String hitValue = searchResponse.getHits().getAt(0).getSourceAsString();
        JsonElement hit = new JsonParser().parse(hitValue);

        response.setObject(hit.getAsJsonObject());

        return response;
    }

    private SearchRequest makeRequest(String id, String branch) {
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        builder.must(QueryBuilders.matchQuery(CONTENT_LOCATION, id));
        if (branch != null) {
            builder.must(QueryBuilders.matchQuery(BRANCH, branch));
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(builder)
                     .size(1)
                     .terminateAfter(1);

        return new SearchRequest().source(sourceBuilder).indices("raw_file_index");
    }
}
