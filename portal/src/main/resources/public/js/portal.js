var navigation = (function(){
	"use strict";

	var portalHistory = [];
	var currentUrl = '/workspace';

	var setStyle = function(){
		var styleUrl = $('#theme').attr('href');
		var message = {
			name: 'set-style',
			data: styleUrl
		};

		return message;
	};

	var setHistory = function(){
		var message = {
			name: 'set-history',
			data: history
		};

		return message;
	};

	return {
		redirect: function(data, invisible){
			if(!invisible){
				portalHistory.push(data);
			}

			history.pushState({}, null, '/?app=' + data);

			$('#applications').attr('src', data);

			$('#applications').on('load', function(e){
				setTimeout(function(){
					messenger.sendMessage('#applications', setStyle());
					messenger.sendMessage('#applications', setHistory());
				}, 100);
			});
		},
		moveHistory: function(data){
			if(data.action === 'pop'){
				if(portalHistory.length){
					portalHistory.length = portalHistory.length - 1;
				}

				navigation.redirect(portalHistory[portalHistory.length - 1], true);
			}
		}
	};
}());

var messenger = (function(){
	"use strict";

	var messagesHandlers = {
		redirect: function(message){
			navigation.redirect(message.data);
		},
		resize: function(message){
			$('#applications').height(message.data.height);
		},
		lightbox: function(message){
			//We can't put a black background on the whole view (iframe + container) without killing the positioning,
			//therefore we're dropping the opacity of the elements and darkening the background to
			//get the same result
			$('header').addClass('lightbox-header');
			$('body').addClass('lightbox-body');
			$('section.main').addClass('lightbox-main');
			var close = this.closeLightbox;
			$('body').one('click', function(){
				close();
				$('iframe').attr('src', $('iframe').attr('src'));
			});
		},
		'close-lightbox': function(message){
			$('header').removeClass('lightbox-header');
			$('body').removeClass('lightbox-body');
			$('section.main').removeClass('lightbox-main');
			$('body').unbind('click');
		},
		'move-history': function(message){
			navigation.moveHistory(message.data);
		}
	};

	if(window.addEventListener){
		window.addEventListener('message', function(messageData){
			var message = JSON.parse(messageData.data);
			messagesHandlers[message.name](message);
		});
	}

	return {
		sendMessage: function(frame, message){
			var origin = window.location.href;
			if($(frame).attr('src').indexOf('http') !== -1){
				origin = $(frame).attr('src');
			}
			$(frame)[0].contentWindow.postMessage(JSON.stringify(message), origin);
		}
	};
}());

var navigationController = (function(){
	"use strict";

	var app = Object.create(oneApp);
	app.scope = 'nav[role=apps-navigation]';
	app.start = function(){
		this.init();
		var redirect = window.location.href.split('?app=');
		var appUrl = '/apps';

		if(redirect.length > 1){
			appUrl = redirect[1].split('&')[0];
		}

		navigation.redirect(appUrl);
	};

	app.define({
		action: {
			redirect: function(data){
				navigation.redirect(data.url);
			}
		}
	});

	return app;
}());


$(document).ready(function(){
	"use strict";

	$('.search input[type=text]').on('focus', function(){
		$(this).val(' ');
	});

	navigationController.start();
});