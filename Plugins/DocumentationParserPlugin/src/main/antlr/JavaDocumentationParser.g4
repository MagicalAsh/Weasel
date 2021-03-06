parser grammar JavaDocumentationParser;

options { tokenVocab=JavaDocumentationLexer; }

documentation
	: JAVADOC_COMMENT+
	;

compilationUnit
    : packageDeclaration? importDeclaration* typeDeclaration* EOF
    ;

packageDeclaration
    : documentation? annotation* PACKAGE qualifiedName SEMI
    ;

importDeclaration
    : documentation? IMPORT STATIC? qualifiedName (DOT MUL)? SEMI
    ;

typeDeclaration
    : documentation?
      classOrInterfaceModifier*
      (classDeclaration | enumDeclaration | interfaceDeclaration | annotationTypeDeclaration)
    | SEMI
    ;

modifier
    : classOrInterfaceModifier
    | NATIVE
    | SYNCHRONIZED
    | TRANSIENT
    | VOLATILE
    ;

classOrInterfaceModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | STATIC
    | ABSTRACT
    | FINAL    // FINAL for class only -- does not apply to interfaces
    | STRICTFP
    ;

variableModifier
    : FINAL
    | annotation
    ;

classDeclaration
    : CLASS IDENTIFIER typeParameters?
      (EXTENDS typeType)?
      (IMPLEMENTS typeList)?
      classBody
    ;

typeParameters
    : LT typeParameter (COMMA typeParameter)* GT
    ;

typeParameter
    : annotation* IDENTIFIER (EXTENDS typeBound)?
    ;

typeBound
    : typeType (BITAND typeType)*
    ;

enumDeclaration
    : ENUM IDENTIFIER (IMPLEMENTS typeList)? LBRACE enumConstants? COMMA? enumBodyDeclarations? RBRACE
    ;

enumConstants
    : enumConstant (COMMA enumConstant)*
    ;

enumConstant
    : annotation* documentation? IDENTIFIER arguments? classBody?
    ;

enumBodyDeclarations
    : SEMI classBodyDeclaration*
    ;

interfaceDeclaration
    : INTERFACE IDENTIFIER typeParameters? (EXTENDS typeList)? interfaceBody
    ;

classBody
    : LBRACE classBodyDeclaration* RBRACE
    ;

interfaceBody
    : LBRACE interfaceBodyDeclaration* RBRACE
    ;

// it's possible for the last element of a class body to be a doc comment.
// it's not particularly useful, but it's not disallowed.
classBodyDeclaration
    : SEMI
    | STATIC? block
    | documentation? modifier* memberDeclaration
    ;

memberDeclaration
    : methodDeclaration
    | genericMethodDeclaration
    | fieldDeclaration
    | constructorDeclaration
    | genericConstructorDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration
    : typeTypeOrVoid IDENTIFIER formalParameters (LBRACK RBRACK)*
      (THROWS qualifiedNameList)?
      methodBody
    ;

methodBody
    : block
    | SEMI
    ;

typeTypeOrVoid
    : typeType
    | VOID
    ;

genericMethodDeclaration
    : typeParameters methodDeclaration
    ;

genericConstructorDeclaration
    : typeParameters constructorDeclaration
    ;

constructorDeclaration
    : IDENTIFIER formalParameters (THROWS qualifiedNameList)? constructorBody=block
    ;

fieldDeclaration
    : typeType variableDeclarators SEMI
    ;

interfaceBodyDeclaration
    : documentation? modifier* interfaceMemberDeclaration
    | documentation
    | SEMI
    ;

interfaceMemberDeclaration
    : constDeclaration
    | interfaceMethodDeclaration
    | genericInterfaceMethodDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

constDeclaration
    : typeType constantDeclarator (COMMA constantDeclarator)* SEMI
    ;

constantDeclarator
    : IDENTIFIER (LBRACK RBRACK)* ASSIGN variableInitializer
    ;

// see matching of [] comment in methodDeclaratorRest
// methodBody from Java8
interfaceMethodDeclaration
    : interfaceMethodModifier* (typeTypeOrVoid | typeParameters annotation* typeTypeOrVoid)
      IDENTIFIER formalParameters (LBRACK RBRACK)* (THROWS qualifiedNameList)? methodBody
    ;

// Java8
interfaceMethodModifier
    : annotation
    | PUBLIC
    | ABSTRACT
    | DEFAULT
    | STATIC
    | STRICTFP
    ;

genericInterfaceMethodDeclaration
    : typeParameters interfaceMethodDeclaration
    ;

variableDeclarators
    : variableDeclarator (COMMA variableDeclarator)*
    ;

variableDeclarator
    : variableDeclaratorId (ASSIGN variableInitializer)?
    ;

variableDeclaratorId
    : IDENTIFIER (LBRACK RBRACK)*
    ;

