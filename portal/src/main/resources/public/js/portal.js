var navigation = (function(){
	"use strict";

	var currentUrl = '/workspace';
	return {
		redirect: function(url){
			$('#applications').attr('src', url);
			$('#applications').on('load', function(e){
				console.log(e);
				var styleUrl = $('#theme').attr('href');
				var message = {
					name: 'set-style',
					data: styleUrl
				};

				setTimeout(function(){
					messenger.sendMessage('#applications', message);
				}, 100);
			});
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
		closeLightbox: function(message){
			$('header').removeClass('lightbox-header');
			$('body').removeClass('lightbox-body');
			$('section.main').removeClass('lightbox-main');
			$('body').unbind('click');
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
		navigation.redirect('/apps');
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
	})

	navigationController.start();
});