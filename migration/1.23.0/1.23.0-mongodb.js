db.documentsRevisions.createIndex({"owner":1});
db.documents.find({"file" : {"$regex" : ".srv.*"}}, {"_id":1, "file":1}).forEach(function(doc) {
  var fileId = doc.file.split("/").slice(-1)[0];
  db.documents.update({"_id" : doc._id}, { $set : { "file" : fileId }});
});
