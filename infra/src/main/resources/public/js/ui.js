// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
var ui = (function(){
	var mainLightbox = {
		show: function(){
			$('.lightbox-backdrop').fadeIn();
			$('.lightbox-window').fadeIn();
			$('.lightbox-window').css({
				'margin-top': 0,
				'position': 'fixed',
				top: 0
			})
			var offset = $('.lightbox-window').offset();
			$('.lightbox-window').css({
				'position': 'absolute',
				'top': offset.top,
				'margin-top': '100px'
			});

			var that = this;
			$('body').on('click', '.lightbox-backdrop', function(){
				that.hide();
			});
		},
		hide: function(){
			$('.lightbox-backdrop').fadeOut();
			$('.lightbox-window').fadeOut();
		}
	};

	var uiInterface = {
		scrollToTop: function(){
			var scrollUp = function(){
				var scrollTop = window.scrollY || document.getElementsByTagName('html')[0].scrollTop;
				if(scrollTop <= $('body').offset().top){
					return;
				}
				window.scrollTo(0, scrollTop - parseInt(scrollTop / 10) - 1);
				setTimeout(scrollUp, 10);
			};
			scrollUp();
		},
		showLightbox: function(){
			mainLightbox.show();
		},
		hideLightbox: function(){
			mainLightbox.hide();
		},
		updateAvatar: function(){
			var scope = angular.element(document.getElementById('my-photo')).scope();
			scope.refreshAvatar();
		},
		setStyle: function(stylePath){
			if($('#theme').length === 0){
				var style = $('<link>', {
					rel: 'stylesheet',
					type: 'text/css',
					href: stylePath + 'theme.css',
					id: 'theme'
				});
				var favicon = $('<link>', {
					rel: 'icon',
					href: stylePath + '../img/illustrations/favicon.ico'
				});
				style.on('load', function(){
					$('body').show();
				});
				$('head')
					.append(style)
					.append(favicon);
				setTimeout(function(){
					$('body').show();
				}, 300);
			}
			else{
				$('#theme').attr('href', stylePath + 'theme.css');
			}
		}
	};

	$(document).ready(function(){
		if(!document.createEvent){
			return;
		}
		var evt = document.createEvent("Event");
		evt.initEvent("ui-ready", true, false);
		window.dispatchEvent(evt);

		$('.display-buttons i').on('click', function(){
			$(this).parent().find('i').removeClass('selected');
			$(this).addClass('selected');
		});


		var resizeTextarea = function(){
			$(this).height(1);
			$(this).height(this.scrollHeight - 1);
		};

		$('body').on('keydown', 'textarea.inline-editing', resizeTextarea);
		$('body').on('keyup', 'textarea.inline-editing', resizeTextarea);
		$('body').on('focus', 'textarea.inline-editing', resizeTextarea);

		$('body').on('click', '[data-reload]', function(){
			setTimeout(function(){
				window.location.reload();
			}, 200);
		});

		$('body').on('click', '.lightbox-window .close-lightbox i, .lightbox-window .lightbox-buttons .cancel, .lightbox-window .cancel', function(){
			ui.hideLightbox();
		});

		$('.remove-fout').removeClass('remove-fout');

		$('body').on('click', '.select-file input[type!="file"], .select-file button, .file-selector', function(e){
			var inputFile = $(this).parent().find('input[type=file]');
			if($(this).attr('for')){
				inputFile = $('#' + $(this).attr('for'));
			}

			if(inputFile.length === 0){
				inputFile = $('input[type=file]');
			}
			if($(this).attr('type') === 'text'){
				if(!$(this).data('changed')){
					inputFile.click();
				}
			}
			else{
				inputFile.click();
			}
			$('[data-display-file]').data('changed', true);

			inputFile.on('prettyinput.change', function(){
				var displayElement = inputFile.parent().parent().find('[data-display-file]');
				var fileUrl = $(this).val();
				if(fileUrl.indexOf('fakepath') !== -1){
					fileUrl = fileUrl.split('fakepath')[1];
					fileUrl = fileUrl.substr(1);
					fileUrl = fileUrl.split('.')[0];
				}
				if(displayElement.length > 0 && displayElement[0].tagName === 'INPUT'){
					displayElement.val(fileUrl);
				}
				else{
					displayElement.text(fileUrl);
				}
				$(this).unbind('prettyinput.change');
			});

			e.preventDefault();
		});

		$('.search input[type=text]').on('focus', function(){
			$(this).val(' ');
		})

		$('body').on('click', '.icons-select .current', function(e){
			e.stopPropagation();
			var select = $(this).parent();
			var optionsList = select.children('.options-list');

			if($(this).hasClass('editing')){
				$(this).removeClass('editing');
				optionsList.removeClass('toggle-visible');
				$(document).unbind('click.close');
				e.preventDefault();
				return;
			}

			var that = this;
			$(that).addClass('editing');
			optionsList.addClass('toggle-visible');
			optionsList.find('.option').on('click', function(){
				$(that).removeClass('editing');
				$(that).data('selected', $(this).data('value'));
				$(that).html($(this).html());
				optionsList.removeClass('toggle-visible');
				select.change();
			});

			$(document).on('click.close', function(e){
				$(that).removeClass('editing');
				optionsList.removeClass('toggle-visible');
				$(document).unbind('click.close');
				e.preventDefault();
			})
		});

		//CSS transitions expansions
		$('body').on('click', 'article.preview', function(e){
			if($(this).hasClass('expanded')){
				setTimeout(function(){
					$(this).height(this.scrollHeight);
				}.bind(this), 100);
			}
			else{
				$(this).removeAttr('style');
			}
		});
	});

	return uiInterface;
}());



