db.timeline.update({ "type" : "WORKSPACE"}, {"$set" : {"event-type": "WORKSPACE_SHARE"}}, false, true);
db.timeline.update({ "type" : "USERBOOK", "message" : {"$regex" : /^.*jour sa devise.*$/}}, {"$set" : {"event-type": "USERBOOK_MOTTO"}}, false, true);
db.timeline.update({ "type" : "USERBOOK", "message" : {"$regex" : /^.*jour son humeur$/}}, {"$set" : {"event-type": "USERBOOK_MOOD"}}, false, true);
db.timeline.update({ "type" : "BLOG", "message" : {"$regex" : /^.*vous a donné accès au blog.*$/i}}, {"$set" : {"event-type": "BLOG_SHARE"}}, false, true);
db.timeline.update({ "type" : "BLOG", "message" : {"$regex" : /^.*a modifié le blog.*$/i}}, {"$set" : {"event-type": "BLOG_UPDATE"}}, false, true);
db.timeline.update({ "type" : "BLOG", "sub-resource" : { "$exists" : true }}, {"$set" : {"event-type": "BLOG_POST_PUBLISH"}}, false, true);
