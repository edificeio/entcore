db.timeline.update({ "message" : {"$regex" : /^.*un document.{5}$/}}, {"$set" : {"type": "WORKSPACE"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*jour sa devise.*$/}}, {"$set" : {"type": "USERBOOK"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*jour son humeur$/}}, {"$set" : {"type": "USERBOOK"}}, false, true);
db.timeline.update({ "message" : {"$regex" : /^.*blog.*$/i}}, {"$set" : {"type": "BLOG"}}, false, true);

