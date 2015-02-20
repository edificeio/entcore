db.documents.find().forEach(function(item){
    if(item.file){
        var exists = db.documentsRevisions.count({file: item.file})
        if(!exists){
            db.documentsRevisions.insert({
                _id: ObjectId().str,
                documentId: item._id,
                file: item.file,
                name: item.name,
                owner: item.owner,
                userId: item.owner,
                userName: item.ownerName,
                date: new Date(),
                metadata : item.metadata
            })
        }
    }
})
