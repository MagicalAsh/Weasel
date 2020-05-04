package us.magicalash.weasel.provider.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import us.magicalash.weasel.plugin.WeaselPlugin;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface ProviderPlugin extends WeaselPlugin {
    /**
     * Determines whether or not this plugin is capable of refreshing this
     * resource.
     *
     * @param name the name to test for refreshing
     * @return true if the plugin can refresh the source, false otherwise.
     */
    boolean canRefresh(String name);

    /**
     * Refreshes the named resource.
     *
     * @param name the name to refresh
     * @return a json array representing the resources, where each entry is a
     * single portion of the repo, usually a file.
     */
    default List<ProvidedFile> refresh(String name) {
        ArrayList<ProvidedFile> array = new ArrayList<>();
        refresh(name, array::add);
        return array;
    };

    /**
     * Refreshes the named resource.
     * @param name                  the name of the resource to refresh
     * @param onElementCompleted    the action to take on every produced resource
     */
    void refresh(String name, Consumer<ProvidedFile> onElementCompleted);
}