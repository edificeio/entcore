db.documents.createIndex({ name: "text", ownerName: "text" }, { background: true });
db.documentsRevisions.createIndex({ name: "text", ownerName: "text" }, { background: true });