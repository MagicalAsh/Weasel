package us.magicalash.weasel.test;


import com.google.gson.*;
import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.fsprovider.FileSystemConstants;
import us.magicalash.weasel.plugin.fsprovider.FileSystemProviderPlugin;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;

import java.util.ArrayList;
import java.util.List;
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
        List<ProvidedFile> testing = plugin.refresh("file://" + System.getProperty("user.dir") + "/src/");
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

        List<ProvidedFile> array = plugin.refresh("./");
        for (ProvidedFile file : array) {
            String location = file.getFileLocation();
            assertFalse(location.endsWith(".java"));
            System.out.println(gson.toJson(file));
        }
    }
}
