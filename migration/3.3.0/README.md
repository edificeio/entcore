# Migration to 3.3.0

This guide describes migration steps to upgrade entcore framework databases from 3.2.0 to 3.3.0.
The 3.3.0 migration concerne the **Workspace** application (contribution into folders).

This migration has 3 scripts:
- *changeRights.cypher* : update *Action* in NEO4J database.
- *workspaceFolders.js* : update the *documents* collection in Mongo database
- *workspaceIndex.js* : udate indexes of the *documents* collection in Mongo database


1. *changeRights.cypher*

This scripts must be executed before starting vertx with the 3.3.0 entcore's version.
This scripts update *Action* names and add missing *Action* related to new fonctionnalities.

If the script is runned after vertx restart, entcore will create duplicate actions (because it find that new action names are missing).

Run the script using the following command:

```
neo4j-shell -file changeRights.cypher 
```

2. *workspaceFolder.js*

This script could be executed before or after starting vertx with the 3.3.0 entcore framework.
It computes the new folder's tree structure and add new fields in the MongoDB *documents* collection.

This script does not override existing fields, so it could be possible (if needed) to downgrade from entcore 3.3.0 to 3.2.0.


Run the script using the following command:

```
mongo DB_NAME workspaceFolders.js
```

The script display a reports as follow:

```
report;owner;shared;deleted
folder-before;100;100;100
folder-after;100;100;100
folder-ignored;100;100;100
file-before;100;100;100
file-after;100;100;100
file-ignored;100;"100;100
errors;10

```

This reports compute how many files (or folders) are deleted, shared, or not shared (owner) before and after the migration script. So, numbers before and after should be equals. 

Also, this report display how many files (or folders) are ignored. The script ignore files only if it considers that they already have been upgrade (so if you run this script multiple times, some documents should be ignored).

Finally, the report display how many errors it has found. Generally, errors are orphan files (or folders). That means that the file has a parent in the old hierarchy but the script has not found his parent is the new hierarchy (so it will put the file at root of the tree in the new structure). It could be possible to have 1% of files in errors (because of corrupted data).

3. *workspaceIndex.js*

This script remove old indexes and add new.
It could be executed after *workspaceFolders.js*.


Run the script using the following command:

```
mongo DB_NAME workspaceIndex.js
```