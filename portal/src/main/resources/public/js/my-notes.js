(function(){
	var Widget = model.widgets.findWidget('my-notes');

	function Note(){

	}
	model.makeModel(Note);
	model.makePermanent(Note);
	model.notes.mine.on('sync', function(){
		Widget.notes = model.notes.mine;
		model.widgets.apply();
	})
}());

