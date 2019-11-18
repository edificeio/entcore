db.documents.createIndex({ "shared.groupId": 1 }, { name: "idx_shared_groupid" })
db.documents.createIndex({ "shared.userId": 1 }, { name: "idx_shared_userid" })