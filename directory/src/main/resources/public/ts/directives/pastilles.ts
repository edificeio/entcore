import { ng, _, angular } from 'entcore';

/**
 * @description Display pastilles that can be used as tabs.
 * @param images An array of string containing the list of images paths.
 * @param ngModel The activated pastille index.
 * @example
 *  <pastilles 
        ng-model="<index>"
        images="<images>">
    </pastilles>
 */

export const pastilles = ng.directive('pastilles', ['$window', ($window) => {
    return {
        restrict: 'E',
        template: `
            <div class="spacer-medium invisible-content">
                <a class="absolute-w round square-large img-shadow high-index inactive" ng-repeat="image in images track by $index">
                    <img skin-src="[[ image ]]"/>
                </a>
            </div>
        `,

        scope: {
            ngModel: '=',
            images: '=',
        },

        link: (scope, element, attributes) => {

            // Waiting for pastilles to be created
            setTimeout(function () {
                var i, l;
                var pastilles = element.find("div").children();
                var totalWidth, pastilleWidth, leftOffset, offset, pastilleOffset, count, nbPastilles = pastilles.length;

                // Update z index
                var updateZIndex = function() {
                    pastilles.eq(nbPastilles - 1 - scope.ngModel).css("z-index", "" + (1000 + nbPastilles));
                    for (i = scope.ngModel - 1, count = 0; i >= 0; i--) {
                        pastilles.eq(nbPastilles - 1 - i).css("z-index", "" + (1000 + nbPastilles - count - 1));
                        count++;
                    }
                    for (i = scope.ngModel + 1, count = 0; i < nbPastilles; i++) {
                        pastilles.eq(nbPastilles - 1 - i).css("z-index", "" + (1000 + nbPastilles - count - 1));
                        count++;
                    }
                }

                // Update pastille position (also if resizing)
                var updatePastillesPosition = function() {
                    totalWidth = element.find('div').width();
                    pastilleWidth = pastilles[0].offsetWidth;
                    offset = pastilleWidth * 3 / 5;
                    leftOffset = (totalWidth - (pastilleWidth + ((nbPastilles - 1) * offset))) / 2;

                    for (i = nbPastilles - 1; i >= 0; i--) {
                        pastilles[nbPastilles - 1 - i].originalLeft = leftOffset + (i * offset);
                        pastilles.eq(nbPastilles - 1 - i).css("left", pastilles[nbPastilles - 1 - i].originalLeft + "px");
                    }
                    updateZIndex();
                }

                scope.$watch(function() { return element.find('div').css('width'); }, function(newValue) {
                    updatePastillesPosition();
                });

                angular.element($window).bind('resize', function() {
                    // Avoid weird left/top animation when resizing
                    for (i = 0; i < nbPastilles; i++)
                        pastilles.eq(i).removeClass("animated");
                    updatePastillesPosition();

                    setTimeout(function () {
                        for (i = 0; i < nbPastilles; i++)
                            pastilles.eq(i).addClass("animated");
                    }, 250);
                });
                updatePastillesPosition();

                // Centering when hovering (padding+absolute position)
                element.find('.round').on('mouseenter', function() {
                    if (this.classList.contains("inactive"))
                        scope.setActive(this);
                });

                element.find('.round').on('mouseleave', function() {
                    if (this.classList.contains("inactive")) {
                        scope.setInactive(this);
                    }
                });

                // Set active on click
                element.find('.round').on('click', function() {
                    element.find(".active").addClass("inactive");
                    element.find(".active").removeClass("active");
                    for (i = 0; i < nbPastilles; i++)
                        scope.setInactive(pastilles[i]);
                    this.classList.remove("inactive");
                    this.classList.add("active");
                    scope.setActive(this);
                    scope.ngModel = nbPastilles - Array.prototype.slice.call(element.find("div")[0].children).indexOf(this) - 1;
                    updateZIndex();
                    scope.$apply();
                });

                scope.setActive = (e) => {
                    e.style.left = this.originalLeft - 3 + "px";
                    e.style.top = "-3px";
                    e.style.marginTop = "-3px";
                };

                scope.setInactive = (e) => {
                    e.style.left = this.originalLeft + "px";
                    e.style.top = "0px";
                    e.style.marginTop = "0px";
                };

                // Activate the first pastille
                scope.setActive(pastilles[nbPastilles - 1]);
                pastilles.eq(nbPastilles - 1).removeClass("inactive");
                pastilles.eq(nbPastilles - 1).addClass("active");

                // Animated class added after fix positioned
                setTimeout(function () {
                    for (i = 0; i < nbPastilles; i++)
                        pastilles.eq(i).addClass("animated");
                }, 250);

                element.find("div").removeClass("invisible-content");
            }, 250);
        }
    };
}]);