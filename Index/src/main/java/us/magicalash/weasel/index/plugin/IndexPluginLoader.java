package us.magicalash.weasel.index.plugin;

import com.google.gson.JsonObject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.PluginLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class IndexPluginLoader extends PluginLoader<IndexPlugin> {
    private static final String HIERARCHY_REGEX = "hierarchy\\((.+)\\):(.+)";
    private static final Logger logger = LoggerFactory.getLogger(IndexPluginLoader.class);

    @Value("${weasel.plugin.failOnSpecialRequestException:false}")
    private boolean failOnHierarchyException;

    @Value("${weasel.plugin.scrollKeepalive:10m}")
    private String scrollLength;

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    public IndexPluginLoader(Environment environment) {
        super(IndexPlugin.class, environment);

        // fits hierarchy(indexName).field and builds a package hierarchy out of the
        // elasticsearch entries under parsed_result. This is probably pretty slow.
        super.specialRequestProperties.put(HIERARCHY_REGEX, this::buildHierarchy);
    }

    public List<IndexPlugin> getApplicablePlugins(Object test) {
        if (test instanceof JsonObject) {
            JsonObject indexable = (JsonObject) test;
            return this.getLoadedPlugins().stream().filter(p -> p.canIndex(indexable)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private PackageHierarchy buildHierarchy(IndexPlugin plugin, String propertyName) {
        Pattern pattern = Pattern.compile(HIERARCHY_REGEX);
        Matcher matcher = pattern.matcher(propertyName);

        if (!matcher.matches()) {
            throw new RuntimeException("Regular Expression Matches but no groups match. This should not happen.");
        }

        String index = matcher.group(1);
        String fieldName = matcher.group(2);

        PackageHierarchy hierarchy = new PackageHierarchy();
        try {
            SearchRequest request = matchAll(index, fieldName);
            SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);

            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();

            while (searchHits != null && searchHits.length > 0) {
                processHits(hierarchy, fieldName, searchHits);

                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(request.scroll());
                searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();
            }
        } catch (IOException e) {
            // we can't recover, but we also shouldn't crash everything if a special request failed.
            if (failOnHierarchyException) {
                logger.error("Failed to parse hierarchy for plugin " + plugin.getName() + ", failed with exception", e);
                logger.error("Raising RuntimeException to prevent loading from finishing...");
                throw new RuntimeException(e);
            } else {
                logger.error("Failed to parse hierarchy for plugin " + plugin.getName() + ", failed with exception", e);
            }
        }

        return hierarchy;
    }

    private SearchRequest matchAll(String index, String fieldName) {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchAllQuery())
               .fetchSource("parsed_result." + fieldName, null)
               .size(1000);
        return new SearchRequest().indices(index).source(builder).scroll(scrollLength);
    }

    @SuppressWarnings("unchecked")
    private void processHits(PackageHierarchy hierarchy, String fieldName, SearchHit[] searchHits){
        for (SearchHit hit : searchHits) {
            Map<String, Object> object = hit.getSourceAsMap();
            object = (Map<String, Object>) object.get("parsed_result");
            Object field = object.get(fieldName);

            if (field == null) {
                System.out.println("object doesn't have field");
                return;
            }
            hierarchy.addType(field.toString());
        }
    }
}
