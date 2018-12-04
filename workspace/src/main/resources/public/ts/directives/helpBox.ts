import { idiom as lang, ng } from 'entcore';
import { Subject, Observable } from 'rxjs';
import { startWith } from 'rxjs/operator/startWith';
//TODO move to infrafront? with i18n and css
/**
 * HELPERS
 */

declare var jQuery;
const lockScroll = function (onMouseWheel) {
    const $window = jQuery(window);
    $window.on("mousewheel DOMMouseScroll", onMouseWheel);
}
function unlockScroll(onMouseWheel) {
    const $window = jQuery(window);
    $window.off("mousewheel DOMMouseScroll", onMouseWheel);
}
function computePosition(direction: "left" | "right" | "top" | "bottom", rect, height: number) {
    let left: number = null, top: number = null, right: number = null, bottom: number = null;
    const offset = 25;
    switch (direction) {
        case "left":
            right = (rect.left - offset);
            top = rect.top;
            break;
        case "right":
            left = (rect.right + offset);
            top = rect.top;
            break;
        case "top":
            left = rect.left;
            bottom = (rect.top - offset);
            break;
        case "bottom":
            left = rect.left;
            top = (rect.bottom + offset);
            break;
    }
    const toString = function (val) {
        return val != null && (val + "px")
    }
    top = ensureIsInViewReport(top, height)
    return {
        position: "fixed", top: toString(top), left: toString(left), bottom: toString(bottom), right: toString(right)
    };
}

function ensureIsInViewReport(top: number, height: number) {
    //ensure top
    if (top < 0) {
        return 0;
    }
    //ensure bottom
    const windowHeight = jQuery(window).height()
    const diff = (windowHeight - (top + height))
    if (diff < 0) {
        return top + diff
    }
    return top;
}

function needVerticalScroll(el: HTMLElement, parent: HTMLElement) {
    const rect = el.getBoundingClientRect();
    const parentRect = parent.getBoundingClientRect()
    //
    const relativePos = {} as any;
    relativePos.bottom = rect.bottom - parentRect.bottom;
    if (rect.top < parentRect.top) {
        return rect.top - parentRect.top;//delta negatif (up)
    }
    if (rect.bottom > parentRect.bottom) {
        return rect.bottom - parentRect.bottom;//delta positif (down)
    }
    return 0;
}
function synchronizeCssStyles(src: HTMLElement, destination: HTMLElement, recursively: boolean) {
    const style = window.getComputedStyle ? window.getComputedStyle(src) : {};
    jQuery(destination).css({ ...style })
    if (recursively) {
        const vSrcElements = jQuery(src).find("*");
        const vDstElements = jQuery(destination).find("*");

        for (let i = vSrcElements.length; i--;) {
            const vSrcElement = vSrcElements[i];
            const vDstElement = vDstElements[i];
            const vStyle = window.getComputedStyle ? window.getComputedStyle(vSrcElement) : {};
            jQuery(vDstElement).removeClass();
            jQuery(vDstElement).css({ ...vStyle })
        }
    }
}
function hasScrollBar(element: HTMLElement, direction: "vertical" | "horizontal") {
    const jElement = jQuery(element);
    if (direction == 'vertical') {
        return element.scrollHeight > jElement.innerHeight();
    }
    else if (direction == 'horizontal') {
        return element.scrollWidth > jElement.innerWidth();
    }
    return false;
}
function makeVisible(element: HTMLElement) {
    const jElement = jQuery(element);
    const parents = jElement.parents();
    for (let i = 0; i < parents.length; i++) {
        const parent = parents[i];
        if (hasScrollBar(parent, "vertical")) {
            const delta = needVerticalScroll(element, parent)
            if (delta != 0) {
                const jParent = jQuery(parent);
                const top = jParent.scrollTop() + delta;
                jParent.animate({ scrollTop: top }, { duration: 'medium', easing: 'swing' });
            }
        }
    }
}
/**
 * 
 */
