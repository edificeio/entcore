print("begin transaction");
db.documents.distinct("owner", { "file" : { "$exists" : true}}).forEach(function(user) {
  var usedSize = 0;
  print("MATCH (u:UserBook { userid : '" + user + "'}) ");
  db.documents.find({"owner" : user, "file" : { "$exists" : true}}, { "metadata" : 1}).forEach(function(doc) {
    usedSize = usedSize + doc.metadata.size;
  });
  print("SET u.storage = coalesce(u.storage, 0) + " + usedSize + ";");
});
db.racks.distinct("to", { "file" : { "$exists" : true}}).forEach(function(user) {
  var usedSize = 0;
  print("MATCH (u:UserBook { userid : '" + user + "'}) ");
  db.racks.find({"to" : user, "file" : { "$exists" : true}}, { "metadata" : 1}).forEach(function(doc) {
    usedSize = usedSize + doc.metadata.size;
  });
  print("SET u.storage = coalesce(u.storage, 0) + " + usedSize + ";");
});
print("commit");

