package us.magicalash.weasel.search.web;

import com.google.gson.*;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import us.magicalash.weasel.representation.ApiMetadata;
import us.magicalash.weasel.search.representation.FileHitContainer;
import us.magicalash.weasel.search.representation.SearchHitContext;
import us.magicalash.weasel.search.representation.StructuralSearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/structural")
public class StructuralSearchController {

    private final RestHighLevelClient client;

    @Autowired
    public StructuralSearchController(RestHighLevelClient client) {
        this.client = client;
    }


    @CrossOrigin
    @PostMapping("/search")
    public StructuralSearchResponse search(@RequestBody JsonObject body) {
        StructuralSearchResponse response = new StructuralSearchResponse();

        int numHits = -1;
        if (body.get("max_hits") != null) {
            numHits =  body.get("max_hits").getAsInt();
        }

        SearchRequest request = searchRequest(body, numHits);
        try {
            int hitCount = 0;
            SearchResponse search = client.search(request, RequestOptions.DEFAULT);
            List<FileHitContainer> array = new ArrayList<>();
            for(SearchHit hit : search.getHits()) {
                String json = hit.getSourceAsString();
                JsonObject source = new JsonParser().parse(json).getAsJsonObject();

                FileHitContainer file = createHitContexts(source);

                array.add(file);

                hitCount++;
            }

            response.setHits(array);
            response.setHitCount(hitCount);
        } catch (IOException e) {
            ApiMetadata metadata = response.getMetadata();
            metadata.setResponseCode(500);
            metadata.setMessage(e.getMessage());
        } catch (ElasticsearchStatusException e) {
            ApiMetadata metadata = response.getMetadata();
            metadata.setMessage(e.getMessage());
            Throwable cause = e.getCause();
            if (cause instanceof ResponseException) {
                metadata.setResponseCode(((ResponseException) cause).getResponse().getStatusLine().getStatusCode());
            } else {
                metadata.setResponseCode(400);
            }
        }

        return response;
    }

    private FileHitContainer createHitContexts(JsonObject source) throws IOException {
        FileHitContainer container = new FileHitContainer();
        SearchHitContext context = new SearchHitContext();

        JsonObject parsedResult = source.get("parsed_result").getAsJsonObject();
        int startingLine = parsedResult.get("start_line").getAsInt();
        int endingLine = parsedResult.get("end_line").getAsInt();
        context.setStartingLine(startingLine);
        context.setEndingLine(endingLine);

        String fileName = source.get("file_location").getAsString();
        List<String> lines = getLines(fileName, startingLine, endingLine);
        context.setLines(lines);

        JsonObject fileData = new JsonObject();
        fileData.addProperty("file_location", fileName);
        container.setFileData(fileData);

        // since we're not highlighting specific lines, don't include matches.
        context.setMatches(Collections.emptyList());

        container.setContexts(Collections.singletonList(context));
        return container;
    }

    private List<String> getLines(String fileName, int start, int end) throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("file_location.keyword", fileName))
                .size(1)
                .terminateAfter(1);
        SearchResponse response = client.search(new SearchRequest().indices("raw_file_index").source(sourceBuilder),
                RequestOptions.DEFAULT);

        JsonObject object = new JsonParser()
                .parse(response.getHits().getAt(0).getSourceAsString())
                .getAsJsonObject();

        List<String> lines = new ArrayList<>();
        for (int i = start; 0 < i && i <= end; i++) {
            lines.add(object.getAsJsonArray("parsed_result").get(i - 1).getAsString());
        }

        return lines;
    }

    private SearchRequest searchRequest(JsonObject request, int maxHits) {
        BoolQueryBuilder builder = QueryBuilders.boolQuery();

        fixQualifiedNames(request, "extends");
        fixQualifiedNames(request, "interfaces");

        // these don't get keyword as they are already keyword types.
        addListQueryToBuilder(builder, request, "extends", "parsed_result.parentClass");
        addListQueryToBuilder(builder, request, "interfaces", "parsed_result.implementsInterfaces");
        addListQueryToBuilder(builder, request, "modifiers", "parsed_result.modifiers");

        // these two get keyword since I apparently didn't specify them in the schema at all. Oops.
        addNestedQueryToBuilder(builder, request, "fields", "parsed_result.fields");
        addNestedQueryToBuilder(builder, request, "methods", "parsed_result.methods");

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(builder)
                .size(maxHits > 0? maxHits : 10);

        System.out.println(request);
        System.out.println(builder);
        return new SearchRequest().indices("parsed_java")
                .source(sourceBuilder);
    }

    /**
     * Fixes qualified names to use the database standard rather than standard java.
     * See notes in the documentation parser for the justification behind this.
     * @param object object to fix
     * @param key    key to fix
     */
    private void fixQualifiedNames(JsonObject object, String key) {
        if (object.get(key) == null) {
            return;
        }
        JsonArray array = new JsonArray();
        for (JsonElement element : object.remove(key).getAsJsonArray()) {
            array.add(element.getAsString().replaceAll("\\.", "/"));
        }

        object.add(key, array);
    }

    private void addListQueryToBuilder(BoolQueryBuilder builder, JsonObject request, String elementName, String dbName) {
        if (request.getAsJsonArray(elementName) != null) {
            // elasticsearch doesn't have a nice "match any of these", so we add each match to
            // the should clause. Elements with higher scores will float to the top, meaning
            // that they should match as many as possible. Switching the query type to must
            // requires that all match.
            for (JsonElement element : request.getAsJsonArray(elementName)) {
                // don't include empty searches, in case they're inputted.
                if (!element.getAsString().equals("")) {
                    // if the query type is "should", then match any. otherwise require all
                    if (request.get("type") != null && request.get("type").getAsString().equals("should")) {
                        builder.should(QueryBuilders.matchQuery(dbName, element.getAsString()));
                    } else {
                        builder.must(QueryBuilders.matchQuery(dbName, element.getAsString()));
                    }
                }
            }
        }
    }

    private void addNestedQueryToBuilder(BoolQueryBuilder builder, JsonObject request, String elementName, String dbNamePrefix) {

        if (request.getAsJsonArray(elementName) != null) {
            for (JsonElement object : request.getAsJsonArray(elementName)) {
                BoolQueryBuilder innerQueryBuilder = QueryBuilders.boolQuery();
                for (String key : object.getAsJsonObject().keySet()) {
                    if (!object.getAsJsonObject().get(key).getAsString().equals("")) {
                        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery(
                                dbNamePrefix + "." + key + ".keyword",
                                object.getAsJsonObject().get(key).getAsString()
                        );

                        if (request.get("type") != null && request.get("type").getAsString().equals("should")) {
                            innerQueryBuilder.should(queryBuilder);
                        } else {
                            innerQueryBuilder.must(queryBuilder);
                        }
                    }
                }

                if (request.get("type") != null && request.get("type").getAsString().equals("should")) {
                    builder.should(QueryBuilders.nestedQuery(dbNamePrefix, innerQueryBuilder, ScoreMode.Max));
                } else {
                    builder.must(QueryBuilders.nestedQuery(dbNamePrefix, innerQueryBuilder, ScoreMode.Max));
                }
            }
        }
    }
}
