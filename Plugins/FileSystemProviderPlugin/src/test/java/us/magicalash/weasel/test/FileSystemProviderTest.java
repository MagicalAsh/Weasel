package us.magicalash.weasel.test;


import com.google.gson.*;
import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.FileSystemConstants;
import us.magicalash.weasel.plugin.FileSystemProviderPlugin;

import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileSystemProviderTest {

    private FileSystemProviderPlugin plugin;
    private Gson gson;

    @Before
    public void newProvider() {
        plugin = new FileSystemProviderPlugin();
        plugin.load(new Properties());

        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Test
    public void testCanRefresh() {
        assertFalse(plugin.canRefresh("git@github.com:AdoptOpenJDK/openjdk-jdk11.git"));
        assertTrue(plugin.canRefresh("file://" + System.getProperty("user.dir")));
    }

    @Test
    public void testRefresh() {
        JsonArray testing = plugin.refresh("file://" + System.getProperty("user.dir") + "/src/");
        System.out.println(gson.toJson(testing));
        assertTrue(testing.size() > 0);
    }

    @Test
    public void testGlobs() {
        Properties p = new Properties();
        ArrayList<String> ignored = new ArrayList<>();
        ignored.add("*.java");
        p.put(FileSystemConstants.IGNORED_FILES, ignored);
        plugin.load(p);

        JsonArray array = plugin.refresh("./");
        for (JsonElement j : array) {
            if (j instanceof JsonObject) {
                JsonObject file = (JsonObject) j;
                String location = file.get("content_location").getAsString();
                assertFalse(location.endsWith(".java"));
                System.out.println(gson.toJson(j));
            }
        }
    }
}
