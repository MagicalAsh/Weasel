package us.magicalash.weasel.plugin.docparser.visitor;

import lombok.Getter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeResolutionHelper {
    public static Logger logger = LoggerFactory.getLogger(TypeResolutionHelper.class);

    @Getter
    private final List<String> starImports;
    private final PackageHierarchy hierarchy;
    private final Map<String, String> imports;

    public TypeResolutionHelper(PackageHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        this.starImports = new ArrayList<>();
        this.starImports.add("java/lang");
        this.imports = new HashMap<>();
    }

    public void addStarImport(String starImport) {
        this.starImports.add(starImport);
    }

    public void addImport(String importName) {
        String[] identifiers = importName.split("/");
        imports.put(identifiers[identifiers.length - 1], importName);

        // if we've imported it, we can assume that it is a type of
        // some kind.
        hierarchy.addType(importName);
    }

    public String resolveName(String name) {
        if (name.contains("/")) {
            String[] qualifiedName = name.split("/");
            String prefix = resolveName(qualifiedName[0]);
            if (prefix != null) {
                StringBuilder postfix = new StringBuilder();
                for (int i = 1; i < qualifiedName.length; i++) {
                    postfix.append('/');
                    postfix.append(qualifiedName[i]);
                }
                name = prefix + postfix.toString();
            }
        }

        String resolvedName = null;
        if (imports.containsKey(name)) {
            resolvedName = imports.get(name);
        } else if (hierarchy.containsType(name)) {
            resolvedName = name;
        } else {
            for (String prefix : starImports) {
                if (hierarchy.containsType(prefix + "/" + name)) {
                    resolvedName = prefix + "/" + name;
                    break; // this is gross but we don't need to continue
                }
            }
        }

        // if it's still not resolved, we have an issue
        if (resolvedName == null) {
            return null;
        }

        resolvedName = resolvedName.replace('.', '/');

        logger.trace("Resolved '{}' to '{}'", name, resolvedName);

        return resolvedName;
    }

    public String getTypeName(JavaDocumentationParser.TypeTypeContext context) {
        if (context == null) {
            return "java/lang/Object";
        }

        StringBuilder name = new StringBuilder();
        if (context.classOrInterfaceType() != null) {
            JavaDocumentationParser.ClassOrInterfaceTypeContext typeContext = context.classOrInterfaceType();
            // Resolve this name into a qualified name. We always need to resolve the first name
            // (since a qualified name could be an inner class of an imported name). After that,
            // everything can be assumed to be a qualified name.
            List<TerminalNode> identifiers = typeContext.IDENTIFIER();
            String prefix = resolveName(identifiers.get(0).getText());

            if (prefix != null) {
                name.append(prefix);
            } else if (hierarchy.containsPackage(identifiers.get(0).getText())) {
                name.append(identifiers.get(0).getText());
            } else {
                // if it's neither in imported or a package, it's a type
                // that hasn't been discovered yet. Not good.
                return null;
            }


            for (int i = 1; i < identifiers.size(); i++) {
                name.append('/');
                name.append(identifiers.get(i).getText());
            }
        } else if (context.primitiveType() != null) { // its a primitive type
            JavaDocumentationParser.PrimitiveTypeContext typeContext = context.primitiveType();
            name.append(typeContext.getChild(0).getText());
        }

        context.LBRACK().forEach(e -> name.append("[]"));

        return name.toString();
    }

    public boolean canResolve(UnresolvedDependency dependency) {
        if (this.resolveName(dependency.getName()) != null) {
            return true;
        }

        for (String prefix : dependency.getValidPackages()) {
            if (this.resolveName(prefix + '/' + dependency.getName()) != null) {
                return true;
            }
        }

        return false;
    }
}
