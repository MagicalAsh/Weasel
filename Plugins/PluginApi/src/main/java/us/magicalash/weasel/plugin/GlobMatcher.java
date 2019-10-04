package us.magicalash.weasel.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GlobMatcher {
    private List<Pattern> whiteListPatterns;
    private List<Pattern> blackListPatterns;

    @SuppressWarnings("unchecked")
    public GlobMatcher(List<String> whiteListGlobs, List<String> blackListGlobs) {
        if (whiteListGlobs == null) {
            whiteListGlobs = new ArrayList<>();
        }

        if (blackListGlobs == null) {
            blackListGlobs = new ArrayList<>();
        }

        whiteListPatterns = new ArrayList<>();
        for (String glob : whiteListGlobs) {
            String regex = globToRegex(glob);
            Pattern globPattern = Pattern.compile(regex);
            this.whiteListPatterns.add(globPattern);
        }

        blackListPatterns = new ArrayList<>();
        for (String glob : blackListGlobs) {
            String regex = globToRegex(glob);
            Pattern globPattern = Pattern.compile(regex);
            this.blackListPatterns.add(globPattern);
        }
    }

    public boolean isWhitelisted(String name) {
        for (Pattern p : whiteListPatterns) {
            if (p.matcher(name).find()) {
                return true;
            }
        }

        return false;
    }

    public boolean isBlacklisted(String name) {
        for (Pattern p : blackListPatterns) {
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
