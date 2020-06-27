package us.magicalash.weasel.plugin.docparser;

import com.google.gson.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.docparser.representation.JavaDocumentation;
import us.magicalash.weasel.plugin.docparser.visitor.CodeVisitor;
import us.magicalash.weasel.plugin.docparser.representation.JavaType;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationLexer;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser;
import us.magicalash.weasel.plugin.docparser.visitor.TypeCheckVisitor;
import us.magicalash.weasel.plugin.docparser.visitor.UnresolvedDependency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class DocumentationParserPlugin implements IndexPlugin {
    private static Logger logger = LoggerFactory.getLogger(DocumentationParserPlugin.class);

    private PackageHierarchy hierarchy;

    private ExecutorService threadPool;

    private DependencyManager dependencyManager;
    private Thread dependencyManagerThread;

    @Override
    public String getName() {
        return "Java Documentation Parser";
    }

    @Override
    public String[] requestProperties() {
        return new String[]{
                "hierarchy(parsed_java):name",
                "internalThreadPoolSize"
        };
    }

    @Override
    public void load(Properties properties) {
        this.hierarchy = (PackageHierarchy) properties.get("hierarchy(parsed_java):name");
        int threadPoolSize = Integer.getInteger((String) properties.get("internalThreadPoolSize"), 10);
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.dependencyManager = new DependencyManager(this.threadPool, this::runCodeVisitor);
        this.hierarchy.addTypeAddListener(this.dependencyManager::addType);

        this.dependencyManagerThread = new Thread(dependencyManager);
        this.dependencyManagerThread.setName("Documentation Parser Plugin Dependency Manager");
        this.dependencyManagerThread.start();
    }

    @Override
    public boolean canIndex(JsonObject obj) {
        if (obj.get("content_location") != null) {
            return obj.get("content_location").getAsString().endsWith(".java");
        }
         return false;
    }

    @Override
    public void index(JsonObject obj, Consumer<ParsedCodeUnit> onCompletion) {
        threadPool.submit(runTypeCheck(obj, onCompletion));
    }

    private JavaDocumentationParser createParser(JsonObject obj) {
        JsonArray fileContents = obj.getAsJsonArray("file_contents");
        StringBuilder contents = new StringBuilder();
        for(JsonElement element : fileContents) {
            contents.append(element.getAsString());
            contents.append('\n');
        }

        JavaDocumentationLexer lexer = new JavaDocumentationLexer(CharStreams.fromString(contents.toString()));

        // why is this on by default?
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaDocumentationParser parser = new JavaDocumentationParser(tokens);
        parser.setErrorHandler(new CommentIgnoringBailStrategy());
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE);

        return parser;
    }

    private Runnable runTypeCheck(JsonObject obj, Consumer<ParsedCodeUnit> onCompletion) {
        return () -> {
            JavaDocumentationParser parser = createParser(obj);

            TypeCheckVisitor listener = new TypeCheckVisitor(hierarchy);
            try {
                List<UnresolvedDependency> dependencies = listener.visit(parser.compilationUnit());
                this.dependencyManager.addUnit(dependencies, obj, onCompletion);
            } catch (ParseCancellationException e) {
                // parsing of this object failed
                String fileName = obj.get("content_location").getAsString();
                if (fileName.endsWith("package-info.java") || fileName.endsWith("module-info.java"))
                    return; // these files don't contain any useful information other than dependencies and exports

                logger.warn("Failed parsing object at: " + obj.get("content_location").getAsString());
                return;
            }
        };
    }

    private Runnable runCodeVisitor(JsonObject obj, Consumer<ParsedCodeUnit> onCompletion) {
        return () -> {
            JavaDocumentationParser parser = createParser(obj);

            CodeVisitor listener = new CodeVisitor(hierarchy);
            try {
                listener.visit(parser.compilationUnit());
            } catch (ParseCancellationException e) {
                // parsing of this object failed
                String fileName = obj.get("content_location").getAsString();
                if (fileName.endsWith("package-info.java") || fileName.endsWith("module-info.java"))
                    return; // these files don't contain any useful information other than dependencies and exports

                logger.warn("Failed parsing object at: " + obj.get("content_location").getAsString());
                return;
            }

            // todo parent/child relations might be a better idea here than normal direct indexing
            for (JavaType type : listener.getTypes()) {
                ParsedCodeUnit unit = new ParsedCodeUnit();
                unit.setIndexedBy(getName());

                if (obj.get("content_location") != null)
                    unit.setLocation(obj.get("content_location").getAsString());

                if (obj.get("metadata") != null){
                    JsonObject metadata = obj.get("metadata").getAsJsonObject();
                    Map<String, String> map = new HashMap<>();
                    for(String key : metadata.keySet()) {
                        map.put(key, metadata.get(key).getAsString());
                    }
                    unit.setMetadata(map);
                }

                unit.setDestinationIndex("parsed_java");
                unit.setParsedObject(type);

                onCompletion.accept(unit);
            }
        };
    }


}
