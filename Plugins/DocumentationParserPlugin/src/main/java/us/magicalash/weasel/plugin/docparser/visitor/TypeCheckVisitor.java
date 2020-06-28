package us.magicalash.weasel.plugin.docparser.visitor;

import org.antlr.v4.runtime.tree.TerminalNode;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParserBaseVisitor;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class visits a java class and builds a list of required types. This class
 * visits type declarations and usages to ensure that all required types are available,
 * and returns a dependency model for the unknown classes encountered.
 */
public class TypeCheckVisitor extends JavaDocumentationParserBaseVisitor<List<UnresolvedDependency>> {
    private final TypeResolutionHelper typeHelper;
    private final List<UnresolvedDependency> unresolvedDependencies;
    private final Deque<String> typeNames;

    public TypeCheckVisitor(PackageHierarchy packageHierarchy) {
        this.typeHelper = new TypeResolutionHelper(packageHierarchy);
        this.unresolvedDependencies = new ArrayList<>();
        this.typeNames = new ArrayDeque<>();
    }

    @Override
    public List<UnresolvedDependency> visitPackageDeclaration(PackageDeclarationContext ctx) {
        String packageName = ctx.qualifiedName().getText().replaceAll("\\.", "/");
        typeHelper.addStarImport(packageName);
        typeNames.add(packageName);
        return super.visitPackageDeclaration(ctx);
    }

    @Override
    public List<UnresolvedDependency> visitImportDeclaration(ImportDeclarationContext ctx) {
        // see the above comment explaining why '.' is replaced with '/'.
        JavaDocumentationParser.QualifiedNameContext fqnContext = ctx.qualifiedName();
        String fqn = fqnContext.getText().replaceAll("\\.", "/");
        fqn = fqn.replace(".*", "");

        // we don't care about static imports, they're constants not types.
        if (ctx.STATIC() == null) {
            if (ctx.MUL() == null) { // if its not a star import
                typeHelper.addImport(fqn);
            } else {
                // it is a star import
                typeHelper.addStarImport(fqn);
            }
        }

        return super.visitImportDeclaration(ctx);
    }

    @Override
    public List<UnresolvedDependency> visitCompilationUnit(CompilationUnitContext ctx) {
        super.visitCompilationUnit(ctx);

        // filter it again, in case we find a resolvable type later down the road.
        return unresolvedDependencies.stream()
                .filter(Predicate.not(typeHelper::canResolve))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnresolvedDependency> visitBlock(BlockContext ctx) {
        return null; // we don't care about type definitions within blocks of code
    }

    @Override
    public List<UnresolvedDependency> visitTypeParameters(TypeParametersContext ctx) {
        // type parameters are used when defining a generic, not when using one.
        List<String> generics = new ArrayList<>();
        for (TypeParameterContext context : ctx.typeParameter()) {
            this.typeHelper.addImport(context.IDENTIFIER().getText());
        }

        return null;
    }

    @Override
    public List<UnresolvedDependency> visitTypeArguments(TypeArgumentsContext ctx) {
        return null;
    }

    @Override
    public List<UnresolvedDependency> visitTypeType(TypeTypeContext ctx) {
        if (typeHelper.getTypeName(ctx) == null) {
            // if this is true, we've discovered an unresolved type.
            UnresolvedDependency dependency = new UnresolvedDependency();
            String name = typeHelper.getTypeNameWithoutResolving(ctx);
            name = name.replace("[]", "");
            dependency.setName(name);
            dependency.setValidPackages(typeHelper.getStarImports());
            this.unresolvedDependencies.add(dependency);
        }

        return super.visitTypeType(ctx);
    }

    /*
     * The visit*Declaration methods add their respective types to the package hierarchy
     * so that methods can return them.
     */

    @Override
    public List<UnresolvedDependency> visitInterfaceDeclaration(InterfaceDeclarationContext ctx) {
        typeHelper.addImport(getName(ctx.IDENTIFIER()));
        typeNames.push(ctx.IDENTIFIER().getText());
        List<UnresolvedDependency> out = super.visitInterfaceDeclaration(ctx);
        typeNames.pop();
        return out;
    }

    @Override
    public List<UnresolvedDependency> visitClassDeclaration(ClassDeclarationContext ctx) {
        typeHelper.addImport(getName(ctx.IDENTIFIER()));
        typeNames.push(ctx.IDENTIFIER().getText());
        List<UnresolvedDependency> out = super.visitClassDeclaration(ctx);
        typeNames.pop();
        return out;
    }

    @Override
    public List<UnresolvedDependency> visitAnnotationTypeDeclaration(AnnotationTypeDeclarationContext ctx) {
        typeHelper.addImport(getName(ctx.IDENTIFIER()));
        typeNames.push(ctx.IDENTIFIER().getText());
        List<UnresolvedDependency> out = super.visitAnnotationTypeDeclaration(ctx);
        typeNames.pop();
        return out;
    }

    @Override
    public List<UnresolvedDependency> visitEnumDeclaration(EnumDeclarationContext ctx) {
        typeHelper.addImport(getName(ctx.IDENTIFIER()));
        typeNames.push(ctx.IDENTIFIER().getText());
        List<UnresolvedDependency> out = super.visitEnumDeclaration(ctx);
        typeNames.pop();
        return out;
    }

    private String getName(TerminalNode identifier) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> nameComponents = typeNames.descendingIterator();
        while (nameComponents.hasNext()) {
            String prefix = nameComponents.next();
            builder.append(prefix);
            builder.append('/');
        }

        builder.append(identifier.getText());

        return builder.toString();
    }
}
