# Structural Parser Plugin
This plugin is designed to take in Java code and output a structured java 
object, intended to be converted to JSON.

# Notes
There were a couple of compromises made when developing this plugin, especially
so when regarding the outputted data.
  
Some of these include:
   - Constructors are considered methods with the name "#constructor"
   - Annotation members are treated as methods.
   - Fully qualified names are separated using the '/' character rather than 
    the '.' character.
   - Annotation members defined without specifying the member name are given 
    the name "#body".
   - Classes imported using a star import or classes imported from the same 
    package will not have their fully qualified name discovered.  
   - Array types are not always discovered, especially when using
    "Type name[]" style declarations.
   - Documentation attached to code is output, although incomplete.
   
# Future Directions
There are several things that could be improved upon in this plugin. Lack of
support for star imports is a major one, and could be solved by keeping a list
of discovered classes. 

# Output JSON/Elasticsearch Schema
```json
{
    "properties": {
      "content_location": {
        "type": "text"
      },
      "indexed_by": {
        "type": "keyword"
      },
      "metadata": {
        "type": "object"
      },
      "parsed_result": {
        "type": "object",
        "properties": {
          "documentation": {
            "type": "object"
          },
          "modifiers": {
            "type": "keyword"
          },
          "annotations": {
            "type": "object"
          },
          "type": {
            "type": "keyword"
          },
          "parentClass": {
            "type": "keyword"
          },
          "implementsInterfaces": {
            "type": "keyword"
          },
          "fields": {
            "type": "nested",
            "properties": {
              "type": {
                "type": "keyword"
              },
              "documentation": {
                "type": "object"
              },
              "modifiers": {
                "type": "keyword"
              },
              "annotations": {
                "type": "object"
              },
              "start_line": {
                "type": "integer"
              },
              "end_line": {
                "type": "integer"
              }
            }
          },
          "methods": {
            "type": "nested",
            "properties": {
              "documentation": {
                "type": "object"
              },
              "modifiers": {
                "type": "keyword"
              },
              "annotations": {
                "type": "object"
              },
              "returnType": {
                "type": "keyword"
              },
              "parameters": {
                "type": "object"
              }
            }
          }
        }
      }
    }
}
```