package us.magicalash.weasel.plugin.docparser;

import lombok.Getter;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import us.magicalash.weasel.plugin.PackageHierarchy;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParserBaseVisitor;
import us.magicalash.weasel.plugin.docparser.generated.JavadocLexer;
import us.magicalash.weasel.plugin.docparser.generated.JavadocParser;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser;
import us.magicalash.weasel.plugin.docparser.generated.JavaDocumentationParser.*;
import us.magicalash.weasel.plugin.docparser.representation.*;

import java.util.*;

public class CodeVisitor extends JavaDocumentationParserBaseVisitor<JavaCodeUnit> {
    private static final int[] KEYWORD_MODIFIERS = {
            JavaDocumentationParser.ABSTRACT, JavaDocumentationParser.FINAL, JavaDocumentationParser.STATIC,
            JavaDocumentationParser.STRICTFP, JavaDocumentationParser.PUBLIC, JavaDocumentationParser.PROTECTED,
            JavaDocumentationParser.PRIVATE, JavaDocumentationParser.NATIVE, JavaDocumentationParser.TRANSIENT,
            JavaDocumentationParser.VOLATILE, JavaDocumentationParser.SYNCHRONIZED, JavaDocumentationParser.DEFAULT
    };

    private String packageName;
    private List<String> starImports;
    private PackageHierarchy hierarchy;
    private Map<String, String> imports;

    private Deque<JavaCodeUnit> codeUnitsEncountered;

    /**
     * A list of all types encountered so far
     */
    @Getter
    private List<JavaType> types;

    public CodeVisitor(PackageHierarchy hierarchy) {
        super();

        // Most files I've ever interacted with have only one top level class file,
        // so the overhead of growing is better than wasted space
        types = Collections.synchronizedList(new ArrayList<>(1));
        this.hierarchy = hierarchy;
        codeUnitsEncountered = new ArrayDeque<>();
        packageName = "";
        starImports = new ArrayList<>();
        imports = new HashMap<>();
    }

    @Override
    public JavaCodeUnit visit(ParseTree tree) {
        if (tree != null)
            return super.visit(tree);
        else
            return null;
    }

    @Override
    public JavaCodeUnit visitTypeDeclaration(TypeDeclarationContext ctx) {
        JavaCodeUnit unit;

        ParseTree tree;
        if (ctx.classDeclaration() != null) {
            tree = ctx.classDeclaration();
        } else if (ctx.enumDeclaration() != null) {
            tree = ctx.enumDeclaration();
        } else if (ctx.interfaceDeclaration() != null) {
            tree = ctx.interfaceDeclaration();
        } else if (ctx.annotationTypeDeclaration() != null) {
            tree = ctx.annotationTypeDeclaration();
        } else {
            // there are no type declarations?
            return null;
        }

        if (!packageName.equals("")) {
            JavaType dummyType = new JavaType();
            dummyType.setName(packageName); // this is a workaround to make sure that the top level gets its name right
            codeUnitsEncountered.push(dummyType);
        }

        unit = visit(tree);

        if (!packageName.equals(""))
            codeUnitsEncountered.pop();

        codeUnitsEncountered.push(unit);

        ctx.classOrInterfaceModifier().forEach(this::visit);

        if (ctx.documentation() != null) {
            visit(ctx.documentation());
        }

        unit.setStartLine(ctx.start.getLine());
        unit.setEndLine(ctx.stop.getLine());
        codeUnitsEncountered.pop();

        return unit;
    }

    @Override
    public JavaCodeUnit visitInterfaceDeclaration(InterfaceDeclarationContext ctx) {
        JavaType type = new JavaType();

        type.setName(getName(ctx.IDENTIFIER()));

        // technically this is an extends, but for simplicity
        type.getImplementsInterfaces().addAll(getImplementedInterfaces(ctx.typeList()));

        type.setType(ctx.INTERFACE().getText());

        type.setStartLine(ctx.start.getLine());
        type.setEndLine(ctx.stop.getLine());

        types.add(type);

        codeUnitsEncountered.push(type);

        visit(ctx.interfaceBody());

        codeUnitsEncountered.pop();

        return type;
    }