interface Step {
    title: string
    content: string
    priority: number
    highlight(cb: () => void)
    unhighlight();
    getHtml(): HTMLElement
}
let currentStep: Step = null;
const stepsManager = {
    steps: [] as Step[],
    onAdd: new Subject(),
    onCurrentChange: new Subject(),
    setCurrentStep(s: Step) {
        currentStep = s;
        stepsManager.onCurrentChange.next()
    },
    getCurrentStep() {
        return currentStep;
    },
    isCurrentContent(content: string) {
        return stepsManager.hasCurrentStep() && currentStep.content == content;
    },
    hasCurrentStep() {
        return !!currentStep;
    },
    addStep(step: Step) {
        const founded = stepsManager.steps.find(st => st.content == step.content);
        if (founded) {
            Object.assign(founded, step)
        } else {
            stepsManager.steps.push(step);
            stepsManager.steps = stepsManager.steps.sort((s1, s2) => {
                return s1.priority - s2.priority;
            })
            stepsManager.onAdd.next()
        }
    },
    getNextStep(step: Step) {
        let index = stepsManager.steps.findIndex(f => f === step);
        return stepsManager.steps[index + 1];
    },
    hasNextStep(step: Step) {
        let founded = stepsManager.getNextStep(step);
        return !!founded;
    },
    hightlight(cb: () => void) {
        for (let cu of stepsManager.steps) {
            if (cu === currentStep) {
                cu.highlight(cb)
            } else {
                cu.unhighlight()
            }
        }
    }
};
export interface HelpBoxScope {
    steps: Step[]
    title: string
    display: boolean
    canClose: boolean
    direction: "left" | "right" | "top" | "bottom"
    getTitle(): string
    getContent(): string
    isCurrentStep(step: Step)
    onFinished?: () => void
    goTo(step: Step)
    hasNextStep()
    next()
    close()
    start()
    visible(): boolean
    //
    $watch(a?, b?)
}
export const helpBox = ng.directive('helpBox', ['$timeout', ($timeout) => {
    return {
        restrict: 'E',
        scope: {
            onFinished: '&',
            canClose: '@',
            display: '@',
            title: '@',
            direction: "@"
        },
        template: ` 
        <section class="helpbox-highlight-message" ng-if="visible()">
            <div style="display:none" class="content no-margin message-box block-container four minwidth-400">
                <div class="twelve cell">
                <div class="reduce-block-eight">
                    <div class="block-container flex-row center-component ">
                        <h3 class="left-text">
                            <span class="no-style">[[getTitle()]]</span>
                        </h3>
                        <p class="eleven justified-text">
                            <span class="no-style">[[getContent()]]</span>
                        </p>
                    </div>
                    <div class="flex-row align-center">
                        <nav class="dots flex-all-remains align-center">
                            <ul>
                                <li ng-repeat="step in steps" class="dot active" ng-click="goTo(step)" ng-class="{ active: isCurrentStep(step) }"></li>
                            </ul>
                        </nav>
                        <div class="right-magnet align-center">
                        <button class="right-magnet no-margin-bottom" ng-click="next()" ng-if="hasNextStep()">
                            <span class="no-style"><i18n>helpbox.next</i18n></span>
                        </button>
                        <button class="right-magnet no-margin-bottom" ng-click="close()" ng-if="!hasNextStep()">
                            <span class="no-style"><i18n>helpbox.finish</i18n></span>
                        </button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="close-lightbox" ng-if="!hasNextStep()" ng-click="close()"><i class="close-2x"></i></div></div>
        </section>
        `,
        compile: function (element, attributes) {
            return {
                pre: function (scope: HelpBoxScope, element, attributes, controller, transcludeFn) {
                    //scroll
                    let started = false;
                    function onMouseWheel(e) {
                        e.preventDefault();
                    }

                    //
                    const isDisplay = () => {
                        return (scope.display === true || scope.display as any === "true");
                    }
                    scope.start = function () {
                        if (started || !isDisplay() || !stepsManager.steps.length) {
                            return;
                        }
                        //must be before got
                        started = true;
                        //lockScroll(onMouseWheel)
                        scope.goTo(stepsManager.steps[0])
                    }
                    scope.close = () => {
                        scope.display = false;
                        stepsManager.setCurrentStep(null)
                        stepsManager.hightlight(() => {

                        })
                        //unlockScroll(onMouseWheel)
                        scope.onFinished && scope.onFinished()
                        started = false
                    }
                    //
                    let firstTime = true;
                    scope.goTo = (step: Step) => {
                        stepsManager.setCurrentStep(step);
                        stepsManager.hightlight(() => {
                            const el = stepsManager.getCurrentStep().getHtml();
                            const rect = el.getBoundingClientRect();
                            const direction = scope.direction || "right";
                            const content = element.find(".content");
                            const height = content.height()
                            const position = computePosition(direction, rect, height)
                            content.css(position)
                            if (firstTime) {
                                content.css({ display: "block" })
                                firstTime = false
                            }
                        })
                        if (stepsManager.hasCurrentStep()) {
                            stepsManager.getCurrentStep().title = stepsManager.getCurrentStep().title || lang.translate(scope.title);
                        }
                    }
                    //must be after scope.goto
                    (stepsManager.onAdd as Observable<any>).debounceTime(600).subscribe(e => {
                        $timeout(() => {
                            scope.start()
                        })
                    })
                    stepsManager.onAdd.next()//trigger first time
                    scope.$watch("display", () => {
                        stepsManager.onAdd.next()
                    })
                    //
                    scope.getTitle = function () {
                        return stepsManager.hasCurrentStep() && stepsManager.getCurrentStep().title;
                    }
                    scope.getContent = function () {
                        return stepsManager.hasCurrentStep() && stepsManager.getCurrentStep().content;
                    }
                    //
                    scope.hasNextStep = () => {
                        return stepsManager.hasNextStep(stepsManager.getCurrentStep());
                    }
                    scope.next = () => {
                        let next = stepsManager.getNextStep(stepsManager.getCurrentStep());
                        scope.goTo(next);
                    }
                    scope.isCurrentStep = (step: Step) => {
                        return stepsManager.getCurrentStep() === step;
                    }
                    scope.visible = function () {
                        return isDisplay() && !!stepsManager.getCurrentStep();
                    }
                },
                post: function (scope, element, attributes, controller, transcludeFn) {

                }
            }
        }
    }
}])

