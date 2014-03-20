CKEDITOR.plugins.add('video', {
		icons: 'video',
		init: function(editor){
			editor.addCommand('Video',new CKEDITOR.dialogCommand('Video'));

			editor.ui.addButton( 'Video', {
				label: 'Ajouter une image',
				command: 'Video',
				toolbar: 'insert'
			});

			CKEDITOR.dialog.add('Video', function(api){
				// CKEDITOR.dialog.definition
				var dialogDefinition =
				{
					title : 'Vidéo',
					minWidth : 390,
					minHeight : 130,
					contents : [
						{
							id : 'tab1',
							label : 'Label',
							title : 'Vidéo',
							expand : true,
							padding : 0,
							elements : [{
									type : 'html',
									html : '<p>Au préalable, votre vidéo doit être enregistrée sur une plateforme de partage ' +
										'(youtube, vimeo, ScolaWebTV, etc.). <br />Pour l\'insérer ici, copiez le le lien de partage "embed" et collez-le ci-dessous.</p>'
								},{
									type : 'textarea',
									id : 'video-link'
								}
							]
						}
					],
					buttons: [CKEDITOR.dialog.okButton, CKEDITOR.dialog.cancelButton],
					onOk: function() {
						var videoLink = this.getContentElement('tab1', 'video-link');
						var link = videoLink.getValue();
						var element;

						if(link.indexOf('http') === -1 && link.indexOf('https') === -1){
							link.replace('//', 'https://');
						}
						else if(link.indexOf('https') === -1){
							link.replace('http://', 'https://');
						}

						element = editor.document.createElement('div');
						element.appendHtml(link);

						editor.insertElement(element);
					}
				};

				return dialogDefinition;
			});
		}
	});
