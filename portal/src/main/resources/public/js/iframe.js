var messenger = (function(){
	"use strict";

	var send = function(message){
		parent.window.postMessage(JSON.stringify(message), window.location.href);
	};

	var messagesHandlers = {
		'set-style': function(message){
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
							height: $(document).outerHeight(true) + 1
						}
					};

					send(appSizeMessage);
				});


		}
	};

	if(window.addEventListener){
		window.addEventListener('message', function(messageData){
			var message = JSON.parse(messageData.data);
			messagesHandlers[message.name](message);
		});
	}

	return {
		sendMessage: function(message){
			send(message);
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
