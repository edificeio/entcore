import { ng, angular } from 'entcore';
declare var window: any
let logged = false;
const FALLBACK_IMAGE_SRC =
    "data:image/svg+xml;utf8," +
    encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">' +
        '<rect x="3" y="3" width="18" height="18" rx="2" fill="none" stroke="#c6cbd4" stroke-width="1.6"/>' +
        '<circle cx="8.5" cy="8.5" r="1.5" fill="#c6cbd4"/>' +
        '<path d="M21 15l-5-5L5 21" fill="none" stroke="#c6cbd4" stroke-width="1.6"/>' +
        '</svg>',
    );
function lazyLoadImgFunc() {
    return {
        restrict: 'A',
        scope: {
            lazyLoadImg: "="
        },
        link: function (scope, element, attrs) {
            const imgEl = angular.element(element)[0];
            imgEl.addEventListener("error", () => {
                if (imgEl.dataset.fallbackApplied) return;
                imgEl.dataset.fallbackApplied = "true";
                imgEl.src = FALLBACK_IMAGE_SRC;
            });
            const win = window as any;
            if (!win.IntersectionObserver ||
                !win.IntersectionObserverEntry ||
                !win.IntersectionObserverEntry.prototype) {
                // load polyfill 
                if (!logged){
                    console.warn("lazy load is disabled")
                }
                logged = true;
                //set src on img
                const img = angular.element(element)[0];
                img.src = scope.lazyLoadImg;
            } else {
                if (!logged){
                    console.info("lazy load is enabled")
                }
                logged = true;
                //
                const loadImg = (changes) => {
                    changes.forEach(change => {
                        if (change.intersectionRatio > 0) {
                            if(!change.target.src || change.target.src.indexOf(scope.lazyLoadImg)==-1){
                                change.target.src = scope.lazyLoadImg;
                            }
                        }
                    })
                }
                const observer = new IntersectionObserver(loadImg)
                const img = angular.element(element)[0];
                observer.observe(img)
            }
        }
    }
}
export const lazyLoadImg = ng.directive('lazyLoadImg', lazyLoadImgFunc)