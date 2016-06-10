db.documents.find({"modified": {$regex : /\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d+/}}, {"_id":1, "modified":1}).forEach(function(doc) {
  var date = doc.modified.replace(/:(\d{2})\.(\d+)$/, '.$1.$2');
  db.documents.update({"_id" : doc._id}, { $set : { "modified" : date}});
});

