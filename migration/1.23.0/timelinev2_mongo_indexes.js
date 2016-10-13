db.timeline.createIndex( { "reporters.userId": 1 } )
db.timeline.createIndex( { "reporters.date": -1 } )
db.timeline.createIndex( { "reportAction": 1 } )
db.timeline.createIndex( { "reportAction.date": -1 } )
