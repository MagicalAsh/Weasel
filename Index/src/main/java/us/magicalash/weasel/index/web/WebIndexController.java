package us.magicalash.weasel.index.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.IndexPluginLoader;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.index.representation.IndexingResponse;
import us.magicalash.weasel.index.representation.ParsedIndexResponse;
import us.magicalash.weasel.plugin.PluginTask;
import us.magicalash.weasel.plugin.PluginTaskService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static us.magicalash.weasel.index.plugin.IndexPlugin.DESTINATION;
import static us.magicalash.weasel.index.plugin.IndexPlugin.SOURCE_ID;

@RestController
public class WebIndexController {
    private static final Logger logger = LoggerFactory.getLogger(WebIndexController.class);

    private final Gson gson;
    private final RestHighLevelClient restClient;
    private final IndexPluginLoader pluginLoader;
    private final PluginTaskService taskService;

    public WebIndexController(RestHighLevelClient restClient, IndexPluginLoader pluginLoader, PluginTaskService service,
                              Gson gson) {
        this.restClient = restClient;
        this.pluginLoader = pluginLoader;
        this.taskService = service;
        this.gson = gson;
    }

    @PostMapping("/index")
    public IndexingResponse index(@RequestBody JsonObject body) {
        IndexingResponse response = new IndexingResponse();
        List<String> scheduled = new ArrayList<>();

        for (IndexPlugin plugin : pluginLoader.getApplicablePlugins(body)) {
            scheduled.add(plugin.getName());
            // todo make a way to request failure messages
            PluginTask<JsonObject> task = PluginTask.<JsonObject>builder()
                .pluginName(plugin.getName())
                .task(() -> {
                    plugin.index(body, codeUnit -> {
                        try {
                            restClient.index(buildQuery(codeUnit), RequestOptions.DEFAULT);
                        } catch (IOException e) {
                            logger.warn("Failed to index response!", e);
                        }
                    });

                    return null;
                })
                .build();

            taskService.submit(task);
        }

        response.getMetadata().setStatus("scheduled");
        response.setProcessedBy(scheduled);
        return response;
    }

    @PostMapping("/dry_run")
    public ParsedIndexResponse dryRun(@RequestBody JsonObject body) {
        ParsedIndexResponse response = new ParsedIndexResponse();
        List<JsonObject> pluginResults = new ArrayList<>();
        List<String> processed = new ArrayList<>();

        for (IndexPlugin plugin : pluginLoader.getApplicablePlugins(body)) {
            try {
                JsonArray result = (JsonArray) gson.toJsonTree(plugin.index(body));
                JsonObject pluginResult = new JsonObject();

                pluginResult.addProperty("plugin_name", plugin.getName());
                pluginResult.add("result", result);

                pluginResults.add(pluginResult);

                processed.add(plugin.getName());
            } catch (IllegalArgumentException e) {
                response.getMetadata().setStatus("failed");
                response.getMetadata().setMessage(e.getMessage());
                response.getMetadata().setResponseCode(HttpServletResponse.SC_BAD_REQUEST);

                return response;
            }
        }

        response.getMetadata().setStatus("success");
        response.setParsedResults(pluginResults);
        response.setProcessedBy(processed);
        return response;
    }

    private IndexRequest buildQuery(ParsedCodeUnit object) {
        return new IndexRequest().id(object.getIndexId())
                                 .index(object.getDestinationIndex())
                                 .source(gson.toJson(object), XContentType.JSON);
    }
}
