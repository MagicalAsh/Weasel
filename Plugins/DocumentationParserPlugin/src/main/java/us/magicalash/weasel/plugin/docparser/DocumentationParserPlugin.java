package us.magicalash.weasel.plugin.docparser;

import com.google.gson.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.docparser.representation.JavaType;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationLexer;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class DocumentationParserPlugin implements IndexPlugin {
    @Override
    public String getName() {
        return "Java Documentation Parser";
    }

    @Override
    public String[] requestProperties() {
        return new String[0];
    }

    @Override
    public void load(Properties properties) {
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


        CodeVisitor listener = new CodeVisitor();
        try {
            listener.visit(parser.compilationUnit());
        } catch (ParseCancellationException e) {
            // parsing of this object failed
            throw new RuntimeException(e);
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
                for(String key : obj.keySet()) {
                    map.put(key, metadata.get(key).getAsString());
                }
                unit.setMetadata(map);
            }

            unit.setDestinationIndex("parsed_java");
            unit.setParsedObject(type);

            onCompletion.accept(unit);
        }
    }
}