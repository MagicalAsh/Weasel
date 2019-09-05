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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.IndexPluginLoader;

import java.io.IOException;
import java.nio.charset.Charset;

import static us.magicalash.weasel.index.plugin.IndexPlugin.DESTINATION;
import static us.magicalash.weasel.index.plugin.IndexPlugin.SOURCE_ID;

@RestController
@RequestMapping("/index/index")
public class WebIndexController {
    private static final Logger logger = LoggerFactory.getLogger(WebIndexController.class);

    private final RestHighLevelClient restClient;
    private final IndexPluginLoader pluginLoader;

    public WebIndexController(RestHighLevelClient restClient, IndexPluginLoader pluginLoader) {
        this.restClient = restClient;
        this.pluginLoader = pluginLoader;
    }

    @PostMapping
    public JsonObject index(@RequestBody JsonObject body) {
        JsonObject response = new JsonObject();
        response.add("processed_by", new JsonArray());

        for (IndexPlugin plugin : pluginLoader.getCapableLoadedPlugins(body)) {
            try {
                JsonObject result = plugin.index(body);
                restClient.index(buildQuery(result), RequestOptions.DEFAULT);

                response.getAsJsonArray("processed_by")
                        .add(plugin.getName());
            } catch (IOException | IllegalArgumentException e) {
                response.addProperty("status", "failed");
                response.addProperty("reason", e.getMessage());

                logger.error("", e);

                if (e instanceof IOException)
                    throw HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Processing Failed",
                            new HttpHeaders(), response.toString().getBytes(), Charset.defaultCharset());
                else if (e instanceof IllegalArgumentException)
                    throw HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Malformed Request",
                            new HttpHeaders(), response.toString().getBytes(), Charset.defaultCharset());
            }
        }

        response.addProperty("status", "success");
        return response;
    }

    private IndexRequest buildQuery(JsonObject object) {
        return new IndexRequest().id(object.remove(SOURCE_ID).getAsString())
                                 .index(object.remove(DESTINATION).getAsString())
                                 .source(object.toString(), XContentType.JSON);
    }
}
