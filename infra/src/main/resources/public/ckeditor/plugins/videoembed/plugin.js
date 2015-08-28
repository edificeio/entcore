CKEDITOR.plugins.add('videoembed', {
	icons: 'video',
	init: function( editor ) {
		editor.addCommand('videoembed', new CKEDITOR.command( editor, {
			exec: function( editor ) {
			}
		} ));
		editor.ui.addButton( 'Video', {
			label: 'Ajouter un code embed',
			command: 'Video',
			toolbar: 'insert'
		});
	}
});
