var ui = (function(){
	var mainLightbox = {
		show: function(){
			$('.lightbox-backdrop').fadeIn();
			$('.lightbox-window').fadeIn();
			$('.lightbox-window').css({
				'margin-top': 0,
				'position': 'fixed',
				top: 0
			})
			var offset = $('.lightbox-window').offset();
			$('.lightbox-window').css({
				'position': 'absolute',
				'top': offset.top,
				'margin-top': '100px'
			});

			var that = this;
			$('body').on('click', '.lightbox-backdrop', function(){
				that.hide();
			});
		},
		hide: function(){
			$('.lightbox-backdrop').fadeOut();
			$('.lightbox-window').fadeOut();
		}
	};

	var iframeLightbox = {
		show: function(){
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
		hide: function(){
			$('.lightbox-backdrop').fadeOut();
			$('.lightbox-window').fadeOut();
			messenger.closeLightbox();
		}
	}

	var uiInterface = {
		scrollToTop: function(){
			var scrollUp = function(){
				var scrollTop = window.scrollY || document.getElementsByTagName('html')[0].scrollTop;
				if(scrollTop <= $('body').offset().top){
					return;
				}
				window.scrollTo(0, scrollTop - parseInt(scrollTop / 10) - 1);
				setTimeout(scrollUp, 10);
			};
			scrollUp();
		},
		showLightbox: function(){
			if(parent !== window){
				iframeLightbox.show();
			}
			else{
				mainLightbox.show();
			}
		},
		hideLightbox: function(){
			if(parent !== window){
				iframeLightbox.hide();
			}
			else{
				mainLightbox.hide();
			}
		},
		updateAvatar: function(){
			var scope = angular.element(document.getElementById('my-photo')).scope();
			scope.refreshAvatar();
		},
		setStyle: function(stylePath){
			if($('#theme').length === 0){
				var style = $('<link>', {
					rel: 'stylesheet',
					type: 'text/css',
					href: stylePath + 'theme.css',
					id: 'theme'
				});
				style.on('load', function(){
					$('body').show();
				})
				$('head').append(style);
			}
			else{
				$('#theme').attr('href', stylePath + 'theme.css');
			}
		}
	};

	$(document).ready(function(){
		$('.display-buttons i').on('click', function(){
			$(this).parent().find('i').removeClass('selected');
			$(this).addClass('selected');
		});


		var resizeTextarea = function(){
			$(this).height(1);
			$(this).height(this.scrollHeight - 1);
		}

		$('body').on('keydown', 'textarea.inline-editing', resizeTextarea);
		$('body').on('keyup', 'textarea.inline-editing', resizeTextarea);
		$('body').on('focus', 'textarea.inline-editing', resizeTextarea);

		$('body').on('click', '[data-reload]', function(){
			window.location.href = window.location.href;
		});

		$('body').on('click', '.lightbox-window .close-lightbox i, .lightbox-window .lightbox-buttons .cancel, .lightbox-window .cancel', function(){
			ui.hideLightbox();
		});

		$('.remove-fout').removeClass('remove-fout');

		$('body').on('click', '.select-file input[type!="file"], .select-file button, .file-selector', function(e){
			var inputFile = $(this).parent().find('input[type=file]');
			if($(this).attr('for')){
				inputFile = $('#' + $(this).attr('for'));
			}

			if(inputFile.length === 0){
				inputFile = $('input[type=file]');
			}
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

		$('body').on('mousedown', '.icons-select .current', function(e){
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

	return uiInterface;
}());



// Remove event in JQuery
(function($){
	$.event.special.removed = {
		remove: function(o) {
			if (o.handler) {
				o.handler()
			}
		}
	}
})(jQuery)
