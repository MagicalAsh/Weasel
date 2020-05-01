# File System Provider Plugin
The File System Provider plugin takes a file structure on disk and provides 
it as a repository.

# Output JSON schema
```json
{
   "properties" : {
      "source" : {
         "items" : {
            "type" : "object",
            "properties" : {
               "obtained_by" : {
                  "type" : "string"
               },
               "file_contents" : {
                  "items" : {
                     "type" : "string"
                  },
                  "type" : "array"
               },
               "content_location" : {
                  "type" : "string"
               },
               "line_count" : {
                  "type" : "integer"
               },
               "accessed" : {
                  "type" : "string"
               }
            }
         },
         "type" : "array"
      },
      "provided_by" : {
         "type" : "string"
      }
   }
}
```