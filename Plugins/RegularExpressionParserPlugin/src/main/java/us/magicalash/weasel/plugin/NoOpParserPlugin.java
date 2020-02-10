package us.magicalash.weasel.plugin;

import com.google.gson.JsonObject;
import us.magicalash.weasel.index.plugin.IndexPlugin;

import java.util.Properties;
import java.util.function.Consumer;

public class NoOpParserPlugin implements IndexPlugin {
    private static final String INDEX_NAME = "raw_file_index";
    @Override
    public String getName() {
        return "No-op Parser Plugin";
    }

    @Override
    public String[] requestProperties() {
        return new String[0];
    }

    @Override
    public void load(Properties properties) {
        // noop
    }

    @Override
    public boolean canIndex(JsonObject obj) {
        return true;
    }

    @Override
    public void index(JsonObject obj, Consumer<JsonObject> onCompletion) {
        obj.addProperty(DESTINATION, INDEX_NAME);
        obj.addProperty(SOURCE_ID, obj.get("content_location").getAsString()); //todo figure out a better source_id
        onCompletion.accept(obj);
    }
}
