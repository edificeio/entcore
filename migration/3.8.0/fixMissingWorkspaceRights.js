var bulk = []
var PREFIX = "org-entcore-workspace-controllers-WorkspaceController";
db.documents.find({}).forEach(function(document){
    var newRight = PREFIX + "|shareResource";
    var managers = [PREFIX + "|deleteDocument", PREFIX + "|shareJsonSubmit"];
    var sharedChanged = false;
    var inheritChanged = false;
    if(document.shared){
        document.shared.forEach(function(share){
            managers.forEach(function(right){
                if(share[right] && !share[newRight]){
                    share[newRight] = true;
                    sharedChanged = true;
                }
            })
        })
    }
    if(document.inheritedShares){
        document.inheritedShares.forEach(function(share){
            managers.forEach(function(right){
                if(share[right] && !share[newRight]){
                    share[newRight] = true;
                    inheritChanged = true;
                }
            })
        })
    }
    if(sharedChanged || inheritChanged){
        var set = { 'fixShareResource': true };
        if(sharedChanged){
            set["shared"] = document.shared;
        }
        if(inheritChanged){
            set["inheritedShares"] = document.inheritedShares;
        }
        bulk.push({
            "updateOne": {
                "filter": { "_id": document._id },
                "update": {
                    "$set": set
                }
            }
        })
    }
});
print("bulk write : "+bulk.length);
db.documents.bulkWrite(bulk)