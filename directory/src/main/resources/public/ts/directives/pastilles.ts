import { ng, _ } from 'entcore';

/**
 * @description Display pastilles that can be used as tabs.
 * @param ngModel ...
 * @example
 * example
 */
export const pastilles = ng.directive('pastilles', () => {
    return {
        restrict: 'E',
        template: `
            <div class="spacer-medium">
                <a class="absolute-w round square-large img-shadow high-index inactive">
                    <img skin-src="/img/illustrations/group-avatar.svg"/>
                </a>
                <a class="absolute-w round square-large img-shadow high-index inactive">
                    <img skin-src="/img/illustrations/group-avatar.svg"/>
                </a>
                <a class="absolute-w round square-large img-shadow high-index active">
                    <img skin-src="/img/illustrations/group-avatar.svg"/>
                </a>
            </div>
        `,

        scope: {
            ngModel: '='
        },

        link: (scope, element, attributes) => {
            var pastilles = element.find(".spacer-medium").children();
            var i, nbPastilles = pastilles.length;
            var totalWidth, pastilleWidth, leftOffset, offset, pastilleOffset;

            totalWidth = element.width();
            pastilleWidth = pastilles[0].offsetWidth;
            offset = pastilleWidth * 3 / 4;
            leftOffset = (totalWidth - (pastilleWidth + ((nbPastilles - 1) * offset))) / 2;

            for (i = nbPastilles - 1; i >= 0; i--) {
                pastilles[nbPastilles - 1 - i].originalLeft = leftOffset + (i * offset);
                pastilles.eq(nbPastilles - 1 - i).css("left", pastilles[nbPastilles - 1 - i].originalLeft + "px");
            }

            // Centering when hovering (padding+absolute position)
            element.find('.round:not(.active)').on('mouseenter', function() {
                scope.setActive(this);
            });

            element.find('.round:not(.active)').on('mouseleave', function() {
                this.style.left = this.originalLeft + "px";
                this.style.top = "0px";
                this.style.marginTop = "0px";
            });

            scope.setActive = (e) => {
                e.style.left = this.originalLeft - 3 + "px";
                e.style.top = "-3px";
                e.style.marginTop = "-3px";
            };

            // Activate the first pastille
            scope.setActive(pastilles[nbPastilles - 1]);
            
            setTimeout(function () {
                for (i = 0; i < nbPastilles; i++) {
                    pastilles.eq(i).addClass("animated");
                }
            }, 0);
        }
    };
});