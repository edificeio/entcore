var bulk = []
db.documents.find({}).forEach(function(document){
    if(document.nameSearch){
        bulk.push({
            "updateOne": {
                "filter": { "_id": document._id },
                "update": {
                    "$set": { 'nameSearch': document.nameSearch.replace(/_/g, ' ') }
                }
            }
        })
    }
});
print("bulk write : "+bulk.length);
db.documents.bulkWrite(bulk)