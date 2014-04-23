CKEDITOR.plugins.add( 'upload', {
	icons: 'upload',
	init: function( editor ) {
		editor.addCommand( 'uploadFile', new CKEDITOR.command( editor, {
			exec: function( editor ) {

			}
		} ));
		editor.ui.addButton( 'Upload', {
			label: 'Ajouter une image',
			command: 'uploadFile',
			toolbar: 'insert'
		});
	}
});