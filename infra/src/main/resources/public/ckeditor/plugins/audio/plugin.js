CKEDITOR.plugins.add('audio', {
	icons: 'audio',
	init: function( editor ) {
		editor.addCommand('audio', new CKEDITOR.command( editor, {
			exec: function( editor ) {
				var fileSelector = $('<input />', {
					type: 'file'
				})
					.hide()
					.appendTo('body');


				fileSelector.on('change', function(){
					var uploadPath = CKEDITOR.fileUploadPath;
					var formData = new FormData();
					formData.append('file', fileSelector[0].files[0]);

					http().postFile(uploadPath + '&name=' + fileSelector[0].files[0].name,  formData, {
							requestName: 'ckeditor-image'
						})
						.done(function(e){
							var sound = editor.document.createElement('audio');

							sound.setAttribute('src', '/workspace/document/' + e._id);
							sound.setAttribute('controls', 'controls');

							fileSelector.remove();
							editor.insertElement(sound);
					});
				});
				fileSelector.click();
			}
		} ));
		editor.ui.addButton( 'Audio', {
			label: 'Ajouter un fichier audio',
			command: 'audio',
			toolbar: 'insert'
		});
	}
});