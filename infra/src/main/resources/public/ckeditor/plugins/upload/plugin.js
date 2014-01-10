CKEDITOR.plugins.add( 'upload', {
	icons: 'upload',
	init: function( editor ) {
		editor.addCommand( 'uploadFile', new CKEDITOR.command( editor, {
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

					http().postFile(uploadPath + '&thumbnail=600x0&name=' + fileSelector[0].files[0].name,  formData, {
							requestName: 'ckeditor-image'
						})
						.done(function(e){
						var image = editor.document.createElement('img');

						image.setAttribute('src', '/workspace/document/' + e._id + '?thumbnail=600x0');
						fileSelector.remove();
						editor.insertElement(image);
					});
				});
				fileSelector.click();
			}
		} ));
		editor.ui.addButton( 'Upload', {
			label: 'Ajouter une image',
			command: 'uploadFile',
			toolbar: 'insert'
		});
	}
});