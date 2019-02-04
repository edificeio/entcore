var COLLECTION_NAME = "documents";
var INDEX_NAME = "idx_documents_migration";


var startTime, endTime;
function startMeasure(name) {
    print("event;start measuring;" + name);
    startTime = new Date();
}
function endMeasure(name) {
    endTime = new Date();
    var timeDiffMs = endTime - startTime; //in ms
    var timeDiffSeconds = (timeDiffMs / 1000);
    var timeDiffMinutes = timeDiffSeconds / 60;
    var seconds = Math.round(timeDiffSeconds);
    var minutes = Math.round(timeDiffMinutes);
    print("event;end measuring:" + minutes + " minutes (" + seconds + "seconds);" + name);
}
//
var count = 0;
var countTotal = 0;
var countParent = 0;
var countOwner = 0;
var countNotDeleted = 0;
var countWithParentOld = 0;
var countTotalLoop = 0;
var shareById = {};
var owners = {}
var bulk = []
//
startMeasure("loop")
db[COLLECTION_NAME].find({}).forEach(function (doc) {
    shareById[doc._id] = doc;
    countTotal++;
})
endMeasure("loop")
startMeasure("check")
for (var id in shareById) {
    var doc = shareById[id];
    countTotalLoop++;
    if (doc.eParent) {
        countParent++;
        var parent = shareById[doc.eParent];
        if (parent.isShared != doc.isShared && !parent.isShared) {
            count++;
            owners[doc.owner] = doc.ownerName;
            if (!doc.deleted) {
                countNotDeleted++;
            }
            if (doc.eParentOld) {
                countWithParentOld++;
            }
            bulk.push({
                "updateOne": {
                    "filter": { "_id": doc._id },
                    "update": {
                        "$rename": { 'eParent': 'eParentOld' },
                        "$set": { 'fixChildren': true }
                    }
                }
            })
            //print("founded:", parent._id, ";", doc._id, ";", parent.name, ";", doc.name, ";", parent.isShared, ";", doc.isShared, ";", doc.deleted, ";", doc.eParentOld, ";", doc.owner, ";", doc.ownerName)
        }
    }
}
endMeasure("check")
//BULK
//startMeasure("bulk")
//print("dump----------------------------------------------")
//print(bulk)
//print("enddump----------------------------------------------")
db[COLLECTION_NAME].bulkWrite(bulk)
//endMeasure("bulk")
//
//print("owners------------------------------------------------------")
for (var ownerId in owners) {
//    print(ownerId, ";", owners[ownerId]);
    countOwner++;
}
//print("end owners------------------------------------------------------")
//
print("total;" + countTotal, ";", countTotalLoop, ";", countTotalLoop == countTotal)
print("number of files;" + count)
print("number of owners;" + countOwner)
print("number of files not deleted;" + countNotDeleted)
print("number of files with parent old;" + countWithParentOld)