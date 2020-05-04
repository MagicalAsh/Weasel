# Plugin Interfaces and Representations
This project serves as a dependency to pull the interfaces for Provider and
Index plugins, and as a way to standardize representations of the output
of plugins.

# Provider Schema
```json
{
    "properties": {
        "metadata": {
            "type": "object"
        },
        "obtained_by": {
            "type": "string"
        },
        "accessed": {
            "type": "string"
        },
        "content_location": {
            "type": "string"
        },
        "file_contents": {
            "type": "array",
            "items": {
                "type": "string"
            }
        },
        "line_count": {
            "type": "number"
        }
    }
}
```

# Index Schema