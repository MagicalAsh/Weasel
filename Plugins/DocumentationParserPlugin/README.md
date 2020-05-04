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
The provided schema is provided using Elasticsearch types. In order to convert
to a standard JSON schema type, switch "keyword" and "text" to "string", 
"long" to "integer", and "nested" to "object".

```json
{
    "properties":{
        "file_location":{
            "type":"keyword"
        },
        "indexed_by":{
            "type":"keyword"
        },
        "metadata":{
            "type":"object"
        },
        "parsed_result":{
            "properties":{
                "annotations":{
                    "type":"object"
                },
                "documentation":{
                    "properties":{
                        "body":{
                            "type":"text"
                        },
                        "tags":{
                            "type":"object"
                        }
                    }
                },
                "end_line":{
                    "type":"long"
                },
                "fields":{
                    "type":"nested",
                    "properties":{
                        "annotations":{
                            "type":"object"
                        },
                        "documentation":{
                            "properties":{
                                "body":{
                                    "type":"text"
                                },
                                "tags":{
                                    "type":"object"
                                }
                            }
                        },
                        "end_line":{
                            "type":"integer"
                        },
                        "modifiers":{
                            "type":"keyword"
                        },
                        "name":{
                            "type":"keyword"
                        },
                        "start_line":{
                            "type":"integer"
                        },
                        "type":{
                            "type":"keyword"
                        }
                    }
                },
                "implementsInterfaces":{
                    "type":"keyword"
                },
                "methods":{
                    "type":"nested",
                    "properties":{
                        "annotations":{
                            "type":"object"
                        },
                        "documentation":{
                            "type":"object"
                        },
                        "end_line":{
                            "type":"long"
                        },
                        "modifiers":{
                            "type":"keyword"
                        },
                        "name":{
                            "type":"keyword"
                        },
                        "parameters":{
                            "properties":{
                                "end_line":{
                                    "type":"long"
                                },
                                "name":{
                                    "type":"keyword"
                                }
                            },
                            "start_line":{
                                "type":"long"
                            },
                            "type":{
                                "type":"keyword"
                            }
                        }
                    },
                    "returnType":{
                        "type":"keyword"
                    },
                    "start_line":{
                        "type":"long"
                    }
                }
            },
            "modifiers":{
                "type":"keyword"
            },
            "name":{
                "type":"keyword"
            },
            "parentClass":{
                "type":"keyword"
            },
            "start_line":{
                "type":"long"
            },
            "type":{
                "type":"keyword"
            }
        }
    }
}
```