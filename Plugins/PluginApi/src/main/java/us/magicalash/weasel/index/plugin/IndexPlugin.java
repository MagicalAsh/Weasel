package us.magicalash.weasel.index.plugin;

import com.google.gson.JsonObject;
import us.magicalash.weasel.plugin.WeaselPlugin;

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
    JsonObject index(JsonObject obj);
}
