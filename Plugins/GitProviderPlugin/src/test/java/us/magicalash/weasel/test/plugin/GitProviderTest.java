package us.magicalash.weasel.test.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.GitConstants;
import us.magicalash.weasel.plugin.GitProviderPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class GitProviderTest {
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private GitProviderPlugin provider;
    private Path workingDir;

    @Before
    public void before() {
        this.provider = new GitProviderPlugin();
        Properties properties = new Properties();
        try {
            this.workingDir = Files.createTempDirectory("tmp");
            properties.put(GitConstants.TEMP_DIR, workingDir.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp. directory.");
        }

        this.provider.load(properties);
    }

    @Test
    public void testLoadLocalRepo() {
        JsonArray arr = this.provider.refresh("/home/wes/Weasel");

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }

    @Test
    public void testLoadHttpRepo() {
        JsonArray arr = this.provider.refresh("https://github.com/MagicalAsh/discrete-biostatistics-project.git");

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }

    @Test
    public void testLoadSshRepo() {
        JsonArray arr = this.provider.refresh("git@github.com:MagicalAsh/Weasel.git");

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }
}