variableInitializer
    : arrayInitializer
    | expression
    ;

arrayInitializer
    : LBRACE (variableInitializer (COMMA variableInitializer)* (COMMA)? )? RBRACE
    ;

classOrInterfaceType
    : IDENTIFIER typeArguments? (DOT IDENTIFIER typeArguments?)*
    ;

typeArgument
    : typeType
    | QUESTION ((EXTENDS | SUPER) typeType)?
    ;

qualifiedNameList
    : qualifiedName (COMMA qualifiedName)*
    ;

formalParameters
    : LPAREN formalParameterList? RPAREN
    ;

formalParameterList
    : formalParameter (COMMA formalParameter)* (COMMA lastFormalParameter)?
    | lastFormalParameter
    ;

formalParameter
    : variableModifier* typeType variableDeclaratorId
    ;

lastFormalParameter
    : variableModifier* typeType ELLIPSIS variableDeclaratorId
    ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

literal
    : integerLiteral
    | floatLiteral
    | CHAR_LITERAL
    | STRING_LITERAL
    | BOOL_LITERAL
    | NULL_LITERAL
    ;

integerLiteral
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    ;

floatLiteral
    : FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    ;

// ANNOTATIONS

annotation
    : AT qualifiedName (LPAREN ( elementValuePairs | elementValue )? RPAREN)?
    ;

elementValuePairs
    : elementValuePair (COMMA elementValuePair)*
    ;

elementValuePair
    : IDENTIFIER ASSIGN elementValue
    ;

elementValue
    : expression
    | annotation
    | elementValueArrayInitializer
    ;

elementValueArrayInitializer
    : LBRACE (elementValue (COMMA elementValue)*)? (COMMA)? RBRACE
    ;

annotationTypeDeclaration
    : AT INTERFACE IDENTIFIER annotationTypeBody
    ;

annotationTypeBody
    : LBRACE (annotationTypeElementDeclaration)* RBRACE
    ;

annotationTypeElementDeclaration
    : documentation? modifier* annotationTypeElementRest
    | documentation
    | SEMI // this is not allowed by the grammar, but apparently allowed by the actual compiler
    ;

annotationTypeElementRest
    : typeType annotationMethodOrConstantRest SEMI
    | classDeclaration SEMI?
    | interfaceDeclaration SEMI?
    | enumDeclaration SEMI?
    | annotationTypeDeclaration SEMI?
    ;

annotationMethodOrConstantRest
    : annotationMethodRest
    | annotationConstantRest
    ;

annotationMethodRest
    : IDENTIFIER LPAREN RPAREN defaultValue?
    ;

annotationConstantRest
    : variableDeclarators
    ;

defaultValue
    : DEFAULT elementValue
    ;

// STATEMENTS / BLOCKS

block
    : LBRACE blockStatement* RBRACE
    ;

blockStatement
    : localVariableDeclaration SEMI
    | statement
    | localTypeDeclaration
    ;

localVariableDeclaration
    : variableModifier* typeType variableDeclarators
    ;

localTypeDeclaration
    : classOrInterfaceModifier*
      (classDeclaration | interfaceDeclaration)
    | SEMI
    ;

// while documentation is not a statement, I can't figure out how to
// get rid of them. Assigning them to a different channel appears to
// make them impossible to get within a visitor. So now I need to add
// them in places where it would change the parse path should the token
// appear. Ugh.
statement
    : blockLabel=block
    | ASSERT expression (COLON expression)? SEMI
    | IF parExpression statement (documentation? ELSE statement)?
    | FOR LPAREN forControl RPAREN statement
    | WHILE parExpression statement
    | DO statement WHILE parExpression SEMI
    | TRY block (catchClause+ finallyBlock? | finallyBlock)
    | TRY resourceSpecification block catchClause* finallyBlock?
    | SWITCH parExpression LBRACE switchBlockStatementGroup* switchLabel* RBRACE
    | SYNCHRONIZED parExpression block
    | RETURN expression? SEMI
    | THROW expression SEMI
    | BREAK IDENTIFIER? SEMI
    | CONTINUE IDENTIFIER? SEMI
    | SEMI
    | statementExpression=expression SEMI
    | identifierLabel=IDENTIFIER COLON statement
    | documentation
    ;

catchClause
    : documentation? CATCH LPAREN variableModifier* catchType IDENTIFIER RPAREN block
    ;

catchType
    : qualifiedName (BITOR qualifiedName)*
    ;

finallyBlock
    : documentation? FINALLY block
    ;

resourceSpecification
    : LPAREN resources SEMI? RPAREN
    ;

resources
    : resource (SEMI resource)*
    ;


// In Java 9(?) try with resources added the ability to try based on
// existing resources, meaning that we need to add the ability to match
// just an identifier
resource
    : variableModifier* classOrInterfaceType variableDeclaratorId ASSIGN expression
    | IDENTIFIER
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : switchLabel+ blockStatement+
    ;

