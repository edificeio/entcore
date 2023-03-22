import { ng, $ } from 'entcore';

/**
 * A directive to dynamically adapt the height of an iframe to its content.
 * => When embedded in a lightbox, the lightbox height will follow.

 	<lightbox show="true">
		<iframe adaptive-height src="/an/url/from/same/origin"</iframe>
	</lightbox>
 */
export interface AdaptiveHeightScope {
    $on(a?, b?)
}
export const adaptiveHeight = ng.directive('adaptiveHeight', [() => {
    return {
        restrict: 'A',
        link: (scope: AdaptiveHeightScope, element, attributes) => {
            if(typeof ResizeObserver !== "undefined") {
                const observer:ResizeObserver = new ResizeObserver( entries => {
                    for (const entry of entries) {
                        if (entry.contentBoxSize) {
                            // Content-box value, if available
                            element.css({"height": Math.min(entry.contentBoxSize[0].blockSize,800)+'px'});
                            break;
                        }
                        if (entry.contentRect) {
                            // Content-rect value, if available
                            element.css({"height": Math.min(entry.contentRect.height+10,800)+'px'});
                            break;
                        }
                        if (entry.devicePixelContentBoxSize) {
                            // Best value
                            element.css({"height": Math.min(entry.devicePixelContentBoxSize[0].blockSize,800)+'px'});
                            break;
                        }
                        // Default value, if size is not well known
                        element.css({"height": Math.min(entry.target.scrollHeight+110||1000,800)+'px'});
                    }
                });

                // Wait for the iframe content to be loaded then observe
                element.on('load', () => {
                    const html:HTMLHtmlElement = element[0].contentDocument.body.parentElement as HTMLHtmlElement;
                    observer.observe(html);
                    scope.$on('$destroy', function () {
                        observer.unobserve(html);
                    });
                });
            }
        }
    }
}]);
