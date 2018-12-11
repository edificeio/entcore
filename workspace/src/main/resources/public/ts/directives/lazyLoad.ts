import { ng, angular } from 'entcore';
declare var window: any
let logged = false;
function lazyLoadImgFunc() {
    return {
        restrict: 'A',
        scope: {
            lazyLoadImg: "="
        },
        link: function (scope, element, attrs) {
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