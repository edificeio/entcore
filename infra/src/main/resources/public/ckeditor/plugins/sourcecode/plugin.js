CKEDITOR.plugins.add( 'sourcecode', {
	icons: 'upload',
	init: function( editor ) {
		editor.addCommand( 'sourcecode', new CKEDITOR.command( editor, {
			exec: function( editor ) {
				console.log(editor);
				$(editor.element).on('DOMSubtreeModified', function(e){
					console.log('change');
				})
			}
		} ));
		editor.ui.addButton( 'SourceCode', {
			label: 'Afficher la source',
			command: 'sourcecode',
			toolbar: 'document'
		});
	}
});
