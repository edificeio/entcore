import { $, appPrefix, ng } from 'entcore';
//TODO move to infrafront
export const cssTransitionEnd = ng.directive('cssTransitionEnd', [
    '$parse',
    function ($parse) {
        const transitions = {
            "transition": "transitionend",
            "OTransition": "oTransitionEnd",
            "MozTransition": "transitionend",
            "WebkitTransition": "webkitTransitionEnd"
        };

        const whichTransitionEvent = function () {
            let t, el = document.createElement("fakeelement");
            for (t in transitions) {
                if (el.style[t] !== undefined) {
                    return transitions[t];
                }
            }
        };
        const transitionEvent = whichTransitionEvent();
        return {
            'restrict': 'A',
            'link': function (scope, element, attrs) {
                const expr = attrs['cssTransitionEnd'];
                const fn = $parse(expr);
                element.bind(transitionEvent, function (evt) {
                    const phase = scope.$root.$$phase;
                    if (phase === '$apply' || phase === '$digest') {
                        fn();
                    } else {
                        scope.$apply(fn);
                    }
                });
            },
        };
    }]);