    @Override
    public JavaCodeUnit visitAnnotationTypeDeclaration(AnnotationTypeDeclarationContext ctx) {
        JavaType annotation = new JavaType();

        annotation.setName(getName(ctx.IDENTIFIER()));

        annotation.setType(ctx.AT().getText() + ctx.INTERFACE().getText());

        annotation.setImplementsInterfaces(Collections.singletonList("java/lang/annotation/Annotation"));
        annotation.setParentClass("java/lang/Object");

        annotation.setStartLine(ctx.start.getLine());
        annotation.setEndLine(ctx.stop.getLine());

        types.add(annotation);

        codeUnitsEncountered.push(annotation);
        visit(ctx.annotationTypeBody());
        codeUnitsEncountered.pop();
        return annotation;
    }

    @Override
    public JavaCodeUnit visitAnnotationTypeElementRest(AnnotationTypeElementRestContext ctx) {
        if (ctx.typeType() != null) {
            JavaType parent = (JavaType) codeUnitsEncountered.peek();
            JavaCodeUnit unit = visit(ctx.annotationMethodOrConstantRest());
            if (unit instanceof DummyContainer) {
                for (JavaCodeUnit inner : ((DummyContainer) unit).getDummyContainer()){
                    JavaVariable constant = (JavaVariable) inner;
                    constant.setType(getTypeName(ctx.typeType()));
                    parent.getFields().add(constant);
                }
            } else { //its a method
                JavaMethod method = (JavaMethod) unit;
                method.setReturnType(getTypeName(ctx.typeType()));
                parent.getMethods().add(method);
            }

            return unit;
        } else {
            ParseTree tree;
            if (ctx.annotationTypeDeclaration() != null) {
                tree = ctx.annotationTypeDeclaration();
            } else if (ctx.classDeclaration() != null) {
                tree = ctx.classDeclaration();
            } else if (ctx.enumDeclaration() != null) {
                tree = ctx.enumDeclaration();
            } else if (ctx.interfaceDeclaration() != null) {
                tree = ctx.interfaceDeclaration();
            } else {
                throw new RuntimeException("AnnotationTypeElementRest has no body?");
            }

            return visit(tree);
        }
    }

    @Override
    public JavaCodeUnit visitAnnotationMethodRest(AnnotationMethodRestContext ctx) {
        JavaMethod method = new JavaMethod();

        method.setName(ctx.IDENTIFIER().getText());

        method.setStartLine(ctx.start.getLine());
        method.setEndLine(ctx.stop.getLine());

        return method;
    }

    @Override
    public JavaCodeUnit visitAnnotationConstantRest(AnnotationConstantRestContext ctx) {
        DummyContainer container = new DummyContainer();
        List<JavaVariable> vars = new ArrayList<>();
        for (VariableDeclaratorContext context : ctx.variableDeclarators().variableDeclarator()) {
            JavaVariable variable = new JavaVariable();

            //todo make this handle 'Type name[]' type declarations
            variable.setName(context.variableDeclaratorId().IDENTIFIER().getText());
            variable.setStartLine(context.start.getLine());
            variable.setEndLine(context.stop.getLine());

            vars.add(variable);
        }
        container.setDummyContainer(vars);
        return container;
    }

    @Override
    public JavaCodeUnit visitAnnotationTypeElementDeclaration(AnnotationTypeElementDeclarationContext ctx) {
        JavaCodeUnit unit = visit(ctx.annotationTypeElementRest());

        if (!(unit instanceof DummyContainer)) {
            codeUnitsEncountered.push(unit);
            if (ctx.documentation() != null)
                visit(ctx.documentation());

            ctx.modifier().forEach(this::visit);
            codeUnitsEncountered.pop();
        } else {
            DummyContainer container = (DummyContainer) unit;
            for (JavaCodeUnit inner : container.getDummyContainer()) {
                codeUnitsEncountered.push(inner);
                if (ctx.documentation() != null)
                    visit(ctx.documentation());

                ctx.modifier().forEach(this::visit);
                codeUnitsEncountered.pop();
            }
        }

        return unit;
    }

    @Override
    public JavaCodeUnit visitInterfaceBodyDeclaration(InterfaceBodyDeclarationContext ctx) {
        JavaCodeUnit unit = visit(ctx.interfaceMemberDeclaration());

        if (unit == null) {
            return null;
        }

        // since the member declarations add themselves, we need to fix their modifiers only
        if (!(unit instanceof DummyContainer)) { //we have a single member
            codeUnitsEncountered.push(unit);
            if (ctx.documentation() != null)
                visit(ctx.documentation());
            ctx.modifier().forEach(this::visit); // visit each modifier
            codeUnitsEncountered.pop();
        } else { // there were multiple units internally, so we need to mark all of them
            for (JavaCodeUnit subUnit : ((DummyContainer) unit).getDummyContainer()) {
                codeUnitsEncountered.push(subUnit);
                if (ctx.documentation() != null)
                    visit(ctx.documentation());
                ctx.modifier().forEach(this::visit);
                codeUnitsEncountered.pop();
            }
        }

        return null;
    }

