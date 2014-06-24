CKEDITOR.plugins.add('linker', {
	icons: 'linker',
	init: function(editor) {
		editor.addCommand('openLinker', new CKEDITOR.command(editor, {
			exec: function( editor ) {

			}
		}));
		editor.ui.addButton( 'Linker', {
			label: 'Créer un lien',
			command: 'openLinker',
			toolbar: 'insert'
		});
	}
});