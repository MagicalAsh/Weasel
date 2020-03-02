package us.magicalash.weasel.plugin;

import com.google.gson.*;
import org.antlr.v4.runtime.ANTLRErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import us.magicalash.weasel.index.plugin.IndexPlugin;
import us.magicalash.weasel.index.plugin.representations.ParsedCodeUnit;
import us.magicalash.weasel.plugin.docparser.JavaDocumentationLexer;
import us.magicalash.weasel.plugin.docparser.JavaDocumentationParser;
import us.magicalash.weasel.plugin.representation.JavaType;

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
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaDocumentationParser parser = new JavaDocumentationParser(tokens);
        CodeVisitor listener = new CodeVisitor();
        listener.visit(parser.compilationUnit());

        // todo parent/child relations might be a better idea here than normal direct indexing
        for (JavaType type : listener.getTypes()) {
            ParsedCodeUnit unit = new ParsedCodeUnit();
            unit.setIndexedBy(getName());

            if (obj.get("content_location") != null)
                unit.setLocation(obj.get("content_location").getAsString());

            if (obj.get("metadata") != null){
                Map<String, String> map = new HashMap<>();
                for(String key : obj.keySet()) {
                    map.put(key, obj.get(key).getAsString());
                }
                unit.setMetadata(map);
            }

            unit.setDestinationIndex("parsed_java");
            unit.setParsedObject(type);
            //todo set metadata
            onCompletion.accept(unit);
        }
    }
}
