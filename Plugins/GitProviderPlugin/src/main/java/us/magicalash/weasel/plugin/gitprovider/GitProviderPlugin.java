package us.magicalash.weasel.plugin.gitprovider;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.magicalash.weasel.plugin.GlobMatcher;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;
import us.magicalash.weasel.provider.plugin.representations.ProvidedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class GitProviderPlugin implements ProviderPlugin {
    private static final Logger logger = LoggerFactory.getLogger(GitProviderPlugin.class);
    private Properties properties;
    private GlobMatcher globMatcher;


    @Override
    public String getName() {
        return "Git Provider Plugin";
    }

    @Override
    public String[] requestProperties() {
        return new String[] {
                GitConstants.TEMP_DIR,
                GitConstants.BRANCH_WHITELIST,
                GitConstants.BRANCH_BLACKLIST
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(Properties properties) {
        this.globMatcher = new GlobMatcher((List<String>) properties.get(GitConstants.BRANCH_WHITELIST),
                                           (List<String>) properties.get(GitConstants.BRANCH_BLACKLIST));
        this.properties = properties;
    }

    @Override
    public boolean canRefresh(String name) {
        boolean canRefresh = false;
        if (name.startsWith("file+git://") && isLocalRepo(name.replace("file+git://", ""))) {
            canRefresh = true;
        }

        if (!canRefresh && isRemoteRepo(name)) {
            canRefresh = true;
        }

        return canRefresh;
    }

    @Override
    public void refresh(String name, Consumer<ProvidedFile> onProduce) {
        try {
            Repository repo = getRepo(name);
            List<Ref> call = new Git(repo).branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref ref : call) {
                traverseBranch(ref.getName(), repo, onProduce);
            }
        } catch (IOException e) {
            logger.warn("Something went wrong while trying to refresh a git repository.", e);
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            logger.warn("Something went wrong while interacting with git.", e);
            throw new RuntimeException(e);
        }
    }

    private void traverseBranch(String branchName, Repository repository, Consumer<ProvidedFile> onProduce) throws IOException, GitAPIException {
        if (globMatcher.isBlacklisted(branchName)) { // ignore blacklist branches
            if (!globMatcher.isWhitelisted(branchName)) { // don't ignore whitelisted ones though
                return;
            }
        }

        Git git = new Git(repository);
        // checkout the branch so we can traverse it
        git.checkout().setName(branchName).setForced(true).call();

        // keep track of what files we've visited so far, so we don't visit files at
        // various parts of their lifecycle.
        HashSet<String> fileName = new HashSet<>();

        // oh my god who designed this api?
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        try (RevWalk revWalk = new RevWalk(repository)) {
            // newest to oldest
            revWalk.sort(RevSort.COMMIT_TIME_DESC);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            TreeWalk walk = new TreeWalk(repository);
            walk.setRecursive(true);
            walk.addTree(tree);
            while (walk.next()) {
                // if we haven't visited here before, provide it
                if (!fileName.contains(walk.getPathString())) {
                    fileName.add(walk.getPathString());

                    ProvidedFile fileData = new ProvidedFile();
                    List<String> lines = new ArrayList<>();

                    ObjectLoader loader = repository.open(walk.getObjectId(0));
                    Scanner fileReader = new Scanner(loader.openStream());
                    while (fileReader.hasNext()) {
                        lines.add(fileReader.nextLine());
                    }
                    fileReader.close();

                    fileData.setLines(lines);
                    fileData.setFileLocation(walk.getPathString());
                    fileData.setAccessedAt(getTimestamp());
                    fileData.setObtainedBy(getName());

                    Map<String, String> gitData = new HashMap<>(2);
                    gitData.put("branch_name", branchName);
                    gitData.put("commit_id", walk.getObjectId(0).name());

                    fileData.setMetadata(gitData);
                    // todo add information about the file, like when commited etc
                    onProduce.accept(fileData);
                }
            }
        }
    }

    private Repository getRepo(String name){
        if (name.startsWith("file+git://")) {
            name = name.replace("file+git://", "");
        }

        if (isRemoteRepo(name)) {
            Path workingDir = Paths.get(properties.getProperty(GitConstants.TEMP_DIR));

            try {
                File tempFile = Files.createTempDirectory(workingDir, "gitTemp").toFile();

                Git git = Git.cloneRepository()
                                .setDirectory(tempFile)
                                .setGitDir(new File(tempFile.getAbsoluteFile() + "/.git"))
                                .setURI(name)
                                .call();
                return git.getRepository();
            } catch (GitAPIException|IOException e){
                logger.warn("Cloning Remote repo failed!");
                throw new RuntimeException("Failed to get remote repository.", e);
            }
        }
        RepositoryBuilder builder = new RepositoryBuilder();
        builder.setWorkTree(new File(name));

        try {
            return builder.build();
        } catch (IOException e) {
            logger.warn("Failed to build local repository.", e);
            throw new RuntimeException("Failed to build local repository.", e);
        }
    }

    private boolean isLocalRepo(String name) {
        try {
            return new RepositoryBuilder().setWorkTree(new File(name)).build().getObjectDatabase().exists();
        } catch (RepositoryNotFoundException e) {
            return false; // This file isn't a repo. Good bye.
        } catch (IOException e) {
            logger.warn("Something went wrong while trying to build a local repository.", e);
            return false; // something else went wrong. Lets not try to process this.
        }
    }

    private boolean isRemoteRepo(String name) {
        final LsRemoteCommand lsCmd = new LsRemoteCommand(null);
        lsCmd.setRemote(name);
        try {
            lsCmd.call();
        } catch (InvalidRemoteException | TransportException | JGitInternalException e) {
            return false; // it's not a valid remote address in the first place, so we can't refresh it.
        } catch (GitAPIException e) {
            // Something went wrong while working with git. We may  not be able to recover from it, so we
            // can just say its not valid
            logger.warn("Something went wrong while interacting with Git!", e);
            return false;
        }

        return true;
    }

    private String getTimestamp() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        format.setTimeZone(timeZone);
        return format.format(new Date());
    }
}
