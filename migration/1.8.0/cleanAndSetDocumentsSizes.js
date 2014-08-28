var usedFiles = [];
db.documents.find({"file" : { "$exists" : true}}, { "file" : 1, "thumbnails" : 1 }).forEach(function(doc) {
  usedFiles.push(doc.file);
  for (var index in doc.thumbnails) {
    if (doc.thumbnails.hasOwnProperty(index)) {
      usedFiles.push(doc.thumbnails[index]);
    }
  }
});
db.racks.find({"file" : { "$exists" : true}}, { "file" : 1}).forEach(function(doc) {
  usedFiles.push(doc.file);
});

db.fs.chunks.remove({ "files_id" : { "$not" : { "$in" : usedFiles }}});
db.fs.files.remove({ "_id" : { "$not" : { "$in" : usedFiles }}});

db.documents.find({"file" : { "$exists" : true}, "metadata.size" : 0 }, { "file" : 1}).forEach(function(doc) {
  var f = db.fs.files.findOne({"_id": doc.file }, {"length" : 1});
  db.documents.update({ "_id" : doc._id}, { "$set" : { "metadata.size" : f.length.toNumber()}});
});
db.racks.find({"file" : { "$exists" : true}, "metadata.size" : 0 }, { "file" : 1}).forEach(function(doc) {
  var f = db.fs.files.findOne({"_id": doc.file }, {"length" : 1});
  db.racks.update({ "_id" : doc._id}, { "$set" : { "metadata.size" : f.length.toNumber()}});
});

