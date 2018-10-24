db.pages.find({"shared.fr-wseduc-pages-controllers-PagesController|shareSubmit" : true}, {"shared":1}).forEach(function(doc) {
	var modified = false;
	for (var i = 0; i < doc.shared.length; i++) {
		if ("fr-wseduc-pages-controllers-PagesController|shareSubmit" in doc.shared[i] && !("fr-wseduc-pages-controllers-PagesController|shareResource" in doc.shared[i])) {
			doc.shared[i]["fr-wseduc-pages-controllers-PagesController|shareResource"] = true;
			modified = true;
		}
	}
	if (modified) {
		db.pages.update({"_id" : doc._id}, { $set : { "shared" : doc.shared}});
	}
});

