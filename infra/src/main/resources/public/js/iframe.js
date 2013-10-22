var messenger = (function(){
	"use strict";

	var parentUrl;

	var send = function(message){
		if(parentUrl !== undefined){
			parent.postMessage(JSON.stringify(message), parentUrl);
		}
	};

	var requireResize = function(){
		var bodySize = $('body').outerHeight(true);

		var windowSize = 0;
		if($('.lightbox-window').length > 0){
			windowSize = $('.lightbox-window').outerHeight(true) + $('.lightbox-window').offset().top;
		}

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
	};

	var messagesHandlers = {
		'set-history': function(message){
			var history = message.data;
			for(var i = 0; i < message.data.length; i++){
				window.history.pushState({ link: message.data[i] }, null, '/?app=' + message.data[i]);
			}
		},
		'lightbox-position': function(message){
			var top = message.data.posY + (message.data.viewportHeight - $('.lightbox-window').height()) / 2;
			if(top < 0){
				top = 0;
			}

			$('.lightbox-window').offset({
				top: top
			});

			requireResize();
		},
		'set-style': function(message){
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

			$('<link />', {
					rel: 'stylesheet',
					href: message.data,
					type: 'text/css'
				})
				.prependTo('head')
				.attr('data-portal-style', message.data)
				.on('load', function(){
					updateView();
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
		redirectParent: function(location){
			send({
				name: 'redirect-parent',
				data: location
			});
		},
		requireResize: requireResize,
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

function Navigation($scope, http){
	http.bind('disconnected', function(event){
		messenger.redirectParent('/');
	});

	$scope.redirect = function(url){
		messenger.sendMessage({
			name: 'redirect',
			data: url
		});
	};

	$scope.moveHistory = function(data){
		messenger.sendMessage({
			name: 'move-history',
			data: data
		})
	}
}

$(document).ready(function(){
	"use strict";

	if(parent !== window && $('link[data-portal-style]').length === 0){
		$('body').hide();
	}
	if(parent !== window){
		$('body').on('click', 'a[href]', function(e){
			if($(this).attr('call')){
				return;
			}
			if($(this).attr('href').indexOf('javascript:') !== -1){
				return;
			}
			messenger.sendMessage({
				name: 'redirect',
				data: $(this).attr('href')
			});
			e.preventDefault();
		})
	}

	//automated require resize
	//will wait for all changes in a given time and apply all at once
	//to avoid useless load
	var waitForResize = false;
	function resize(){
			messenger.requireResize();
	}

	$("body").bind("DOMSubtreeModified", function() {
		if(!waitForResize){
			resize();
			setTimeout(function(){
				waitForResize = false;
				resize();
			}, 1000)
			waitForResize = true;
		}
	});
});
