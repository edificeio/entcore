CKEDITOR.plugins.add('html', {
	icons: 'html',
	init: function( editor ) {
		editor.addCommand('html', new CKEDITOR.command( editor, {
			exec: function( editor ) {
			}
		} ));
		editor.ui.addButton( 'HTML', {
			label: 'Modifier le code HTML',
			command: 'html',
			toolbar: 'insert'
		});
	}
});