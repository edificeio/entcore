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
                        if (entry.devicePixelContentBoxSize) {
                            element.css({"height": Math.min(entry.devicePixelContentBoxSize[0].blockSize,800)+'px'});
                        } else {
                            // Default if size is not well known
                            element.css({"height": Math.min(entry.target.scrollHeight+110||1000,800)+'px'});
                        }
                    }
                });

                // Wait for the iframe content to be loaded then observe
                element.on('load', () => {
                    const html:HTMLHtmlElement = element[0].contentDocument.body.parentElement as HTMLHtmlElement;
                    observer.observe(html, {box:"device-pixel-content-box"});
                    scope.$on('$destroy', function () {
                        observer.unobserve(html);
                    });
                });
            }
        }
    }
}]);
