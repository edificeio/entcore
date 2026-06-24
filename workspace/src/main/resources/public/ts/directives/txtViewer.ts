import { ng } from 'entcore';

export interface TxtFile {
    id: string;
    content: Promise<string>;
}
export interface TxtController {
    setContent(csv: TxtFile): void;
}
export interface TxtDelegate {
    onInit(ctrl: TxtController): void;
}
interface TxtViewerScope {
    controller: TxtController;
    txtDelegate: TxtDelegate;
    loading: boolean;
    error: boolean;
    content: string;
    $apply: any
}
export const txtViewer = ng.directive('txtViewer', ['$sce', ($sce) => {
    return {
        restrict: 'E',
        scope: {
            txtDelegate: '='
        },
        template: `
            <div class="render">
			    <p ng-if="loading" class="top-spacing-four flex-row align-start justify-center centered-text"><i18n>workspace.preview.loading</i18n>&nbsp;<i class="loading"></i></p>
			    <p ng-if="error" class="top-spacing-four flex-row align-start justify-center centered-text"><i18n>workspace.preview.unavailable</i18n></p>
                <pre ng-if="!loading && !error">[[content]]</pre>
            </div>
        `,
        link: function (scope: TxtViewerScope, element, attributes, ctrl) {
            let _lastId = null;
            scope.loading = true;

            scope.controller = {
                async setContent(txt) {
                    if (_lastId == txt.id) return;
                    try {
                        scope.loading = true;
                        scope.error = false;
                        _lastId = txt.id;
                        scope.content = await txt.content;
                    } catch (e) {
                        scope.error = true;
                    } finally {
                        scope.loading = false;
                        // Plain scope.$apply() throws "$apply already in progress" if this resolves
                        // mid-digest, which (uncaught here) would leave the view stuck on "loading".
                        const phase = (scope as any).$root.$$phase;
                        if (phase !== "$apply" && phase !== "$digest") {
                            scope.$apply();
                        }
                    }
                }
            }
            scope.txtDelegate.onInit(scope.controller);
        }
    }
}]);