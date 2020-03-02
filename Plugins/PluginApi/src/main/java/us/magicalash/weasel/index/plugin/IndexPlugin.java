package us.magicalash.weasel.index.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.WeaselPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface IndexPlugin extends WeaselPlugin {
    String SOURCE_ID = "source_id";
    String DESTINATION = "destination_index";

    /**
     * Determines if this plugin is capable of indexing the provided object.
     *
     * @param obj an object representing something to be indexed
     * @return true if this object can be indexed by this plugin, false otherwise
     */
    boolean canIndex(JsonObject obj);

    /**
     * Indexes the provided object to a serializable object.
     *
     * In addition to the indexed form of the provided object,
     * the returned JsonObject should also contain the key names
     * given by SOURCE_ID and DESTINATION. These indicate the id
     * of the source, and the destination index of the indexed
     * code.
     *
     * @param obj the object to be indexed
     * @return the indexed form of the provided object
     */
    default List<ParsedCodeUnit> index(JsonObject obj) {
        List<ParsedCodeUnit> array = new ArrayList<>();
        index(obj, array::add);
        return array;
    };

    /**
     * Indexes the provided object into a serializable object.
     *
     * This method allows for parsing sub-objects and returning a
     * series of objects, rather than one all at once. Additionally,
     * this allows for plugins parsing sub-objects to properly parallelize
     * output of multiple objects.
     *
     * @param obj           the object to be indexed
     * @param onCompletion  a consumer reacting to a produced object. This
     *                      parameter should be thread-safe.
     */
    void index(JsonObject obj, Consumer<ParsedCodeUnit> onCompletion);
}
