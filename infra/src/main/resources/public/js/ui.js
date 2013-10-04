var ui = (function(){

	var ui = {
		showLightbox: function(){
			$('.lightbox-backdrop').fadeIn('normal', function(){
				messenger.requireResize();
			});
			$('.lightbox-window').fadeIn()
			$('.lightbox-window').css({ 'margin-left': '-' + ($('.lightbox-window').width() / 2) + 'px'});

			messenger.requireLightbox();
			//For now, we ignore parent size and base ourselves on iframe size only.
			messenger.sendMessage({
				name: 'where-lightbox',
				data: {}
			});
		},
		hideLightbox: function(){
			$('.lightbox-backdrop').fadeOut();
			$('.lightbox-window').fadeOut();
			messenger.closeLightbox();
		}
	};

	$(document).ready(function(){
		$('.display-buttons i').on('click', function(){
			$(this).parent().find('i').removeClass('selected');
			$(this).addClass('selected');
		});

		$('.lightbox-window').on('click', '.close-lightbox i, .lightbox-buttons .cancel', function(){
			ui.hideLightbox();
		});

		$('body').on('click', '.select-file input[type!="file"], button', function(e){
			var inputFile = $(this).parent().find('input[type=file]');
			if($(this).attr('type') === 'text'){
				if(!$(this).data('changed')){
					inputFile.click();
				}
			}
			else{
				inputFile.click();
			}
			$('[data-display-file]').data('changed', true);

			inputFile.on('prettyinput.change', function(){
				var displayElement = inputFile.parent().parent().find('[data-display-file]');
				var fileUrl = $(this).val();
				if(fileUrl.indexOf('fakepath') !== -1){
					fileUrl = fileUrl.split('fakepath')[1];
					fileUrl = fileUrl.substr(1);
					fileUrl = fileUrl.split('.')[0];
				}
				if(displayElement.length > 0 && displayElement[0].tagName === 'INPUT'){
					displayElement.val(fileUrl);
				}
				else{
					displayElement.text(fileUrl);
				}
				$(this).unbind('prettyinput.change');
			});

			e.preventDefault();
		});

		$('.search input[type=text]').on('focus', function(){
			$(this).val(' ');
		})

		$('body').on('mousedown', '.enhanced-select .current', function(e){
			var select = $(this).parent();
			var optionsList = select.children('.options-list');

			if($(this).hasClass('editing')){
				$(this).removeClass('editing');
				optionsList.slideUp();
				e.preventDefault();
				return;
			}

			var that = this;
			$(that).addClass('editing');
			optionsList.slideDown();
			optionsList.children('.option').on('mousedown', function(){
				$(that).removeClass('editing');
				$(that).data('selected', $(this).data('value'));
				$(that).html($(this).html());
				optionsList.slideUp();
				select.change();
			});
		});
	});

	return ui;
}());