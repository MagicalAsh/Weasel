# Git Provider Plugin
The Git Provider plugin takes a Git repository, whether on disk or
a remote repository, and provides the repository.

# Output JSON schema
```json
{
   "properties" : {
      "source" : {
         "items" : {
            "type" : "object",
            "properties" : {
               "commit_id" : {
                  "type" : "string"
               },
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
               "branch_name" : {
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