    @Override
    public JavaCodeUnit visitInterfaceMethodDeclaration(InterfaceMethodDeclarationContext ctx) {
        JavaMethod method = new JavaMethod();

        method.setName(ctx.IDENTIFIER().getText());

        method.setStartLine(ctx.start.getLine());
        method.setEndLine(ctx.stop.getLine());

        codeUnitsEncountered.push(method);

        for (AnnotationContext annotationContext : ctx.annotation())
            visit(annotationContext);

        // get the method return type instead of just visiting it
        if (ctx.typeTypeOrVoid().typeType() != null)
            method.setReturnType(getTypeName(ctx.typeTypeOrVoid().typeType()));
        else
            method.setReturnType(ctx.typeTypeOrVoid().VOID().getText());

        ctx.interfaceMethodModifier().forEach(this::visit);

        codeUnitsEncountered.pop();

        return manageParameters(method, ctx.formalParameters());
    }

    @Override
    public JavaCodeUnit visitInterfaceMethodModifier(InterfaceMethodModifierContext ctx) {
        List<String> keywordModifiers = codeUnitsEncountered.peek().getModifiers();

        for (int name : KEYWORD_MODIFIERS) {
            List<TerminalNode> nodes = ctx.getTokens(name);
            for (TerminalNode node : nodes) {
                keywordModifiers.add(node.getText());
            }
        }

        return null;
    }

    @Override
    public JavaCodeUnit visitConstructorDeclaration(ConstructorDeclarationContext ctx) {
        JavaMethod constructor = new JavaMethod();

        constructor.setName("#constructor");

        String className = ctx.IDENTIFIER().getText();
        constructor.setReturnType(resolveName(className));

        constructor.setStartLine(ctx.start.getLine());
        constructor.setEndLine(ctx.stop.getLine());

        return manageParameters(constructor, ctx.formalParameters());
    }

    private JavaCodeUnit manageParameters(JavaMethod methodLike, FormalParametersContext formalParametersContext) {
        codeUnitsEncountered.push(methodLike);
        visit(formalParametersContext);
        codeUnitsEncountered.pop();

        JavaCodeUnit unit = codeUnitsEncountered.peek();
        if (unit instanceof JavaType) {
            ((JavaType) unit).getMethods().add(methodLike);
        } else {
            // this should never happen
            throw new RuntimeException("Parent of class method-like is not a type");
        }

        return methodLike;
    }

    @Override
    public JavaCodeUnit visitConstDeclaration(ConstDeclarationContext ctx) {
        // todo rewrite
        List<JavaVariable> variables = new ArrayList<>(1);
        JavaType enclosingType = (JavaType) codeUnitsEncountered.peek();

        for (ConstantDeclaratorContext context : ctx.constantDeclarator()) {
            JavaVariable variable = (JavaVariable) visit(context);

            variable.setType(getTypeName(ctx.typeType()));

            enclosingType.getFields().add(variable);

            variables.add(variable);
        }

        if (variables.size() == 1) {
            return variables.get(0);
        }

        DummyContainer container = new DummyContainer();
        container.setDummyContainer(variables);
        return container;
    }

    @Override
    public JavaCodeUnit visitConstantDeclarator(ConstantDeclaratorContext ctx) {
        JavaVariable variable = new JavaVariable();

        variable.setName(ctx.IDENTIFIER().getText());

        variable.setStartLine(ctx.start.getLine());
        variable.setEndLine(ctx.stop.getLine());

        return variable;
    }

