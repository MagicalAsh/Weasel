package us.magicalash.weasel.test.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.junit.Before;
import org.junit.Test;
import us.magicalash.weasel.plugin.GitProviderPlugin;

public class GitProviderTest {
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private GitProviderPlugin provider;

    @Before
    public void before() {
        this.provider = new GitProviderPlugin();
    }

    @Test
    public void testLoadHttpRepo() {
        JsonArray arr = this.provider.refresh("https://github.com/MagicalAsh/discrete-biostatistics-project.git");

        System.out.println(gson.toJson(arr));
    }

    @Test
    public void testLoadSshRepo() {
        JsonArray arr = this.provider.refresh("git@github.com:MagicalAsh/Weasel.git");

        System.out.println(gson.toJson(arr));
    }
}
