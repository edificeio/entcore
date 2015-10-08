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
		scrollToId: function(id){
			//jquery doesn't like selecting elements with slashes in their id,
			//whereas native API doesn't care
			var targetElement = document.getElementById(id);
			if(!targetElement){
				return;
			}
			$('html, body').animate({
				scrollTop: $(targetElement).offset().top
			}, 250);
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
				if(($(this).height() + parseInt($(this).css('padding-top')) + parseInt($(this).css('padding-bottom'))) === this.scrollHeight){
					$(this).css({ transition: 'none', height: 'auto' });
				}
				setTimeout(function(){
					$(this).height(this.scrollHeight);
					$(this).css("transition", "");
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
	resizable: function(element, params){
		if(!params){
			params = {};
		}
		if(!params.lock){
			params.lock = {};
		}

		if(element.length > 1){
			element.each(function(index, item){
				ui.extendElement.resizable($(item), params);
			})
		}

		//cursor styles to indicate resizing possibilities
		element.on('mouseover', function(e){
			element.on('mousemove', function(e){
				if(element.data('resizing') || element.data('lock')){
					return;
				}
				var mouse = { x: e.pageX, y: e.pageY };
				var resizeLimits = {
					horizontalRight:  element.offset().left + element.width() + 5 > mouse.x && mouse.x > element.offset().left + element.width() - 15 && element.attr('horizontal-resize-lock') === undefined,
					horizontalLeft: element.offset().left + 5 > mouse.x && mouse.x > element.offset().left - 15 && element.attr('horizontal-resize-lock') === undefined,
					verticalTop: element.offset().top + 5 > mouse.y && mouse.y > element.offset().top - 5 && element.attr('vertical-resize-lock') === undefined,
					verticalBottom: element.offset().top + element.height() + 5 > mouse.y && mouse.y > element.offset().top + element.height() - 5 && element.attr('vertical-resize-lock') === undefined
				};

				var orientations = {
					'ns': resizeLimits.verticalTop || resizeLimits.verticalBottom,
					'ew': resizeLimits.horizontalLeft || resizeLimits.horizontalRight,
					'nwse': (resizeLimits.verticalBottom && resizeLimits.horizontalRight) || (resizeLimits.verticalTop && resizeLimits.horizontalLeft),
					'nesw': (resizeLimits.verticalBottom && resizeLimits.horizontalLeft) || (resizeLimits.verticalTop && resizeLimits.horizontalRight)

				};

				var cursor = '';
				for(var orientation in orientations){
					if(orientations[orientation]){
						cursor = orientation;
					}
				}


				if(cursor){
					cursor = cursor + '-resize';
				}
				element.css({ cursor: cursor });
				element.find('[contenteditable]').css({ cursor: cursor });
			});
			element.on('mouseout', function(e){
				element.unbind('mousemove');
			});
		});

		//actual resize
		element.on('mousedown.resize touchstart.resize', function(e){
			if(element.data('lock') === true || element.data('resizing') === true){
				return;
			}

			$('body').css({
				'-webkit-user-select': 'none',
				'-moz-user-select': 'none',
				'user-select' : 'none'
			});

			var interrupt = false;
			var mouse = {
				y: f.clientY || f.originalEvent.touches[0].clientY,
				x: f.clientX || f.originalEvent.touches[0].clientX
			};
			var resizeLimits = {
				horizontalRight:  element.offset().left + element.width() + 15 > mouse.x && mouse.x > element.offset().left + element.width() - 15 && element.attr('horizontal-resize-lock') === undefined,
				horizontalLeft: element.offset().left + 15 > mouse.x && mouse.x > element.offset().left - 15 && params.lock.horizontal === undefined,
				verticalTop: element.offset().top + 5 > mouse.y && mouse.y > element.offset().top - 15 && element.attr('vertical-resize-lock') === undefined,
				verticalBottom: element.offset().top + element.height() + 5 > mouse.y && mouse.y > element.offset().top + element.height() - 5 && element.attr('vertical-resize-lock') === undefined
			};

			var initial = {
				pos: element.offset(),
				size: {
					width: element.width(),
					height: element.height()
				}
			};
			var parent = element.parents('.drawing-zone');
			var parentData = {
				pos: parent.offset(),
				size: {
					width: parent.width(),
					height: parent.height()
				}
			};

			if(resizeLimits.horizontalLeft || resizeLimits.horizontalRight ||resizeLimits.verticalTop || resizeLimits.verticalBottom){
				element.trigger('startResize');
				e.preventDefault();
				element.data('resizing', true);
				$('.main').css({
					'cursor': element.css('cursor')
				});
				$(window).unbind('mousemove.drag touchmove.start');
				$(window).on('mousemove.resize touchmove.resize', function(e){
					element.unbind("click");
					mouse = {
						y: e.pageY || e.originalEvent.touches[0].pageY,
						x: e.pageX || e.originalEvent.touches[0].pageX
					};
				});

				//animation for resizing
				var resize = function(){
					var newWidth = 0; var newHeight = 0;
					if(resizeLimits.horizontalLeft || resizeLimits.horizontalRight){
						var p = element.offset();
						if(resizeLimits.horizontalLeft){
							var distance = initial.pos.left - mouse.x;
							if(initial.pos.left - distance < parentData.pos.left){
								distance = initial.pos.left - parentData.pos.left;
							}
							if(params.moveWithResize !== false){
								element.offset({
									left: initial.pos.left - distance,
									top: p.top
								});
							}

							newWidth = initial.size.width + distance;
						}
						else{
							var distance = mouse.x - p.left;
							if(element.offset().left + distance > parentData.pos.left + parentData.size.width){
								distance = (parentData.pos.left + parentData.size.width) - element.offset().left - 2;
							}
							newWidth = distance;
						}
						if(newWidth > 0){
							element.width(newWidth);
						}
					}
					if(resizeLimits.verticalTop || resizeLimits.verticalBottom){
						var p = element.offset();
						if(resizeLimits.verticalTop){
							var distance = initial.pos.top - mouse.y;
							if(initial.pos.top - distance < parentData.pos.top){
								distance = initial.pos.top - parentData.pos.top;
							}
							if(params.moveWithResize !== false){
								element.offset({
									left: p.left,
									top: initial.pos.top - distance
								});
							}

							newHeight = initial.size.height + distance;
						}
						else{
							var distance = mouse.y - p.top;
							if(element.offset().top + distance > parentData.pos.top + parent.height()){
								distance = (parentData.pos.top + parentData.size.height) - element.offset().top - 2;
							}
							newHeight = distance;
						}
						if(newHeight > 0){
							element.height(newHeight);
						}
					}
					element.trigger('resizing');
					if(!interrupt){
						requestAnimationFrame(resize);
					}
				};
				resize();

				$(window).on('mouseup.resize touchleave.resize touchend.resize', function(){
					interrupt = true;
					setTimeout(function(){
						element.data('resizing', false);
						element.trigger('stopResize');
					}, 100);
					$(window).unbind('mousemove.resize touchmove.resize');
					$('body').unbind('mouseup.resize touchleave.resize touchend.resize');
					$('.main').css({'cursor': ''})
				});
			}
		});
	},
	draggable: function(element, params){
		if(!params){
			params = {};
		}
		if(!params.lock){
			params.lock = {};
		}

		if(element.length > 1){
			element.each(function(index, item){
				ui.extendElement.draggable($(item), params);
			})
		}

		element.on('touchstart mousedown', function(e){
			if(element.data('lock') === true || (e.target.tagName === 'TEXTAREA' && $(e.target).is(':focus'))){
				return;
			}
			var initialScroll = $(window).scrollTop();
			var interrupt = false;
			if(element.data('resizing') !== true){

				var mouse = mouse = {
					y: e.clientY || e.originalEvent.touches[0].clientY,
					x: e.clientX || e.originalEvent.touches[0].clientX
				};;
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

				$(window).on('touchmove.drag mousemove.drag', function(f){
					moved = true;
					f.preventDefault();
					if((f.clientX || f.originalEvent.touches[0].clientX) === mouse.x &&
						(f.clientY || f.originalEvent.touches[0].clientY) === mouse.y){
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
							'position': 'absolute',
							'transition': 'none'
						});

						setTimeout(moveElement, 5);
					}
					element.unbind("click");
					element.data('dragging', true);
					mouse = {
						y: f.clientY || f.originalEvent.touches[0].clientY,
						x: f.clientX || f.originalEvent.touches[0].clientX
					};
				});

				$('body').on('touchend.drag touchleave.drag mouseup.drag', function(e){
					$('body').css({
						'-webkit-user-select': 'initial',
						'-moz-user-select': 'initial',
						'user-select' : 'initial'
					});
					element.css('transition', '');
					interrupt = true;
					$('body').unbind('mouseup.drag touchend.drag touchleave.drag');
					$(window).unbind('mousemove.drag touchmove.drag');

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