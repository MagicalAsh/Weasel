package us.magicalash.weasel.search.web;

import com.google.gson.*;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RegexpFlag;
import org.elasticsearch.index.query.RegexpQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import us.magicalash.weasel.representation.ApiMetadata;
import us.magicalash.weasel.search.representation.FileHitContainer;
import us.magicalash.weasel.search.representation.RegExpSearchResponse;
import us.magicalash.weasel.search.representation.SearchHitContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/regex")
public class RegExpSearchController {
    private static final Logger logger = LoggerFactory.getLogger(RegexpQueryBuilder.class);

    @Value("${weasel.search.default_context:5}")
    private int defaultContext;

    private final RestHighLevelClient client;

    public RegExpSearchController(RestHighLevelClient client, Environment env, Gson gson){
        this.client = client;
    }

    @CrossOrigin
    @PostMapping("/search")
    public RegExpSearchResponse search(@RequestBody JsonObject body) {
        RegExpSearchResponse response = new RegExpSearchResponse();

        String regex = body.get("regex").getAsString();
        int context = defaultContext;
        int numHits = -1;
        if (body.get("match_context") != null) {
            context = body.get("match_context").getAsInt();
        }

        if (body.get("max_hits") != null) {
            numHits =  body.get("max_hits").getAsInt();
        }

        response.setRegex(regex);
        SearchRequest request = searchRequest(regex, numHits);
        try {
            int hitCount = 0;
            SearchResponse search = client.search(request, RequestOptions.DEFAULT);
            List<FileHitContainer> array = new ArrayList<>();
            for(SearchHit hit : search.getHits()) {
                String json = hit.getSourceAsString();
                JsonObject source = new JsonParser().parse(json).getAsJsonObject();
                List<Integer> hits = getMatchingLines(hit, source);

                hits.sort(Integer::compareTo);

                // now that we have where the hits are for this particular match, create an object for the matching file
                FileHitContainer file = createHitContexts(hits, source, context);

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

    private SearchRequest searchRequest(String regex, int hits) {
        // .keyword because we need the whole line, not each word
        RegexpQueryBuilder builder = QueryBuilders.regexpQuery("file_contents.keyword", regex);
        builder.flags(RegexpFlag.NONE);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(builder)
                     .size(hits > 0? hits : 10)
                     .terminateAfter(hits > 0? hits : 10)
                     .highlighter(
                             new HighlightBuilder().field("file_contents.keyword")
                                                   .preTags("")
                                                   .postTags("")
                     );
        return new SearchRequest().indices("raw_file_index")
                                  .source(sourceBuilder);
    }

    private List<Integer> getMatchingLines(SearchHit hit, JsonObject source){
        List<Integer> hits = new ArrayList<>();
        for(Text textMatch : hit.getHighlightFields().get("file_contents.keyword").getFragments()) {
            int i = 1;
            for (JsonElement line : source.getAsJsonArray("file_contents")) {
                if (line.getAsString().contains(textMatch.toString()) && !hits.contains(i)) {
                    hits.add(i);
                }
                i++;
            }
        }

        return hits;
    }

    private FileHitContainer createHitContexts(List<Integer> hits, JsonObject source, int context) {
        JsonArray contents = source.remove("file_contents").getAsJsonArray();
        FileHitContainer file = new FileHitContainer();
        List<SearchHitContext> contexts = new ArrayList<>();
        for (int hitNum = 0; hitNum < hits.size(); hitNum++) {
            SearchHitContext hitContext = new SearchHitContext();
            int lineHit = hits.get(hitNum);
            JsonArray hitNums = new JsonArray();
            List<String> lines = new ArrayList<>();

            // - 1 to offset the matching line
            int start = Math.max(lineHit - context - 1, 0);
            int end = Math.min(lineHit + context - 1, contents.size() - 1);

            // if the next match has overlapping context, merge them
            hitNums.add(lineHit);
            while (hitNum + 1 < hits.size() && (hits.get(hitNum + 1) - lineHit) < 2*context) {
                hitNum++;
                lineHit = hits.get(hitNum);
                end = Math.min(lineHit + context  - 1, contents.size() - 1);
                hitNums.add(lineHit);
            }

            for (int i = start; i <= end; i++) {
                lines.add(contents.get(i).getAsString());
            }

            hitContext.setLines(lines);
            hitContext.setMatches(hits);
            hitContext.setStartingLine(start);
            hitContext.setEndingLine(end);

            contexts.add(hitContext);
        }

        file.setContexts(contexts);
        file.setFileData(source);

        return file;
    }
}
