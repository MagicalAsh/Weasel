package us.magicalash.weasel.plugin;

import lombok.Getter;
import us.magicalash.weasel.plugin.docparser.JavadocParser;
import us.magicalash.weasel.plugin.docparser.JavadocParserBaseVisitor;
import us.magicalash.weasel.plugin.representation.JavaDocumentation;

import java.util.ArrayList;
import java.util.List;

import static us.magicalash.weasel.plugin.docparser.JavadocParser.*;

public class DocumentationVisitor extends JavadocParserBaseVisitor<JavaDocumentation> {

    @Getter
    private JavaDocumentation documentation;

    public DocumentationVisitor() {
        super();
        documentation = new JavaDocumentation();
    }

    @Override
    public JavaDocumentation visitDocumentation(DocumentationContext ctx) {
        super.visitDocumentation(ctx);
        return documentation;
    }

    @Override
    public JavaDocumentation visitDocumentationContent(JavadocParser.DocumentationContentContext ctx) {
        // we don't really care about the text proper
        if (ctx.description() != null) {
            String body = ctx.description().getText().trim();
            body = body.replaceAll("\n *\\* *", "");
            documentation.setBody(body);
        }
        if (ctx.tagSection() != null)
            visit(ctx.tagSection());

        return documentation;
    }

    @Override
    public JavaDocumentation visitBlockTag(JavadocParser.BlockTagContext ctx) {
        List<String> contents = new ArrayList<>(0);

        for (BlockTagContentContext content : ctx.blockTagContent()) {
            String tagContent = content.getText().trim().replace("\n *\\* *", " ");
            if (!tagContent.equals("") && !tagContent.equals("*")) {
                contents.add(tagContent);
            }
        }

        documentation.getTags().put(ctx.blockTagName().getText(), contents);

        return super.visitBlockTag(ctx);
    }
}
