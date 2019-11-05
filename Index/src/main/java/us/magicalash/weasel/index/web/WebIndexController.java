package us.magicalash.weasel.index.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.IndexPluginLoader;
import us.magicalash.weasel.plugin.PluginTask;

import java.io.IOException;
import java.nio.charset.Charset;

import static us.magicalash.weasel.index.plugin.IndexPlugin.DESTINATION;
import static us.magicalash.weasel.index.plugin.IndexPlugin.SOURCE_ID;

@RestController
public class WebIndexController {
    private static final Logger logger = LoggerFactory.getLogger(WebIndexController.class);

    private final RestHighLevelClient restClient;
    private final IndexPluginLoader pluginLoader;

    public WebIndexController(RestHighLevelClient restClient, IndexPluginLoader pluginLoader) {
        this.restClient = restClient;
        this.pluginLoader = pluginLoader;
    }

    @PostMapping("/index")
    public JsonObject index(@RequestBody JsonObject body) {
        JsonObject response = new JsonObject();
        JsonArray scheduled = new JsonArray();

        for (IndexPlugin plugin : pluginLoader.getApplicablePlugins(body)) {
            scheduled.add(plugin.getName());
            PluginTask<JsonObject> task = new PluginTask<>();
            task.setPluginName(plugin.getName());
            task.setTask(() -> {
                try {
                    JsonObject result = plugin.index(body);
                    restClient.index(buildQuery(result), RequestOptions.DEFAULT);

                    return result;
                } catch (IOException | IllegalArgumentException e) {
                    logger.error("Errored while processing index request.", e);
                }

                return null;
            });


        }

        response.addProperty("status", "success");
        response.add("scheduled_for", scheduled);
        return response;
    }

    @PostMapping("/dry_run")
    public JsonObject dryRun(@RequestBody JsonObject body) {
        JsonObject response = new JsonObject();
        JsonArray pluginResults = new JsonArray();

        for (IndexPlugin plugin : pluginLoader.getApplicablePlugins(body)) {
            try {
                JsonObject result = plugin.index(body);
                JsonObject pluginResult = new JsonObject();

                pluginResult.addProperty("plugin_name", plugin.getName());
                pluginResult.add("result", result);

                pluginResults.add(pluginResult);
            } catch (IllegalArgumentException e) {
                response.addProperty("status", "failed");
                response.addProperty("reason", e.getMessage());

                logger.error("", e);
                throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Malformed Request",
                        new HttpHeaders(), response.toString().getBytes(), Charset.defaultCharset());
            }
        }

        response.addProperty("status", "success");
        response.add("values", pluginResults);
        return response;
    }

    private IndexRequest buildQuery(JsonObject object) {
        return new IndexRequest().id(object.remove(SOURCE_ID).getAsString())
                                 .index(object.remove(DESTINATION).getAsString())
                                 .source(object.toString(), XContentType.JSON);
    }
}