    @Override
    public JavaCodeUnit visitFieldDeclaration(FieldDeclarationContext ctx) {
        List<JavaVariable> variables = new ArrayList<>(1);
        JavaType parent = ((JavaType) codeUnitsEncountered.peek());

        for (VariableDeclaratorContext context : ctx.variableDeclarators().variableDeclarator()) {
            JavaVariable variable = new JavaVariable();

            variable.setType(getTypeName(ctx.typeType()));

            //todo make this handle 'Type name[]' type declarations
            variable.setName(context.variableDeclaratorId().IDENTIFIER().getText());

            variable.setStartLine(context.start.getLine());
            variable.setEndLine(context.stop.getLine());

            variables.add(variable);

            parent.getFields().add(variable);
        }

        if (variables.size() == 1) {
            // we only have one field, so just return it directly
            return variables.get(0);
        } else {
            DummyContainer container = new DummyContainer();
            container.setDummyContainer(variables);
            return container;
        }
    }

    @Override
    public JavaCodeUnit visitClassDeclaration(ClassDeclarationContext ctx) {
        JavaType type = new JavaType();

        type.setType(ctx.CLASS().getText());

        type.setName(getName(ctx.IDENTIFIER()));

        type.setParentClass(getTypeName(ctx.typeType()));

        type.getImplementsInterfaces().addAll(getImplementedInterfaces(ctx.typeList()));

        type.setStartLine(ctx.start.getLine());
        type.setEndLine(ctx.stop.getLine());

        types.add(type);

        codeUnitsEncountered.push(type);
        visit(ctx.classBody());
        codeUnitsEncountered.pop();

        return type;
    }

    @Override
    public JavaCodeUnit visitClassBodyDeclaration(ClassBodyDeclarationContext ctx) {
        JavaCodeUnit unit = visit(ctx.memberDeclaration());

        if (unit == null) {
            return null;
        }

        codeUnitsEncountered.push(unit);

        ctx.modifier().forEach(this::visit);

        if (ctx.documentation() != null)
            visit(ctx.documentation());

        codeUnitsEncountered.pop();

        return unit;
    }

    @Override
    public JavaCodeUnit visitMethodDeclaration(MethodDeclarationContext ctx) {
        JavaMethod method = new JavaMethod();

        method.setName(ctx.IDENTIFIER().toString());

        TypeTypeOrVoidContext retTypeContext = ctx.typeTypeOrVoid();
        if (retTypeContext.VOID() != null) {
            method.setReturnType(retTypeContext.VOID().getText());
        } else {
            method.setReturnType(getTypeName(retTypeContext.typeType()));
        }

        method.setStartLine(ctx.start.getLine());
        method.setEndLine(ctx.stop.getLine());

        return manageParameters(method, ctx.formalParameters());
    }

    @Override
    public JavaCodeUnit visitFormalParameters(FormalParametersContext ctx) {
        FormalParameterListContext parameterListContext  = ctx.formalParameterList();

        JavaMethod unit = (JavaMethod) codeUnitsEncountered.peekFirst();
        if (unit == null)
            return null; // this should never happen

        if (parameterListContext != null) {
            for (FormalParameterContext parameterContext : parameterListContext.formalParameter()) {
                JavaVariable variable = new JavaVariable();
                variable.setType(getTypeName(parameterContext.typeType()));
                variable.setName(parameterContext.variableDeclaratorId().IDENTIFIER().getText());

                variable.setStartLine(parameterContext.start.getLine());
                variable.setEndLine(parameterContext.stop.getLine());

                codeUnitsEncountered.push(variable);
                parameterContext.variableModifier().forEach(this::visit);
                codeUnitsEncountered.pop();

                unit.getParameters().add(variable);
            }

            if (parameterListContext.lastFormalParameter() != null) {
                LastFormalParameterContext lastContext = parameterListContext.lastFormalParameter();
                JavaVariable varArgVariable = new JavaVariable();
                varArgVariable.setType(getTypeName(lastContext.typeType()) + "[]"); // varargs are treated as arrays
                varArgVariable.setName(lastContext.variableDeclaratorId().IDENTIFIER().getText());

                varArgVariable.setStartLine(ctx.start.getLine());
                varArgVariable.setEndLine(ctx.stop.getLine());

                codeUnitsEncountered.push(varArgVariable);
                lastContext.variableModifier().forEach(this::visit);
                codeUnitsEncountered.pop();
            }
        }

        return null;
    }

