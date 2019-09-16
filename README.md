# Weasel: Finding Code

[![Build Status](https://dev.azure.com/wsrogers3/wsrogers3/_apis/build/status/MagicalAsh.Weasel?branchName=master)](https://dev.azure.com/wsrogers3/wsrogers3/_build/latest?definitionId=1&branchName=master)

Weasel is a tool to search the semantics of code. 

## Architecture
Weasel consists of five parts: An extensible indexing service, a search service,
 an extensible provider service, an ElasticSearch database, and a front-end UI.

### Code Index Service
The Code Index Service is responsible for taking code from a repository and 
indexing into the ElasticSearch database. This service is responsible for 
parsing code, analyzing the semantics of the provided code, and entering it into
the database in a scalable and searchable method.

The semantics parsing would be provided through plugins, allowing new 
semantics parsers to be added without requiring service updates. 

Core Parser Plugins:
* Regular Expression Parser
* Java Semantic Parser

### Search Service
The Search Service is scalable service responsible for providing the code in a 
searchable method through a REST API. This would be done through a domain 
specific language to be determined.

### Provider Service
The provider service is a method of providing code to be indexed in an 
extensible and updatable. The provider would dynamically load available 
providers as plugins and would submit code to be indexed to the Code Index 
Service. Additionally, the provider service would be capable of dynamically 
re-submitting code to be indexed. This would be done through a REST Api 
paired with usage of Git's post-receive hooks.

Core Provider Plugins:
* File System Provider
* Git Repository Provider
* Maven Repository Provider

### UI
The UI provides a convenient method of searching code repositories without 
needing to learn the search domain specific language. The Weasel UI would 
provide a convenient, graphical interface that can be accessed from the web.
The Weasel UI would also be accessible from behind a proxy, allowing the
entire Weasel ecosystem to run natively in containers. 

## Technology
Weasel is based on Spring Boot, and uses many of the Spring Framework packages.
Weasel also uses Elasticsearch, which is a document database based on searching
documents. Being Web-native, Weasel is available both as a native Jarfile and 
as a Docker container. 

In addition to the core service technology above, Weasel plugins use technology
that is commonly used such as Git, Regular Expressions, Parsers (Either ANTLR 
or a language specific parser), among others.

## Example Query*

Get implementations of the java.util.EventListener interface where the class
has a method called "foo" taking an object as a parameter.
```json
{
    "get": "classes",
    "where": [
        {
            "impl": "java.util.EventListener"
        },
        {
            "exists": "foo(java.lang.Object)",
            "type": "method"
        }
    ]
}
```

Get methods using a List object.
```json
{
    "get": "methods",
    "where": [
        {
            "used": "java.lang.List"
        }
    ]
}
```

Get usages of System.exit() in classes that implement java.lang.Runnable.
```json
{
    "get": "methods",
    "where": [
        {
            "used": "java.lang.System::exit"
        },
        {
            "impl": "java.lang.Runnable",
            "scope": "enclosing_class"
        }
    ]
}
```

\* NOTE: the DSL has not yet been determined. These are examples of 
what it might look like.
