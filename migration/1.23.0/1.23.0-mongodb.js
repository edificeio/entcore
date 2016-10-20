db.flash_messages.createIndex( { "markedAsRead": 1 } );
db.flash_messages.createIndex( { "modified": -1 } );
db.documentsRevisions.createIndex({"owner":1});
