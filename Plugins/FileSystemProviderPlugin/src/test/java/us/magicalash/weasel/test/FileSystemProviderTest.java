package us.magicalash.weasel.test;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.FileSystemProviderPlugin;

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
}
