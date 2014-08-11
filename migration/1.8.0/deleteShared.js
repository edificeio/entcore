db.documents.update({}, {$unset : { "old_shared" : "" }}, { multi:true });
db.documents.update({}, {$rename : { "shared" : "old_shared" }}, { multi:true });
db.documents.update({}, {$set : { shared : []}}, { multi:true });
db.blogs.update({}, {$set : { shared : []}}, { multi:true });
