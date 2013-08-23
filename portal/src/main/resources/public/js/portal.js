var navigation = (function(){
	"use strict";

	var currentUrl = '/workspace';
	return {
		redirect: function(url){
			$('#applications').attr('src', url);
			$('#applications').on('load', function(){
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

	navigationController.start();
});