    @Override
    public JavaCodeUnit visitEnumDeclaration(EnumDeclarationContext ctx) {
        JavaType type = new JavaType();

        // All enums implicitly extend java.lang.Enum
        type.setParentClass("java/lang/Enum");

        type.setType(ctx.ENUM().getText());

        type.setStartLine(ctx.start.getLine());
        type.setEndLine(ctx.stop.getLine());

        String enumName = getName(ctx.IDENTIFIER());

        type.setName(enumName);

        type.getImplementsInterfaces().addAll(getImplementedInterfaces(ctx.typeList()));

        for (EnumConstantContext constantContext : ctx.enumConstants().enumConstant()) {
            JavaVariable enumValue = new JavaVariable();
            enumValue.setType(enumName);

            enumValue.setStartLine(ctx.start.getLine());
            enumValue.setEndLine(ctx.stop.getLine());

            enumValue.setName(constantContext.IDENTIFIER().getText());

            // we have no modifiers, so just set the annotations directly
            for(AnnotationContext annotationContext : constantContext.annotation()) {
                codeUnitsEncountered.push(enumValue);
                visit(annotationContext);
                codeUnitsEncountered.pop();
            }

            type.getFields().add(enumValue);
        }

        types.add(type);

        codeUnitsEncountered.push(type);

        visit(ctx.enumBodyDeclarations());

        codeUnitsEncountered.pop();

        return type;
    }

    @Override
    public JavaCodeUnit visitPackageDeclaration(JavaDocumentationParser.PackageDeclarationContext ctx) {
        // Since the expected database is elasticsearch, this fixes an issues with
        // mapping explosions. Elasticsearch internally remodels objects, flattening
        // names to be of the form "outer_object.inner_object.field". Apparently
        // inserting names with the '.' character causes elasticsearch to insert these
        // as subobjects, and causes elasticsearch to generate a LOT of mappings for only
        // a few names. It's also an issue when parsing imports.
        packageName = ctx.qualifiedName().getText().replaceAll("\\.", "/");
        this.starImports.add(packageName); // being in the same package acts like a star import
        this.starImports.add("java/lang"); // java.lang does not need to be imported either
        return super.visitPackageDeclaration(ctx);
    }

    @Override
    public JavaCodeUnit visitImportDeclaration(ImportDeclarationContext ctx) {
        // see the above comment explaining why '.' is replaced with '/'.
        JavaDocumentationParser.QualifiedNameContext fqnContext = ctx.qualifiedName();
        String fqn = fqnContext.getText().replaceAll("\\.", "/");
        int totalIdentifiers = fqnContext.IDENTIFIER().size();
        String className = fqnContext.IDENTIFIER(totalIdentifiers - 1).getText();

        if (ctx.MUL() == null) { // if its not a star import
            imports.put(className, fqn);
        } else {
            // it is a star import
            starImports.add(className);
        }

        return super.visitImportDeclaration(ctx);
    }

    public JavaCodeUnit visitVariableModifier(VariableModifierContext context) {
        JavaCodeUnit unit = codeUnitsEncountered.peek();
        if (context.FINAL() != null) {
            unit.getModifiers().add(context.FINAL().getText());
        } else if (context.annotation() != null) {
            visit(context.annotation());
        }

        return null;
    }

    public JavaCodeUnit visitModifier(ModifierContext context) {
        JavaCodeUnit unit = codeUnitsEncountered.peek();
        List<String> keywordModifiers = unit.getModifiers();

        if (context.classOrInterfaceModifier() != null) {
            visit(context.classOrInterfaceModifier());
        } else { //its a keyword modifier
            for (int name : KEYWORD_MODIFIERS) {
                List<TerminalNode> nodes = context.getTokens(name);
                for (TerminalNode node : nodes) {
                    keywordModifiers.add(node.getText());
                }
            }
        }

        return null;
    }

    @Override
    public JavaCodeUnit visitClassOrInterfaceModifier(ClassOrInterfaceModifierContext context) {
        JavaCodeUnit unit = codeUnitsEncountered.peek();
        List<String> keywordModifiers = unit.getModifiers();
        if (context.annotation() != null) { // if it's an annotation modifier
            visit(context.annotation());
        } else { // its a keyword modifier
            for (int name : KEYWORD_MODIFIERS) {
                List<TerminalNode> nodes = context.getTokens(name);
                for (TerminalNode node : nodes) {
                    keywordModifiers.add(node.getText());
                }
            }
        }

        return null;
    }

