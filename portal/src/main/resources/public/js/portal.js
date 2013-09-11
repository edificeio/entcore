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



			$('#applications').attr('src', data);

			$('#applications').unbind('load');
			$('#applications').on('load', function(e){
				setTimeout(function(){
					messenger.sendMessage('#applications', setStyle());
					history.pushState({}, null, '#app=' + data);
				}, 100);
			});
		},
		applyHash: function(){
			var redirect = window.location.href.split('#app=');
			var appUrl = $('.horizontal[role=apps-navigation] a')
				.first()
				.attr('href');

			if(redirect.length > 1){
				appUrl = redirect[1].split('&')[0];
			}

			navigation.redirect(appUrl);
		},
		back: function(){
			portalHistory.length = portalHistory.length - 1;
			navigation.redirect(portalHistory.length - 1);
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
		notify: function(message){
			oneApp.notify[message.data.type](oneApp.i18n.i18n()(message.data.message));
		},
		'redirect-parent': function(message){
			window.location.href = message.data;
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
		navigation.applyHash();
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

$(window).on('hashchange', function() {
	"use strict";

	navigation.applyHash();
});