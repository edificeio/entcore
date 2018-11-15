import { idiom as lang, ng } from 'entcore';
import { Subject, Observable } from 'rxjs';
import { startWith } from 'rxjs/operator/startWith';
//TODO move to infrafront? with i18n and css

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
declare var jQuery;
type Position = { position: string, left: string, top: string, right: string, bottom: string }
export interface HelpBoxScope {
    steps: Step[]
    title: string
    display: boolean
    canClose: boolean
    direction: "left" | "right" | "top" | "bottom"
    getTitle(): string
    getContent(): string
    currentPosition(): Position
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
            <div class="content no-margin message-box block-container four" ng-style="currentPosition()">
                <div class="twelve cell">
                <div class="reduce-block-eight">
                    <div class="block-container flex-row center-component ">
                        <h3 class="centered-text">
                            <span class="no-style">[[getTitle()]]</span>
                        </h3>
                        <h4 class="eleven justified-text">
                            <span class="no-style">[[getContent()]]</span>
                        </h4>
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
            <div class="close-lightbox" ng-if="canClose" ng-click="close()"><i class="close-2x"></i></div></div>
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
                    const lockScroll = function () {
                        const $window = jQuery(window);
                        $window.on("mousewheel DOMMouseScroll", onMouseWheel);
                    }
                    const unlockScroll = function () {
                        const $window = jQuery(window);
                        $window.off("mousewheel DOMMouseScroll", onMouseWheel);
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
                        lockScroll()
                        scope.goTo(stepsManager.steps[0])
                    }
                    scope.close = () => {
                        scope.display = false;
                        stepsManager.setCurrentStep(null)
                        stepsManager.hightlight(() => {

                        })
                        unlockScroll()
                        scope.onFinished && scope.onFinished()
                        started = false
                    }
                    scope.goTo = (step: Step) => {
                        stepsManager.setCurrentStep(step);
                        stepsManager.hightlight(() => {
                            $timeout(() => {
                                const el = stepsManager.getCurrentStep().getHtml();
                                const rect = el.getBoundingClientRect();
                                const direction = scope.direction || "right";
                                let left = "", top = "", right = "", bottom = "";
                                let margin = 25;
                                switch (direction) {
                                    case "left":
                                        right = (rect.left - margin) + "px";
                                        top = rect.top + "px";
                                        break;
                                    case "right":
                                        left = (rect.right + margin) + "px";
                                        top = rect.top + "px";
                                        break;
                                    case "top":
                                        left = rect.left + "px";
                                        bottom = (rect.top - margin) + "px";
                                        break;
                                    case "bottom":
                                        left = rect.left + "px";
                                        top = (rect.bottom + margin) + "px";
                                        break;
                                }
                                position = setCachePos({
                                    position: "fixed", top, left, bottom, right
                                })
                            }, 300)
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
                    let cachePos: Position = null;
                    const setCachePos = function (pos: Position) {
                        if (!cachePos
                            || cachePos.bottom != pos.bottom
                            || cachePos.left != pos.left
                            || cachePos.position != pos.position
                            || cachePos.right != pos.right
                            || cachePos.top != pos.top
                        ) {
                            cachePos = pos;
                        }
                        return cachePos;
                    }
                    scope.getTitle = function () {
                        return stepsManager.hasCurrentStep() && stepsManager.getCurrentStep().title;
                    }
                    scope.getContent = function () {
                        return stepsManager.hasCurrentStep() && stepsManager.getCurrentStep().content;
                    }
                    let position = null;
                    scope.currentPosition = function () {
                        return position;
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

function isElementInViewport(el: HTMLElement) {
    const rect = el.getBoundingClientRect();
    return (
        rect.top >= 0 &&
        rect.left >= 0 &&
        rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /*or $(window).height() */
        rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
    );
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
                        if (!isElementInViewport(el)) {
                            el.scrollIntoView()
                        }
                        setTimeout(() => {
                            clone = jQuery(el).clone();
                            jQuery("body").append(clone);
                            clone.addClass("helpbox-highlight")
                            const rect = el.getBoundingClientRect();
                            clone.css({
                                'position': 'fixed',
                                'top': rect.top,
                                'left': rect.left,
                                'width': rect.width,
                                'height': rect.height
                            })
                            cb();
                        }, 300)
                    },
                    unhighlight() {
                        clone && jQuery(clone).remove();
                    }
                });
            }
        }
    }
}])
