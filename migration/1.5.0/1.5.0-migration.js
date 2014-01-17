db.documents.find({"shared.0" : { "$exists" : true }}, {"_id":1, "shared":1}).forEach(function(doc) {
  var string = JSON.stringify(doc);
  var obj = JSON.parse(string.replace(/edu\-one\-core/g, 'org-entcore'));
  db.documents.update({"_id" : doc._id}, { $set : { "shared" : obj.shared}});
});

db.blogs.find({"shared.0" : { "$exists" : true }}, {"_id":1, "shared":1}).forEach(function(doc) {
  var string = JSON.stringify(doc);
  var obj = JSON.parse(string.replace(/edu\-one\-core/g, 'org-entcore'));
  db.blogs.update({"_id" : doc._id}, { $set : { "shared" : obj.shared}});
});

