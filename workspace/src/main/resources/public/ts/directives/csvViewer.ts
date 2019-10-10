import { ng } from 'entcore';

export interface CsvFile {
    id: string;
    content: Promise<string>;
}
export interface CsvController {
    setContent(csv: CsvFile): void;
}
export interface CsvDelegate {
    onInit(ctrl: CsvController): void;
}
interface CsvViewerScope {
    csvContent: CsvDelegate;
    rows: any[][];
    controller: CsvController;
    csvDelegate: CsvDelegate;
    colIndexes: number[];
    loading: boolean;
    getValue(row: any[], index: number): string;
    $apply: any
}
export const csvViewer = ng.directive('csvViewer', ['$sce', ($sce) => {
    return {
        restrict: 'E',
        scope: {
            csvDelegate: '='
        },
        template: `
            <div class="render">
			    <p ng-if="loading" class="top-spacing-four flex-row align-start justify-center centered-text"><i18n>workspace.preview.loading</i18n>&nbsp;<i class="loading"></i></p>
                <table>
                    <tbody>
                        <tr ng-repeat="row in rows">
                            <td ng-repeat="colIndex in colIndexes">[[getValue(row, colIndex)]]</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `,
        link: function (scope: CsvViewerScope, element, attributes, ctrl) {
            let _ngmodel = "";
            let _lastId = null;
            scope.rows = [];
            scope.loading = true;
            scope.colIndexes = [];
            const separator = [",", ";", "|"];
            const countOcurrences = (str: string, value: string) => {
                if(value=="|") value = "\\|";
                const regExp = new RegExp(value, "gi");
                return (str.match(regExp) || []).length;
            }
            const findSeparator = (row: string) => {
                let count = 0;
                let sep = separator[0];
                for (let cSep of separator) {
                    const current = countOcurrences(row, cSep);
                    if (count < current) {
                        count = current;
                        sep = cSep;
                    }
                }
                return sep;
            }
            const recompute = () => {
                if (_ngmodel) {
                    const rows = _ngmodel.split("\n");
                    if (rows.length == 0) {
                        scope.colIndexes = [];
                        return;
                    }
                    let sep = findSeparator(rows[0]);
                    scope.rows = [];
                    let countCol = 0;
                    for (let row of rows) {
                        const cols = row.split(sep);
                        countCol = Math.max(countCol, cols.length);
                        scope.rows.push(cols);
                    }
                    scope.colIndexes = Array.from(Array(countCol).keys())
                }
            }
            scope.getValue = (row, index) => {
                return `${row[index] || ""}`;
            }
            scope.controller = {
                async setContent(csv) {
                    if (_lastId == csv.id) return;
                    scope.loading = true;
                    _lastId = csv.id;
                    _ngmodel = await csv.content;
                    recompute();
                    scope.loading = false;
                    scope.$apply();
                }
            }
            scope.csvDelegate.onInit(scope.controller);
        }
    }
}]);