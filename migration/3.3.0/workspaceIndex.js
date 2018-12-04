db.documents.createIndex({ name: "text", ownerName: "text" }, { background: true });
db.documentsRevisions.createIndex({ name: "text", ownerName: "text" }, { background: true });
db.documents.createIndex({eParent:1},{name:"idx_eparent"})
db.documents.createIndex({"inheritedShares.userId":1},{name:"idx_inherited_userid"})
db.documents.createIndex({"inheritedShares.groupId":1},{name:"idx_inherited_groupid"})
db.documents.dropIndex("shared.userId_1");
db.documents.dropIndex("shared.groupId_1");
db.documents.dropIndex("old-folder_1");
db.documents.dropIndex("folder_1");