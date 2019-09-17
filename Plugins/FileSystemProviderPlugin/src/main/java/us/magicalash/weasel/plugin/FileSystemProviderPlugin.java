package us.magicalash.weasel.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static us.magicalash.weasel.plugin.GlobMatcher.IGNORED_FILES;

public class FileSystemProviderPlugin implements ProviderPlugin {
    private Properties properties;
    private GlobMatcher matcher;

    @Override
    public String getName() {
        return "File System Provider";
    }

    @Override
    public String[] requestProperties() {
        return new String[]{
                IGNORED_FILES
        };
    }

    @Override
    public void load(Properties properties) {
        this.properties = properties;
        this.matcher = new GlobMatcher(properties);
    }

    @Override
    public boolean canRefresh(String name) {
        // if it's not a file, this isn't the right plugin
        if (!name.startsWith("file://"))
            return false;

        File file = new File(name.replace("file://", ""));
        return file.exists(); // if the file doesn't exist we can't refresh it anyway
    }

    @Override
    public JsonArray refresh(String name) {
        JsonArray out = new JsonArray();
        File root = new File(name.replace("file://", ""));

        recursiveSearch(out, root);

        return out;
    }

    private JsonArray recursiveSearch(JsonArray array, File root){
        // return early if this is ignored.
        if (matcher.matchesAny(root.getName())) {
            return array;
        }

        if (root.isDirectory()) {
            for (File file : Objects.requireNonNull(root.listFiles())) {
                recursiveSearch(array, file);
            }
        } else {
            array.add(parseFile(root));
        }
        return array;
    }

    private JsonObject parseFile(File file) {
        JsonObject fileData = new JsonObject();
        JsonArray lines = new JsonArray();

        try (Scanner fileReader = new Scanner(file)) {
            while (fileReader.hasNext()) {
                lines.add(fileReader.nextLine());
            }
        } catch (FileNotFoundException e) {
            // We read that this file exists, then doesn't. Something weird
            // is going on, but we can probably just ignore it.
        }

        fileData.add("file_contents", lines);
        fileData.addProperty("content_location", file.getAbsolutePath());
        fileData.addProperty("accessed", getTimestamp());
        fileData.addProperty("obtained_by", getName());
        fileData.addProperty("line_count", lines.size());

        return fileData;
    }

    private String getTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        format.setTimeZone(timeZone);
        return format.format(new Date());
    }
}