switchLabel
    : CASE (constantExpression=expression | enumConstantName=IDENTIFIER) COLON
    | DEFAULT COLON
    ;

forControl
    : enhancedForControl
    | forInit? SEMI expression? SEMI forUpdate=expressionList?
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

enhancedForControl
    : variableModifier* typeType variableDeclaratorId COLON expression
    ;

// EXPRESSIONS

parExpression
    : LPAREN expression RPAREN
    ;

expressionList
    : expression (COMMA expression)*
    ;

methodCall
    : IDENTIFIER LPAREN expressionList? RPAREN
    | THIS LPAREN expressionList? RPAREN
    | SUPER LPAREN expressionList? RPAREN
    ;

expression
    : primary
    | expression bop=DOT
      ( IDENTIFIER
      | methodCall
      | THIS
      | NEW nonWildcardTypeArguments? innerCreator
      | SUPER superSuffix
      | explicitGenericInvocation
      )
    | expression LBRACK expression RBRACK
    | methodCall
    | NEW creator
    | LPAREN typeType (BITAND typeType)* RPAREN expression
    | expression postfix=(INC | DEC)
    | prefix=(ADD | SUB | INC | DEC) expression
    | prefix=(TILDE | BANG) expression
    | expression bop=(MUL | DIV | MOD) expression
    | expression bop=(ADD | SUB) expression
    | expression (LT LT | GT GT GT | GT GT) expression
    | expression bop=(LE | GE | GT | LT) expression
    | expression bop=INSTANCEOF typeType
    | expression bop=(EQUAL | NOTEQUAL) expression
    | expression bop=BITAND expression
    | expression bop=CARET expression
    | expression bop=BITOR expression
    | expression bop=AND expression
    | expression bop=OR expression
    | <assoc=right> expression bop=QUESTION expression COLON expression
    | <assoc=right> expression
      bop=(ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | AND_ASSIGN | OR_ASSIGN | XOR_ASSIGN | RSHIFT_ASSIGN | URSHIFT_ASSIGN | LSHIFT_ASSIGN | MOD_ASSIGN)
      expression
    | lambdaExpression // Java8

    // Java 8 methodReference
    | expression COLONCOLON typeArguments? IDENTIFIER
    | typeType COLONCOLON (typeArguments? IDENTIFIER | NEW)
    | classType COLONCOLON typeArguments? NEW
    ;

// Java8
lambdaExpression
    : lambdaParameters ARROW lambdaBody
    ;

// Java8
lambdaParameters
    : IDENTIFIER
    | LPAREN formalParameterList? RPAREN
    | LPAREN IDENTIFIER (COMMA IDENTIFIER)* RPAREN
    ;

// Java8
lambdaBody
    : expression
    | block
    ;

primary
    : LPAREN expression RPAREN
    | THIS
    | SUPER
    | literal
    | IDENTIFIER
    | typeTypeOrVoid DOT CLASS
    | nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
    ;

classType
    : (classOrInterfaceType DOT)? annotation* IDENTIFIER typeArguments?
    ;

creator
    : nonWildcardTypeArguments createdName classCreatorRest
    | createdName (arrayCreatorRest | classCreatorRest)
    ;

createdName
    : IDENTIFIER typeArgumentsOrDiamond? (DOT IDENTIFIER typeArgumentsOrDiamond?)*
    | primitiveType
    ;

innerCreator
    : IDENTIFIER nonWildcardTypeArgumentsOrDiamond? classCreatorRest
    ;

arrayCreatorRest
    : LBRACK (RBRACK (LBRACK RBRACK)* arrayInitializer | expression RBRACK (LBRACK expression RBRACK)* (LBRACK RBRACK)*)
    ;

classCreatorRest
    : arguments classBody?
    ;

explicitGenericInvocation
    : nonWildcardTypeArguments explicitGenericInvocationSuffix
    ;

typeArgumentsOrDiamond
    : LT GT
    | typeArguments
    ;

nonWildcardTypeArgumentsOrDiamond
    : LT GT
    | nonWildcardTypeArguments
    ;

nonWildcardTypeArguments
    : LT typeList GT
    ;

typeList
    : typeType (COMMA typeType)*
    ;

typeType
    : annotation? (classOrInterfaceType | primitiveType) (LBRACK RBRACK)*
    ;

primitiveType
    : BOOLEAN
    | CHAR
    | BYTE
    | SHORT
    | INT
    | LONG
    | FLOAT
    | DOUBLE
    ;

typeArguments
    : LT typeArgument (COMMA typeArgument)* GT
    ;

superSuffix
    : arguments
    | DOT IDENTIFIER arguments?
    ;

explicitGenericInvocationSuffix
    : SUPER superSuffix
    | IDENTIFIER arguments
    ;

arguments
    : LPAREN expressionList? RPAREN
    ;
