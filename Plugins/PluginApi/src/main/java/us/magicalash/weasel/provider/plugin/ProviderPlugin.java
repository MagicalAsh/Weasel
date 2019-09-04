package us.magicalash.weasel.provider.plugin;

import com.google.gson.JsonArray;
import us.magicalash.weasel.plugin.WeaselPlugin;

public interface ProviderPlugin extends WeaselPlugin {
    /**
     * Determines whether or not this plugin is capable of refreshing this
     * resource.
     * @param name the name to test for refreshing
     * @return     true if the plugin can refresh the source, false otherwise.
     */
    boolean canRefresh(String name);

    /**
     * Refreshes the named resource.
     * @param name the name to refresh
     * @return a json array representing the resources, where each entry is a
     *         single portion of the repo, usually a file.
     */
    JsonArray refresh(String name);
}
