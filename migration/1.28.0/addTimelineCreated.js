db.timeline.find({ "created" : { $exists: false }}, { "_id" : 1, "date" : 1}).forEach(function(item){
  db.timeline.update({ "_id" : item._id }, { $set : { "created" :  item.date }});
});
db.timeline.createIndex( { created : -1 }, { background: true } );
db.timeline.dropIndex("date_1");
db.timeline.dropIndex("event-type_1");
