(function(){
	"use strict";

	var widget = model.widgets.findWidget('notes');

	http().get('/userbook/preference/notes').done(function(notes){
		widget.notes = JSON.parse(notes.preference) || '';
		model.widgets.apply();
	});

	widget.save = function(){
		http().putJson('/userbook/preference/notes', widget.notes);
		notify.info('notify.object.saved');
	};
}());