import { $, appPrefix, ng } from 'entcore';

export const fileViewer = ng.directive('fileViewer', () => {
	return {
		restrict: 'E',
		scope: {
			ngModel: '='
		},
		templateUrl: '/workspace/public/template/directives/file-viewer.html',
		link: function(scope, element, attributes){
			scope.contentType = scope.ngModel.metadata.role;
			scope.isFullscreen = false;

			scope.download = function(){
				window.location.href = scope.ngModel.link;
			};
			let renderElement;
            let renderParent;
            
            scope.editImage = () => {
                scope.$parent.openedFolder.content.forEach(d => d.selected = false);
                scope.ngModel.selected = true;
                scope.$parent.display.editImage = true;
            }

			scope.fullscreen = (allow) => {
				scope.isFullscreen = allow;
				if(allow){
					let container = $('<div class="fullscreen-viewer"></div>');
					container.hide();
					container.on('click', function(e){
						if(!$(e.target).hasClass('render')){
							scope.fullscreen(false);
							scope.$apply('isFullscreen');
						}
					});
					element.children('.embedded-viewer').addClass('fullscreen');
					renderElement = element
						.find('.render');
					renderParent = renderElement.parent();

					renderElement
						.addClass('fullscreen')
						.appendTo(container);
					container.appendTo('body');
					container.fadeIn();
					if(typeof scope.render === 'function'){
						scope.render();
					}
				}
				else{
					renderElement.removeClass('fullscreen').appendTo(renderParent);
					element.children('.embedded-viewer').removeClass('fullscreen');
					var fullscreenViewer = $('body').find('.fullscreen-viewer');
					fullscreenViewer.fadeOut(400, function(){
						fullscreenViewer.remove();
					});

					if(typeof scope.render === 'function'){
						scope.render();
					}
				}
			}
		}
	}
});