// Remove event in JQuery
(function($){
	$.event.special.removed = {
		remove: function(o) {
			if (o.handler) {
				o.handler()
			}
		}
	}
})(jQuery)


ui.extendElement = {
	draggable: function(element, params){
		element.on('mousedown', function(e){
			if(element.data('lock') === true || (e.target.tagName === 'TEXTAREA' && $(e.target).is(':focus'))){
				return;
			}
			var initialScroll = $(window).scrollTop();
			var interrupt = false;
			if(element.data('resizing') !== true){

				var mouse = { y: e.clientY, x: e.clientX };
				var elementDistance = {
					y: mouse.y - element.offset().top,
					x: mouse.x - element.offset().left
				};
				var moved = false;

				var moveElement = function(){
					var parent = element.parents('.drawing-zone');
					var parentPosition = parent.offset();
					var boundaries = {
						left: -Infinity,
						top: -Infinity,
						right: Infinity,
						bottom: Infinity
					};

					if(parentPosition) {
						boundaries = {
							left: parentPosition.left,
							top: parentPosition.top,
							right: parentPosition.left + parent.width() - element.width(),
							bottom: parentPosition.top + parent.height() - element.height()
						};
					}

					var newOffset = {
						top: parseInt((mouse.y - elementDistance.y) + ($(window).scrollTop() - initialScroll)),
						left: parseInt(mouse.x - elementDistance.x)
					};

					if(mouse.x < boundaries.left + elementDistance.x && element.width() < parent.width()){
						newOffset.left = boundaries.left;
					}
					if(mouse.x > boundaries.right + elementDistance.x && element.width() < parent.width()){
						newOffset.left = boundaries.right - 2
					}
					if(mouse.y < boundaries.top + elementDistance.y && element.height() < parent.height()){
						newOffset.top = boundaries.top;
					}
					if(mouse.y > boundaries.bottom + elementDistance.y && element.height() < parent.height()){
						newOffset.top = boundaries.bottom - 2;
					}

					if(params.lock && params.lock.vertical){
						newOffset.top = element.offset().top;
					}
					if(params.lock && params.lock.horizontal){
						newOffset.left = element.offset().left;
					}
					element.offset(newOffset);

					if(params && typeof params.tick === 'function'){
						params.tick();
					}

					if(!interrupt){
						requestAnimationFrame(moveElement);
					}
				};

				$(window).on('mousemove.drag', function(f){
					moved = true;
					e.preventDefault();
					if(f.clientX === mouse.x && f.clientY === mouse.y){
						return;
					}
					if(!element.data('dragging')){
						if(params && typeof params.startDrag === 'function'){
							params.startDrag();
						}
						
						element.trigger('startDrag');
						$('body').css({
							'-webkit-user-select': 'none',
							'-moz-user-select': 'none',
							'user-select' : 'none'
						});
						if(element.css('position') === 'relative'){
							element.css({ top: element.position().top, left: element.position().left });
						}
						element.css({
							'position': 'absolute'
						});

						setTimeout(moveElement, 5);
					}
					element.unbind("click");
					element.data('dragging', true);
					mouse = {
						y: f.clientY,
						x: f.clientX
					};
				});

				$('body').on('mouseup.drag', function(e){
					$('body').css({
						'-webkit-user-select': 'initial',
						'-moz-user-select': 'initial',
						'user-select' : 'initial'
					});
					interrupt = true;
					$('body').unbind('mouseup.drag');
					$(window).unbind('mousemove.drag');

					setTimeout(function(){
						if(element.data('dragging')){
							element.trigger('stopDrag');
							element.data('dragging', false);
							if(params && typeof params.mouseUp === 'function' && moved){
								params.mouseUp();
							}
						}
					}, 100);
				});
			}
		});
	}
}