package us.magicalash.weasel.test.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import us.magicalash.weasel.plugin.GitConstants;
import us.magicalash.weasel.plugin.GitProviderPlugin;

import java.io.File;
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
        String fileName;
        try {
            File temp = Files.createTempDirectory("temp").toFile();
            Git.cloneRepository()
                    .setURI("https://github.com/MagicalAsh/discrete-biostatistics-project.git")
                    .setDirectory(temp)
                    .call();

            fileName = temp.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Something went wrong setting up for testLoadLocalRepo()", e);
        } catch (GitAPIException e) {
            throw new RuntimeException("Something went wrong setting git up for testLoadLocalRepo()", e);
        }

        JsonArray arr = this.provider.refresh(fileName);

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }

    @Test
    public void testLoadHttpRepo() {
        JsonArray arr = this.provider.refresh("https://github.com/MagicalAsh/discrete-biostatistics-project.git");

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }

    @Ignore("Requires ssh key set up, which may not be set up.")
    @Test
    public void testLoadSshRepo() {
        JsonArray arr = this.provider.refresh("git@github.com:MagicalAsh/Weasel.git");

        assertTrue(arr.size() > 0);

        System.out.println(gson.toJson(arr));
    }
}
