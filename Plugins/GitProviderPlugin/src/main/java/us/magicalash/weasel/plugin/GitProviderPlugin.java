package us.magicalash.weasel.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.magicalash.weasel.provider.plugin.ProviderPlugin;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitProviderPlugin implements ProviderPlugin {
    private static final Logger logger = LoggerFactory.getLogger(GitProviderPlugin.class);
    private Properties properties;


    @Override
    public String getName() {
        return "Git Provider Plugin";
    }

    @Override
    public String[] requestProperties() {
        return new String[0];
    }

    @Override
    public void load(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean canRefresh(String name) {
        boolean canRefresh = false;
        if (name.startsWith("file://") && isLocalRepo(name.replace("file://", ""))) {
            canRefresh = true;
        }

        if (isRemoteRepo(name)) {
            canRefresh = true;
        }

        return canRefresh;
    }

    @Override
    public JsonArray refresh(String name) {
        JsonArray files = new JsonArray();
        try {

            Repository repo = getRepo(name);
            List<Ref> call = new Git(repo).branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref ref : call) {
                files.addAll(traverseBranch(ref.getName(), repo));
            }
        } catch (IOException e) {
            logger.warn("Something went wrong while trying to refresh a git repository.", e);
        } catch (GitAPIException e) {
            logger.warn("Something went wrong while interacting with git.", e);
        }

        return files;
    }

    private JsonArray traverseBranch(String branchName, Repository repository) throws IOException, GitAPIException {
        JsonArray out = new JsonArray();
        Git git = new Git(repository);
        // checkout the branch so we can traverse it
        git.checkout().setName(branchName).setForced(true).call();

        // oh my god who designed this api?
        ObjectId lastCommitId = repository.resolve(Constants.HEAD);
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();
            TreeWalk walk = new TreeWalk(repository);
            walk.setRecursive(true);
            walk.addTree(tree);
            while (walk.next()) {
                JsonObject fileData = new JsonObject();
                JsonArray lines = new JsonArray();

                ObjectLoader loader = repository.open(walk.getObjectId(0));
                Scanner fileReader = new Scanner(loader.openStream());
                while (fileReader.hasNext()) {
                    lines.add(fileReader.nextLine());
                }
                fileReader.close();

                fileData.add("file_contents", lines);
                fileData.addProperty("content_location", walk.getPathString());
                fileData.addProperty("accessed", getTimestamp());
                fileData.addProperty("obtained_by", getName());
                fileData.addProperty("branch_name", branchName);
                // todo add information about the file, like when commited etc
                out.add(fileData);
            }
        }

        return out;
    }

    private Repository getRepo(String name){
        if (isRemoteRepo(name)) {
            try {
                Git git = Git.cloneRepository().setURI(name).call();
                return git.getRepository();
            } catch (GitAPIException e){
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
            Repository repo = new RepositoryBuilder().setWorkTree(new File(name)).build();
            repo.close();
        } catch (RepositoryNotFoundException e) {
            return false; // This file isn't a repo. Good bye.
        } catch (IOException e) {
            logger.warn("Something went wrong while trying to build a local repository.", e);
            return false; // something else went wrong. Lets not try to process this.
        }

        return true;
    }

    private boolean isRemoteRepo(String name) {
        final LsRemoteCommand lsCmd = new LsRemoteCommand(null);
        lsCmd.setRemote(name);
        try {
            lsCmd.call();
        } catch (InvalidRemoteException e) {
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
