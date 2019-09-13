package us.magicalash.weasel.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class GlobMatcher {
    public static final String IGNORED_FILES = "weasel.provider.plugin.files.ignored[*]";

    private List<String> globs;
    private List<Pattern> globPatterns;

    public GlobMatcher(Properties properties) {
        globs = (List<String>) properties.get(IGNORED_FILES);
        if (globs == null) {
            globs = new ArrayList<>();
        }
        globPatterns = new ArrayList<>();
        for (String glob : globs) {
            String regex = globToRegex(glob);
            Pattern globPattern = Pattern.compile(regex);
            this.globPatterns.add(globPattern);
        }
    }

    public boolean matchesAny(String name) {
        for (Pattern p : globPatterns) {
            if (p.matcher(name).find()) {
                return true;
            }
        }

        return false;
    }

    private String globToRegex(String glob) {
        StringBuilder out = new StringBuilder();
        for(int i = 0; i < glob.length(); ++i)
        {
            final char c = glob.charAt(i);
            switch(c)
            {
                case '*':
                    out.append(".*");
                    break;
                case '?':
                    out.append('.');
                    break;
                case '.':
                    out.append("\\.");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                default: out.append(c);
            }
        }
        return out.append("$").toString();
    }


}
