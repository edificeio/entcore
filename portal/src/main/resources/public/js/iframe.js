var messenger = (function(){
	"use strict";

	var parentUrl = 'http://';

	var send = function(message){
		parent.postMessage(JSON.stringify(message), parentUrl);
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
					height: $(document).outerHeight(true) + 1
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
