var messenger = (function(){
	"use strict";

	var parentUrl;

	var send = function(message){
		if(parentUrl !== undefined){
			parent.postMessage(JSON.stringify(message), parentUrl);
		}
	};

	var messagesHandlers = {
		'set-style': function(message){
			if($('link[href="' + message.data + '"]').length > 0){
				return;
			}

			$('<link />', {
					rel: 'stylesheet',
					href: message.data,
					type: 'text/css'
				})
				.appendTo('head')
				.on('load', function(){
					var appSizeMessage = {
						name: 'resize',
						data: {
							height: $('body').height() + 1
						}
					};

					send(appSizeMessage);
				});


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
		requireResize: function(){
			var appSizeMessage = {
				name: 'resize',
				data: {
					height: $('html').outerHeight(true) + 1
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
		closeLightbox: function(){
			var appSizeMessage = {
				name: 'closeLightbox',
				data: {
				}
			};

			send(appSizeMessage);
		}
	};
}());

var navigationController = (function(){
	"use strict";

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
			}
		}
	});

	return app;
}());

$(document).ready(function(){
	"use strict";

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
		$('.display-buttons i').on('click', function(){
			$(this).parent().find('i').removeClass('selected');
			$(this).addClass('selected');
		});

		$('.close-lightbox i').on('click', function(){
			ui.hideLightbox();
		});

		$('body').on('click', '.file-button', function(e){
			var linkedId = $(this).data('linked');
			$('#' + linkedId).click();
			$('#' + linkedId).on('change', function(e){
				var fileUrl = $(this).val();
				if(fileUrl.indexOf('fakepath') !== -1){
					fileUrl = fileUrl.split('fakepath')[1];
				}
				$('#' + linkedId + '-content').text(fileUrl);
			})

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
