var queries = {};
db.documents.distinct("owner").forEach(function(owner) {
  db.documents.distinct("folder", {"owner" : owner}).forEach(function(folder) {
    var d = db.documents.findOne({"owner":owner, "folder":folder});
    var f = folder.split('_');
    if (f.length === 1) {
      var id = ObjectId();
      queries[(owner+folder)] = {"_id":id.valueOf(),"owner":d.owner,"ownerName":d.ownerName,"created":d.created,"modified":d.modified,"folder":folder,"application":d.application,"name":folder}
    } else {
      var path = '';
      f.forEach(function(subfolder) {
        var id = ObjectId();
        if (path === '') {
          path = subfolder;
        } else {
          path = path + '_' + subfolder;
        }
        queries[(owner+path)] = {"_id":id.valueOf(),"owner":d.owner,"ownerName":d.ownerName,"created":d.created,"modified":d.modified,"folder":path,"application":d.application,"name":subfolder}
      });
    }
  });
});
var q = [];
for (var key in queries) {
  q.push(queries[key]);
}
db.documents.insert(q);

db.documents.ensureIndex({ "owner" : 1 });
db.documents.ensureIndex({ "folder" : 1 });
db.documents.ensureIndex({ "old-folder" : 1 });

