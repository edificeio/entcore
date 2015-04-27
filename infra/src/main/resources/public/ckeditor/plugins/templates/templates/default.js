/*
 Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 For licensing, see LICENSE.md or http://ckeditor.com/license
*/
var imagesPath = CKEDITOR.getUrl(CKEDITOR.plugins.getPath("templates")+"templates/images/");
CKEDITOR.addTemplates(
	"default",{
		imagesPath: imagesPath,
		templates:[{
				title:"Page blanche",
				image:"simple-text.gif",
				description:"Simple page blanche sans mise en forme.",
				html:'<div class="row">' +
					'<div class="twelve cell" style="text-align: justify">' +
					'<p>Contenu de l\'article</p></div>' +
					'</div>'
			}, {
			title:"Deux colonnes",
			image:"two-cols.gif",
			description:"Deux colonnes et un titre.",
			html:'<h2>Titre</h2>' +
				'<div class="row">' +
				'<div class="six cell reduce-block-eight" style="text-align: justify">' +
				'<h3>Titre</h3>' +
				'<p>Colonne de gauche</p></div>' +
				'<div class="six cell reduce-block-eight" style="text-align: justify">' +
				'<h3>Titre</h3>' +
				'<p>Colonne de droite</p>' +
				'</div>' +
				'</div>'
		},{
			title:"Illustration et texte",
			image:"image-and-text.gif",
			description:"Une illustration enveloppée dans du texte.",
			html:'<h2>Titre</h2>' +
				'<div class="row">' +
				'<div class="three cell clip img-container avatar" style="margin: 10px; margin-left: 0">' +
				'<img src="' + imagesPath + 'filler-image.png" />' +
				'</div>' +
				'<p style="text-align: justify;margin:0">Contenu de l\'article</p></div>'
		},{
			title:"Vignettes",
			image:"images.gif",
			description:"Des vignettes contenant une image et sa description",
			html:'<div class="row">' +
					'<div class="reduce-block-eight cell six">' +
						'<div class="fixed-block height-five">' +
							'<article class="twelve cell absolute clip">' +
								'<div class="absolute cell six avatar">' +
									'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
								'</div>' +
								'<div class="fluid cell six reduce-block-six right-magnet">' +
									'<h2>Description</h2>' +
									'<p>Description de l\'image</p>' +
								'</div>' +
							'</article>' +
						'</div>' +
					'</div>' +
			'<div class="reduce-block-eight cell six">' +
			'<div class="fixed-block height-five">' +
			'<article class="twelve cell absolute clip">' +
			'<div class="absolute cell six avatar">' +
			'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
			'</div>' +
			'<div class="fluid cell six reduce-block-six right-magnet">' +
			'<h2>Description</h2>' +
			'<p>Description de l\'image</p>' +
			'</div>' +
			'</article>' +
			'</div>' +
			'</div>' +
				'<div class="row">' +
			'<div class="reduce-block-eight cell six">' +
			'<div class="fixed-block height-five">' +
			'<article class="twelve cell absolute clip">' +
			'<div class="absolute cell six avatar">' +
			'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
			'</div>' +
			'<div class="fluid cell six reduce-block-six right-magnet">' +
			'<h2>Description</h2>' +
			'<p>Description de l\'image</p>' +
			'</div>' +
			'</article>' +
			'</div>' +
			'</div>' +
			'<div class="reduce-block-eight cell six">' +
			'<div class="fixed-block height-five">' +
			'<article class="twelve cell absolute clip">' +
			'<div class="absolute cell six avatar">' +
			'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
			'</div>' +
			'<div class="fluid cell six reduce-block-six right-magnet">' +
			'<h2>Description</h2>' +
			'<p>Description de l\'image</p>' +
			'</div>' +
			'</article>' +
			'</div>' +
			'</div>' +
					'<div class="row">' +
			'<div class="reduce-block-eight cell six">' +
			'<div class="fixed-block height-five">' +
			'<article class="twelve cell absolute clip">' +
			'<div class="absolute cell six avatar">' +
			'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
			'</div>' +
			'<div class="fluid cell six reduce-block-six right-magnet">' +
			'<h2>Description</h2>' +
			'<p>Description de l\'image</p>' +
			'</div>' +
			'</article>' +
			'</div>' +
			'</div>' +
			'<div class="reduce-block-eight cell six">' +
			'<div class="fixed-block height-five">' +
			'<article class="twelve cell absolute clip">' +
			'<div class="absolute cell six avatar">' +
			'<img src="' + imagesPath + 'filler-image.png" class="absolute" />' +
			'</div>' +
			'<div class="fluid cell six reduce-block-six right-magnet">' +
			'<h2>Description</h2>' +
			'<p>Description de l\'image</p>' +
			'</div>' +
			'</article>' +
			'</div>' +
			'</div>'
		}]
	});