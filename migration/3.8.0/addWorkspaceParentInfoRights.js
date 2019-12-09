var bulk = []
var newRight = "org-entcore-workspace-controllers-WorkspaceController|getParentInfos";
var managers = ["org-entcore-workspace-controllers-WorkspaceController|renameDocument","org-entcore-workspace-controllers-WorkspaceController|renameFolder"]
db.documents.find({}).forEach(function(document){
    var changedShared = false;
    if(document.shared){
        document.shared.forEach(function(share){
            managers.forEach(function(right){
                if(share[right] && !share[newRight]){
                    share[newRight] = true;
                    changedShared = true;
                }
            })
        })
    }
    var changedInheritedShared = false;
    if(document.inheritedShares){
        document.inheritedShares.forEach(function(share){
            managers.forEach(function(right){
                if(share[right] && !share[newRight]){
                    share[newRight] = true;
                    changedInheritedShared = true;
                }
            })
        })
    }
    //build bulk
    if(changedShared && changedInheritedShared){
        bulk.push({
            "updateOne": {
                "filter": { "_id": document._id },
                "update": {
                    "$set": { 'shared': document.shared, 'inheritedShares': document.inheritedShares }
                }
            }
        })
    } else if(changedShared){
        bulk.push({
            "updateOne": {
                "filter": { "_id": document._id },
                "update": {
                    "$set": { 'shared': document.shared }
                }
            }
        })
    } else if(changedInheritedShared){
        bulk.push({
            "updateOne": {
                "filter": { "_id": document._id },
                "update": {
                    "$set": { 'inheritedShares': document.inheritedShares }
                }
            }
        })
    }
});
print("bulk write : "+bulk.length);
db.documents.bulkWrite(bulk)
