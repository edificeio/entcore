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
			var appUrl = $('.logo')
				.attr('data-link');

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

	var send = function(frame, message){
		var origin = window.location.href;
		if($(frame).attr('src').indexOf('http') !== -1){
			origin = $(frame).attr('src');
		}
		$(frame)[0].contentWindow.postMessage(JSON.stringify(message), origin);
	};

	var messagesHandlers = {
		redirect: function(message){
			navigation.redirect(message.data);
		},
		resize: function(message){
			$('#applications').height(message.data.height);
		},
		'where-lightbox': function(message){
			if($('#lightbox-marker').length > 0){
				var fixedPosition = $('#lightbox-marker').offset().top;
				var iframePosition = $('#applications').offset().top;

				send('#applications', {
					name: 'lightbox-position',
					data: {
						posY: fixedPosition - iframePosition,
						viewportHeight: $(window).height()
					}
				});
			}
		},
		lightbox: function(message){
			//We can't put a black background on the whole view (iframe + container) without killing the positioning,
			//therefore we're dropping the opacity of the elements and darkening the background to
			//get the same result
			$('header').addClass('lightbox-header');
			$('body').addClass('lightbox-body');
			$('section.main').addClass('lightbox-main');
			$('<div></div>')
				.attr('id', 'lightbox-marker')
				.css({
					position: 'fixed',
					top: '0px'
				})
				.appendTo('body');
			var close = this.closeLightbox;
			$('body').one('click.lightbox', function(){
				close();
				$('iframe').attr('src', $('iframe').attr('src'));
			});
		},
		'close-lightbox': function(message){
			$('header').removeClass('lightbox-header');
			$('body').removeClass('lightbox-body');
			$('section.main').removeClass('lightbox-main');
			$('body').unbind('click.lightbox');
		},
		notify: function(message){
			humane.spawn({ addnCls: 'humane-original-' + message.data.type })(lang.translate(message.data.message));
		},
		'redirect-parent': function(message){
			window.location.href = message.data;
		},
		'update-avatar': function(){
			ui.updateAvatar();
		}
	};

	if(window.addEventListener){
		window.addEventListener('message', function(messageData){
			var message = JSON.parse(messageData.data);
			messagesHandlers[message.name](message);
		});
	}

	return {
		sendMessage: send
	};
}());

function Navigation($scope){
	"use strict";

	navigation.applyHash();
	$scope.redirect = function(url){
		navigation.redirect(url);
	};
}

function Account($scope, http){
	"use strict";

    $scope.pictureVersion = 0;

	$scope.refreshAvatar = function(){
		http.get('/userbook/api/person').done(function(result){
			$scope.avatar = result.result['0'].photo;
			$scope.pictureVersion = $scope.pictureVersion + 1;
			$scope.$apply();
		});
	};

	$scope.refreshAvatar();
}

window.addEventListener('hashchange', function(){
	"use strict";
	navigation.applyHash();
});

window.addEventListener('load', function(){
	"use strict";
	$('.remove-fout').removeClass('remove-fout');
});