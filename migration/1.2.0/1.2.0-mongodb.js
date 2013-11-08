db.timeline.update({ "message" : {"$regex" : /^.*un document.{5}$/}}, {"$set" : {"type": "WORKSPACE"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*jour sa devise.*$/}}, {"$set" : {"type": "USERBOOK"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*jour son humeur$/}}, {"$set" : {"type": "USERBOOK"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*blog.*$/i}}, {"$set" : {"type": "BLOG"}}, false, true);

db.timeline.find().forEach(function(notification) {
  var id = notification._id;
  var recipients = notification.recipients;
  var newRecipients = [];

  for (var i = 0; i < recipients.length; i++) {
    newRecipients.push({ "userId" : recipients[i], "unread" : NumberInt(0) });
  }

  db.timeline.update({ "_id" : id }, { $set : { "recipients" : newRecipients }});
});

db.timeline.ensureIndex({ "recipients.userId" : 1 })

