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
interface Tab {
    id: number;
    name: string;
    rows: any[][];
    colIndexes: number[];
    separator: string;
}
interface CsvViewerScope {
    csvContent: CsvDelegate;
    controller: CsvController;
    csvDelegate: CsvDelegate;
    loading: boolean;
    tabs: Array<Tab>;
    currentTab: Tab;
    pageIndex: number;
    showTabs(): boolean;
    showContent(): boolean;
    nameTab(): string;
    numPages(): number;
    previousPage(e): void;
    nextPage(e): void;
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
            <div ng-if="showTabs()" class="pagination__area flex-row align-center justify-center">
                <div class="file-controls left"><i class="left" ng-click="previousPage(e)"></i></div>
                <div class="pagination">
                [[nameTab()]] <input type="text" ng-model="pageIndex" /> / [[numPages()]]
                </div>
                <div class="file-controls right"><i class="right" ng-click="nextPage(e)"></i></div>
            </div>
            <div class="render">
			    <p ng-if="loading" class="top-spacing-four flex-row align-start justify-center centered-text"><i18n>workspace.preview.loading</i18n>&nbsp;<i class="loading"></i></p>
                <table ng-if="showContent()">
                    <tbody>
                        <tr ng-repeat="row in currentTab.rows">
                            <td ng-repeat="colIndex in currentTab.colIndexes">[[getValue(row, colIndex)]]</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `,
        link: function (scope: CsvViewerScope, element, attributes, ctrl) {
            let _ngmodel = "";
            let _lastId = null;
            scope.tabs = [];
            scope.loading = true;
            const separator = [",", ";", "|"];
            const countOcurrences = (str: string, value: string) => {
                if (value == "|") value = "\\|";
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
                        scope.tabs = [];
                        scope.currentTab = null;
                        return;
                    }
                    let currentTab: Tab = null;
                    const createTab = (name?: string) => {
                        const tab = {
                            id: scope.tabs.length,
                            name: name ? name : `Feuille ${scope.tabs.length + 1}`,
                            colIndexes: [],
                            rows: [],
                            separator: null
                        } as Tab;
                        scope.tabs.push(tab);
                        return tab;
                    }
                    for (const row of rows) {
                        if (row.startsWith("\f")) {
                            currentTab = createTab(row.trim());
                        } else {
                            if (currentTab == null) currentTab = createTab();
                            if (currentTab.separator == null) {
                                currentTab.separator = findSeparator(rows[0]);
                            }
                            const cols = row.split(currentTab.separator);
                            const countCol = cols.length;
                            currentTab.rows.push(cols);
                            if (countCol > currentTab.colIndexes.length) {
                                currentTab.colIndexes = Array.from(Array(countCol).keys());
                            }
                        }
                    }
                    scope.currentTab = scope.tabs[0];
                }
            }
            Object.defineProperty(scope, "pageIndex", {
                get() {
                    return scope.currentTab ? scope.currentTab.id + 1 : 1
                },
                set(index) {
                    const id = index - 1;
                    if (id < 0) {
                        scope.currentTab = scope.tabs[0]
                    } else if (id >= scope.tabs.length) {
                        scope.currentTab = scope.tabs[scope.tabs.length - 1];
                    } else {
                        scope.currentTab = scope.tabs[id];
                    }
                }
            })
            scope.showContent = () => !scope.loading && !!scope.currentTab;
            scope.showTabs = () => !scope.loading && scope.tabs.length > 1;
            scope.nameTab = () => {
                return scope.currentTab && scope.currentTab.name;
            }
            scope.numPages = () => {
                return scope.tabs.length;
            }
            scope.previousPage = (e) => {
                e && e.stopPropagation();
                if (scope.currentTab) {
                    if (scope.currentTab.id > 0) {
                        scope.currentTab = scope.tabs[scope.currentTab.id - 1];
                    } else {
                        //do nothing
                    }
                } else {
                    scope.currentTab = scope.tabs[0];
                }
            }
            scope.nextPage = (e) => {
                e && e.stopPropagation();
                if (scope.currentTab) {
                    if (scope.currentTab.id < scope.tabs.length - 1) {
                        scope.currentTab = scope.tabs[scope.currentTab.id + 1];
                    } else {
                        //do nothing
                    }
                } else {
                    scope.currentTab = scope.tabs[0];
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