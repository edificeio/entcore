function Note(){

}


(function(){
	"use strict";

	model.makeModel(Note);
	model.makePermanent(Note);

	var Notes = model.widgets.findWidget('notes');
	model.notes.mine.on('sync', function(){
		Notes.note = model.notes.mine.first() || new Note();
		if(Notes.note._id){
			Notes.note.open();
			Notes.note.on('sync', function(){
				model.widgets.apply();
			});
		}
	});
}());