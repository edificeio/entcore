CKEDITOR.plugins.add('audio', {
	icons: 'audio',
	init: function( editor ) {
		editor.addCommand('audio', new CKEDITOR.command( editor, {
			exec: function( editor ) {
			}
		} ));
		editor.ui.addButton( 'Audio', {
			label: 'Ajouter un fichier audio',
			command: 'audio',
			toolbar: 'insert'
		});
	}
});