export interface HelpBoxStepScope {
    helpBoxStep: string
    helpBoxStepTitle?: string
    helpBoxStepPriority?: number
}

export const helpBoxStep = ng.directive('helpBoxStep', ['$timeout', ($timeout) => {
    return {
        restrict: 'A',
        scope: {
            helpBoxStepTitle: '@',
            helpBoxStepPriority: '@',
            helpBoxStep: '@'
        },
        link: function (scope, element, attributes) {
            element.removeClass("helpbox-highlight")
            let clone = null;
            if (scope.helpBoxStep) {
                stepsManager.addStep({
                    content: lang.translate(scope.helpBoxStep),
                    title: scope.helpBoxStepTitle ? lang.translate(scope.helpBoxStepTitle) : null,
                    priority: scope.helpBoxStepPriority || 0,
                    getHtml() {
                        return (element[0] as HTMLElement)
                    },
                    highlight(cb: () => void) {
                        clone && jQuery(clone).remove();
                        const el = (element[0] as HTMLElement);
                        makeVisible(el)
                        setTimeout(() => {
                            clone = jQuery(el).clone();
                            jQuery("body").append(clone);
                            clone.removeClass();
                            clone.addClass("helpbox-highlight")
                            synchronizeCssStyles(el, clone[0], true);
                            const reposition = function () {
                                const rect = el.getBoundingClientRect();
                                clone.css({
                                    'position': 'fixed',
                                    'top': rect.top,
                                    'left': rect.left,
                                    'width': rect.width,
                                    'height': rect.height,
                                    'margin': 0
                                })
                                cb();
                            }
                            this["reposition"] = reposition;
                            jQuery(window).scroll(this["reposition"]);
                            jQuery(window).on('resize', this["reposition"]);
                            reposition();
                        }, 300)
                    },
                    unhighlight() {
                        clone && jQuery(clone).remove();
                        jQuery(window).off('scroll', this["reposition"]);
                        jQuery(window).off('resize', this["reposition"]);
                    }
                });
            }
        }
    }
}])
