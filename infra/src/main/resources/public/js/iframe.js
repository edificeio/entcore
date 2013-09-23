var messenger = (function(){
	"use strict";

	var parentUrl;

	var send = function(message){
		if(parentUrl !== undefined){
			parent.postMessage(JSON.stringify(message), parentUrl);
		}
	};

	var messagesHandlers = {
		'set-history': function(message){
			var history = message.data;
			for(var i = 0; i < message.data.length; i++){
				window.history.pushState({ link: message.data[i] }, null, '/?app=' + message.data[i]);
			}
		},
		'set-style': function(message){
			if($('link[href="' + message.data + '"]').length > 0){
				return;
			}

			var updateView = function(){
				$('body').show();

				var appSizeMessage = {
					name: 'resize',
					data: {
						height: $('body').height() + 1
					}
				};

				send(appSizeMessage);
			};

			var nbStylesheets = document.styleSheets.length;
			$('<link />', {
					rel: 'stylesheet',
					href: message.data,
					type: 'text/css'
				})
				.appendTo('head')
				.attr('data-portal-style', message.data)
				.on('load', function(){
					updateView();
				});

			//we need to give back the main thread to the browser, so it can add the stylesheet to the document
			setTimeout(function(){
				if(document.styleSheets.length > nbStylesheets){
					//loading is done from cache, which means "load" event won't be called at all
					updateView();
				}
			}, 50);
		}
	};

	if(window.addEventListener){
		window.addEventListener('message', function(messageData){
			parentUrl = messageData.origin;
			var message = JSON.parse(messageData.data);
			messagesHandlers[message.name](message);
		});
	}

	return {
		sendMessage: function(message){
			send(message);
		},
		redirectParent: function(location){
			send({
				name: 'redirect-parent',
				data: location
			});
		},
		requireResize: function(){
			var bodySize = $('body').outerHeight(true);
			var windowSize = $('.lightbox-window').outerHeight(true);

			var newSize = bodySize;
			if(windowSize > bodySize){
				newSize = windowSize;
			}

			var appSizeMessage = {
				name: 'resize',
				data: {
					height: newSize + 1
				}
			};

			send(appSizeMessage);
		},
		requireLightbox: function(){
			var appSizeMessage = {
				name: 'lightbox',
				data: {
				}
			};

			send(appSizeMessage);
		},
		notify: function(type, message){
			send({
				name: 'notify',
				data: {
					type: type,
					message: message
				}
			});
		},
		closeLightbox: function(){
			var appSizeMessage = {
				name: 'close-lightbox',
				data: {
				}
			};

			send(appSizeMessage);
		},
		updateAvatar: function(){
			messenger.sendMessage({
				name: 'update-avatar',
				data: {}
			})
		}
	};
}());

var navigationController = (function(){
	"use strict";

	One.filter('disconnected', function(event){
		messenger.redirectParent('/');
	})

	var app = Object.create(oneApp);
	app.scope = 'nav[role=apps-navigation]';
	app.start = function(){
		this.init();
	};

	app.define({
		action: {
			redirect: function(data){
				messenger.sendMessage({
					name: 'redirect',
					data: data.url
				});
			},
			moveHistory: function(data){
				messenger.sendMessage({
					name: 'move-history',
					data: data
				})
			}
		}
	});

	return app;
}());

$(document).ready(function(){
	"use strict";

	if(parent !== window && $('link[data-portal-style]').length === 0){
		$('body').hide();
	}

	navigationController.start();
});

var ui = (function(){

	var ui = {
		showLightbox: function(){
			$('.lightbox-backdrop').fadeIn();
			$('.lightbox-window').fadeIn();
			$('.lightbox-window').css({ 'margin-left': '-' + ($('.lightbox-window').width() / 2) + 'px'});
			messenger.requireLightbox();
		},
		hideLightbox: function(){
			$('.lightbox-backdrop').fadeOut();
			$('.lightbox-window').fadeOut();
			messenger.closeLightbox();
		}
	};

	$(document).ready(function(){
		$('body').on('focus', '[contenteditable]', function(){
			$(this).attr('data-origin', this.innerHTML);
			$(this).on('blur', function(){
				if($(this).attr('data-origin') !== $(this).html()){
					$(this).trigger('change');
					$(this).attr('data-origin', this.innerHTML);
				}
			})
		})

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

			inputFile.on('change', function(){
				var displayElement = inputFile.parent().parent().find('[data-display-file]');
				var fileUrl = $(this).val();
				if(fileUrl.indexOf('fakepath') !== -1){
					fileUrl = fileUrl.split('fakepath')[1];
					fileUrl = fileUrl.substr(1);
					fileUrl = fileUrl.split('.')[0];
				}
				if(displayElement[0].tagName === 'INPUT'){
					displayElement.val(fileUrl);
				}
				else{
					displayElement.text(fileUrl);
				}
				$(this).unbind('change');
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
				select.data('selected', $(this).data('value'));
				$(that).html($(this).html());
				optionsList.slideUp();
				select.change();
			});
		});
	});

	return ui;
}());
