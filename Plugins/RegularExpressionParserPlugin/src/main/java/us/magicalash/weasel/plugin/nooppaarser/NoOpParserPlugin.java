package us.magicalash.weasel.plugin.nooppaarser;

import com.google.gson.JsonObject;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
    public void index(JsonObject obj, Consumer<ParsedCodeUnit> onCompletion) {
        ParsedCodeUnit unit = new ParsedCodeUnit();
        unit.setDestinationIndex(INDEX_NAME);

        if (obj.get("content_location") != null)
            unit.setLocation(obj.get("content_location").getAsString());

        if (obj.get("metadata") != null){
            JsonObject metadata = obj.get("metadata").getAsJsonObject();
            Map<String, String> map = new HashMap<>();
            for(String key : metadata.keySet()) {
                map.put(key, metadata.get(key).getAsString());
            }
            unit.setMetadata(map);
        }

        unit.setParsedObject(obj.get("file_contents"));
        unit.setIndexedBy(getName());
        onCompletion.accept(unit);
    }
}