    @Override
    public JavaCodeUnit visitAnnotation(AnnotationContext context) {
        Map<String, Map<String, String>> annotations = codeUnitsEncountered.peek().getAnnotations();

        String name = getFullyQualifiedName(context.qualifiedName());

        //todo future: determine if parsing annotations passed to annotations is worth the effort or not
        // the answer is probably yes, but I'd like to see some use for it first

        if (context.elementValue() != null) { // single value
            HashMap<String, String> innerMap = new HashMap<>();
            innerMap.put("#body", context.elementValue().getText());
            annotations.put(name, innerMap);
        } else if (context.elementValuePairs() != null) { //multiple value pairs
            Map<String, String> annotationParameters = new HashMap<>();
            for (ElementValuePairContext pairContext : context.elementValuePairs().elementValuePair()){
                annotationParameters.put(pairContext.IDENTIFIER().getText(), pairContext.elementValue().getText());
            }
            annotations.put(name, annotationParameters);
        } else { // no parameters at all
            annotations.put(name, new HashMap<>());
        }

        return null;
    }

    private String getFullyQualifiedName(JavaDocumentationParser.QualifiedNameContext context) {
        if (context.IDENTIFIER().size() > 1) {
            return context.getText().replace('.', '/');
        } else {
            String name = context.IDENTIFIER(0).getText();
            return resolveName(name);
        }
    }

    private String resolveName(String name) {
        if (imports.containsKey(name)) {
            name = imports.get(name);
        } else {
            for (String prefix : starImports) {
                if (hierarchy.containsType(prefix + "/" + name)) {
                    name = prefix + "/" + name;
                    break; // this is gross but we don't need to continue
                }
            }
        }
        return name.replace('.', '/');
    }

    private String getTypeName(TypeTypeContext context) {
        if (context == null) {
            return "java/lang/Object";
        }

        StringBuilder name = new StringBuilder();
        if (context.classOrInterfaceType() != null) {
            ClassOrInterfaceTypeContext typeContext = context.classOrInterfaceType();
            // Resolve this name into a qualified name. We always need to resolve the first name
            // (since a qualified name could be an inner class of an imported name). After that,
            // everything can be assumed to be a qualified name.
            List<TerminalNode> identifiers = typeContext.IDENTIFIER();
            name.append(resolveName(identifiers.get(0).getText()));
            for (int i = 1; i < identifiers.size(); i++) {
                name.append('/');
                name.append(identifiers.get(i).getText());
            }
        } else if (context.primitiveType() != null) { // its a primitive type
            PrimitiveTypeContext typeContext = context.primitiveType();
            name.append(typeContext.getChild(0).getText());
        }

        context.LBRACK().forEach(e -> name.append("[]"));

        return name.toString();
    }

    @SuppressWarnings("unchecked") // the empty list is empty, so we don't care id
    @Deprecated
    private List<String> getImplementedInterfaces(TypeListContext context) {
        if (context == null) {
            return (List<String>) Collections.EMPTY_LIST;
        }
        List<String> impls = new ArrayList<>(1);

        for (TypeTypeContext typeContext : context.typeType()){
            impls.add(getTypeName(typeContext));
        }

        return impls;
    }

    private String getName(TerminalNode identifier) {
        // find the most recent type, and add its name onto the end
        for (JavaCodeUnit unit : codeUnitsEncountered) {
            if (unit instanceof JavaType) {
                String fullName = unit.getName() + "/" + identifier.getText();
                imports.put(identifier.getText(), fullName);
                hierarchy.addType(fullName);
                return fullName;
            }
        }

        return identifier.getText();
    }

    @Override
    public JavaCodeUnit visitDocumentation(DocumentationContext ctx) {
        // because this is technically legal, we have to allow for multiple in a row.
        // the javadoc tool only gets the last one though, so we'll continue that precedent
        // We can't handle this with the error strategy though, as it'll pick the first
        // rather than the last.
        String javadocComment = ctx.JAVADOC_COMMENT().get(ctx.JAVADOC_COMMENT().size() - 1).getText();

        JavadocLexer lexer = new JavadocLexer(CharStreams.fromString(javadocComment));

        // why is this on by default?
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavadocParser parser = new JavadocParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        parser.removeErrorListener(ConsoleErrorListener.INSTANCE);

        JavaCodeUnit unit = codeUnitsEncountered.peek();
        try {
            if (unit != null && unit.getDocumentation() == null)
                unit.setDocumentation(new DocumentationVisitor().visit(parser.documentation()));
        } catch (ParseCancellationException e) {
            // parsing failed. We should warn, but we don't have access to the loggers. Oh no!
        }

        return null;
    }
}
