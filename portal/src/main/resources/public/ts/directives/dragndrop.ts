import { ng } from "entcore";

import { polyfill } from "mobile-drag-drop";
// optional import of scroll behaviour
import { scrollBehaviourDragImageTranslateOverride } from "mobile-drag-drop/scroll-behaviour";

var ua = navigator.userAgent.toLowerCase();
var isAndroid = ua.indexOf("android") > -1; //&& ua.indexOf("mobile");
if (isAndroid) {
    polyfill({
        // use this to make use of the scroll behaviour
        dragImageTranslateOverride: scrollBehaviourDragImageTranslateOverride,
        tryFindDraggableTarget: (event: TouchEvent) => {
            // console.log("android, touch event", event);
            let target = event.target as HTMLElement;
            while (target && !target.getAttribute('ode-drag-value')) {
                target = target.parentElement;
            }
            // console.log("android, touch target found", target);
            return target;
        },
        dragImageSetup: (element: HTMLElement) => {
            // console.log("drag image elem", element);
            return element;
        }
    });
}


/**
 * Native Drag'n'Drop feature
 *
 * usage :
 *
 *  As simple as this :
 *
 *  <span ode-drag-value="some text value">Drag this</span>
 *  <div ode-drop-target ode-ondrop="myFunction($value)">Drop here</div>
 *
 *  Add `ode-drag-target` and `ode-drop-target` parameters to restrict where elements can be dropped :
 *
 *  <span ode-drag-value="Value 1" ode-drag-target="someTarget1">Drag this in 1</span>
 *  <span ode-drag-value="Value 2" ode-drag-target="someTarget2">Drag this in 2</span>
 *
 *  <div ode-drop-target="someTarget1" ode-ondrop="myFunction($value)">Drop here 1</div>
 *  <div ode-drop-target="someTarget2" ode-ondrop="myFunction($value)">Drop here 2</div>
 *  <div ode-drop-target="['someTarget1', 'someTarget2']" ode-ondrop="myFunction($value)">Drop here 1 or 2</div>
 *
 */

export interface OdeDragValueScope {
    odeDragValue: string;
    odeDragTarget?: string;
}

export const odeDragTargetDirective = ng.directive('odeDragTarget', () => {
    return {
        restrict: 'A',
        scope: {
            odeDragValue: '=',
            odeDragTarget: '@'
        },
        link: (scope: OdeDragValueScope, element /* JQuery */, attrs) => {
            const domElement = element.get(0);
            // Apply Drag events
            const applyDragEvents = (el: Element) => {
                // console.log("[odeDragValue] setup drag-target for element", el);
                el.setAttribute('draggable', 'true');
                el.addEventListener('dragstart', function (e: DragEvent) {
                    // console.log("[odeDragValue] dragstart on", e.target);
                    e.dataTransfer.setData('text', (scope.odeDragTarget || "<all>") + "/" + scope.odeDragValue); // IE forces to use `text` as mime type
                    // Find available drop zones
                    var availableDropTargets: NodeListOf<Element>;
                    if (scope.odeDragTarget) { availableDropTargets = document.querySelectorAll(`[ode-drop-target="${scope.odeDragTarget}"]`) }
                    else { availableDropTargets = document.querySelectorAll(`[ode-drop-target]`) }
                    // console.log("[odeDragValue] available drop targets", availableDropTargets);
                    Array.prototype.forEach.call(availableDropTargets, dropTarget => { // Can't use NodeList.forEach due to IE
                        dropTarget.classList.add('ode-js-drop-available');
                    });
                });
                el.addEventListener('dragend', function (e: DragEvent) {
                    // Reset available drop zones
                    const nodes = document.querySelectorAll('.ode-js-drop-available[ode-drop-target]');
                    Array.prototype.forEach.call(nodes, dropTarget => { // Can't use NodeList.forEach due to IE
                        dropTarget.classList.remove('ode-js-drop-available');
                    });
                });
            };
            applyDragEvents(domElement);
        }
    };
});

interface OdeDropTargetScope {
    odeDropTarget?: string;
    odeOndrop?: ({$value: string}) => void;
}

export const odeDropTargetDirective = ng.directive('odeDropTarget', () => {
    return {
        restrict: 'A',
        scope: {
            odeDropTarget: '@',
            odeOndrop: '&'
        },
        link: (scope: OdeDropTargetScope, element /* JQuery */, attrs) => {
            const domElement = element.get(0);
            // Apply Drop events
            const applyDropEvents = (el: Element) => {
                // console.log("[odeDropTarget] setup drop-target for", el);
                el.addEventListener('dragover', function (e) {
                    if (el.classList.contains('ode-js-drop-available')) {
                        e.preventDefault(); // We authorize drop action
                        domElement.classList.add('ode-js-drop-hover');
                    }
                });
                el.addEventListener('dragenter', function (e) { // Needed for Android touch devices (polyfill need it)
                    if (el.classList.contains('ode-js-drop-available')) {
                        e.preventDefault(); // We authorize drop action
                    }
                });

                el.addEventListener('dragleave', function () {
                    domElement.classList.remove('ode-js-drop-hover');
                });

                el.addEventListener('drop', function (e: DragEvent) {

                    // console.log("[odeDropTarget] drop on", e.target);

                    // Step 1, extract the target and the data from dataTransfer
                    const data = e.dataTransfer.getData('text'); // IE forces to use `text` as mime type
                    const slashIndex = data.indexOf('/');
                    const expectedTarget = data.substr(0, slashIndex);
                    const effectiveData = data.substr(slashIndex + 1);
                    const validTargets = scope.odeDropTarget ? (
                        Array.isArray(scope.odeDropTarget) ? scope.odeDropTarget : [scope.odeDropTarget]
                    ) : [];
                    console.log("validTargets", validTargets);
                    if (validTargets === [] || expectedTarget === '<all>' || validTargets.includes(expectedTarget)) {
                        // Step 2 a : drop is authorized
                        e.preventDefault(); // We authorize drop action
                        // console.log("[odeDropTarget] drop in", element, effectiveData);
                        // console.log("[odeDropTarget] ondrop:", scope.odeOndrop);
                        scope.odeOndrop({$value: effectiveData});
                    } else {
                        // Step 2 b : drop isn't authorized
                        console.log("[odeDropTarget] not allowed drop");
                    }
                    domElement.classList.remove('ode-js-drop-hover');
                });
            }
            applyDropEvents(domElement);
        }
    };
});