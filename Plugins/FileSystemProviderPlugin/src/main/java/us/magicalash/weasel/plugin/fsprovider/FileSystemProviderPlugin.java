package us.magicalash.weasel.plugin.fsprovider;

import us.magicalash.weasel.plugin.GlobMatcher;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

import static us.magicalash.weasel.plugin.fsprovider.FileSystemConstants.IGNORED_FILES;

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
    @SuppressWarnings("unchecked")
    public void load(Properties properties) {
        this.properties = properties;
        this.matcher = new GlobMatcher(null, (List<String>) properties.get(IGNORED_FILES));
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
    public void refresh(String name, Consumer<ProvidedFile> onProduce) {
        File root = new File(name.replace("file://", ""));

        recursiveSearch(onProduce, root);
    }

    private void recursiveSearch(Consumer<ProvidedFile> onFileGen, File root){
        // return early if this is ignored.
        if (matcher.isBlacklisted(root.getName())) {
            return;
        }

        if (root.isDirectory()) {
            for (File file : Objects.requireNonNull(root.listFiles())) {
                recursiveSearch(onFileGen, file);
            }
        } else {
            onFileGen.accept(parseFile(root));
        }
        return;
    }

    private ProvidedFile parseFile(File file) {
        ProvidedFile out = new ProvidedFile();
        ArrayList<String> lines = new ArrayList<>();

        try (Scanner fileReader = new Scanner(file)) {
            while (fileReader.hasNext()) {
                lines.add(fileReader.nextLine());
            }
        } catch (FileNotFoundException e) {
            // We read that this file exists, then doesn't. Something weird
            // is going on, but we can probably just ignore it.
        }

        out.setFileLocation(file.getAbsolutePath());
        out.setAccessedAt(getTimestamp());
        out.setLines(lines);
        out.setObtainedBy(getName());

        return out;
    }

    private String getTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        format.setTimeZone(timeZone);
        return format.format(new Date());
    }
}
