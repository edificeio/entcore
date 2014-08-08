db.documents.update({}, {$set : { shared : []}}, { multi:true });
db.blogs.update({}, {$set : { shared : []}}, { multi:true });
