import { ng, idiom as lang, model, http, notify } from 'entcore';
import { create } from 'sortablejs';

export interface App {
    name: string;
    address: string;
    icon: string;
    target: string;
    displayName: string;
    display: boolean;
    prefix: string;
    casType: string;
    scope: Array<string>;
    isExternal: boolean
}

export interface AppControllerScope {
    lang: typeof lang;
    display: {
        showBookmarkSection: boolean, 
        showConnectorSection: boolean, 
        searchText: string,
        modeBookmarkEdition: boolean,
        modeBookmarkEditionButtonLabel: string
    };
    bookmarks: Array<App>;
    applications: Array<App>;
    connectors: Array<App>;
    searchDisplayName: (app: App) => boolean;
    translatedDisplayName: (app: App) => string;
    toggleBookmarkEdition: () => void;
    addBookmark: (app: App, $index: number, $event: Event) => void;
    removeBookmark: (app: App, $index: number, $event: Event) => void;
    showConnectorSection: () => boolean;
    isIconUrl: (app: App) => boolean;
    $apply: () => any;
}

export interface ApplicationListResponse {
    apps: Array<App>;
}

export const appController = ng.controller('ApplicationController', ['$scope', ($scope: AppControllerScope) => {
    $scope.lang = lang;
    
    $scope.display = {
        showBookmarkSection: !!document.getElementById('bookmarked-apps'),
        showConnectorSection: false,
        searchText: null,
        modeBookmarkEdition: false,
        modeBookmarkEditionButtonLabel: 'apps.bookmarks.button.edit.start'
    };
    
    http().get('/conf/public').done(conf => {
        let threshold = conf["my-apps-connectors-threshold"];
		if (threshold == undefined) {
            threshold = 6;
        }
        
        http().get('/applications-list').done((app: ApplicationListResponse) => {
            if (app.apps) {
                let displayedApplications: Array<App> = app.apps
                    .filter(app => app.display)
                    .sort((a, b) => lang.translate(a.displayName).toLowerCase() > lang.translate(b.displayName).toLowerCase()? 1: -1);
                
                const connectors = displayedApplications.filter(app => app.isExternal);
                if (connectors && connectors.length > threshold) {
                    $scope.display.showConnectorSection = true;
                }
                
                // model.me.myApps.applications will contain apps and connectors not bookmarked
                if ($scope.display.showConnectorSection) {
                    // first apps then connectors
                    displayedApplications
                        .filter(app => isApplication(app))
                        .forEach(app => pushToMyApps(app));
                    displayedApplications
                        .filter(app => isConnector(app))
                        .forEach(app => pushToMyApps(app));
                } else {
                    displayedApplications
                        .filter(app => !isBookmark(app))
                        .forEach(app => pushToMyApps(app));
                }
                
                // if case user is switching from no connector section to connector section,
                // and a connector is before an app in model.me.myApps.applications
                // then reorder myApps to have apps first and then connectors
                const applications = displayedApplications.filter(app => !app.isExternal);
                if ($scope.display.showConnectorSection && isConnectorBeforeLastAppInMyApps(applications, connectors)) {
                    orderAppsFirstInMyApps(applications);
                    http().putJson('/userbook/preference/apps', model.me.myApps);
                }
                
                // lists for template
                $scope.bookmarks = displayedApplications
                    .filter(app => isBookmark(app))
                    .sort((a, b) => model.me.myApps.bookmarks.indexOf(a.name) > model.me.myApps.bookmarks.indexOf(b.name)? 1: -1);
                    
                if ($scope.display.showConnectorSection) {
                    $scope.applications = displayedApplications
                        .filter(app => isApplication(app))
                        .sort((a, b) => sortApp(a, b));
                    $scope.connectors = displayedApplications
                        .filter(app => isConnector(app))
                        .sort((a, b) => sortApp(a, b));
                } else {
                    $scope.applications = displayedApplications
                        .filter(app => !isBookmark(app))
                        .sort((a, b) => sortApp(a, b));
                }
                
                $scope.$apply();
            }
        });
    });

    // Elements for D&D list
    const bookmarkedAppsElement: Element = document.getElementById('bookmarked-apps');
    if (bookmarkedAppsElement) {
        create(bookmarkedAppsElement, {
            animation: 150,
            ghostClass: 'blue-background-class',
            delay: 200,
            delayOnTouchOnly: true,
            // Moving within bookmarks
            onUpdate: function (evt) {
                updateSort(evt.item.id, evt.oldIndex, evt.newIndex, "bookmarks");
            }
        });
    }

    const appsElement: Element = document.getElementById('apps');
    if (appsElement) {
        create(appsElement, {
            animation: 150,
            ghostClass: 'blue-background-class',
            delay: 200,
            delayOnTouchOnly: true,
            // Moving within applications
            onUpdate: function (evt) {
                updateSort(evt.item.id, evt.oldIndex, evt.newIndex, "applications");
            }
        });
    }

    const connectorsElement: Element = document.getElementById('connectors');
    if (connectorsElement) {
        create(connectorsElement, {
            animation: 150,
            ghostClass: 'blue-background-class',
            delay: 200,
            delayOnTouchOnly: true,
            // Moving within connectors
            onUpdate: function (evt) {
                if ($scope.display.showConnectorSection) {
                    updateSortConnectorsSection(evt.item.id, evt.oldIndex, evt.newIndex);
                } else {
                    updateSort(evt.item.id, evt.oldIndex, evt.newIndex, "applications");
                }
            }
        });
    }

    const updateSort = (name: string, oldIndex: number, newIndex: number, collection: string) => {
        model.me.myApps[collection].splice(oldIndex, 1);
        model.me.myApps[collection].splice(newIndex, 0, name);
        const app: App = $scope[collection].find((app: App) => app.name === name);
        $scope[collection].splice(oldIndex, 1);
        $scope[collection].splice(newIndex, 0, app);
        http()
            .putJson('/userbook/preference/apps', model.me.myApps)
            .done(() => $scope.$apply());
    };
    
    const updateSortConnectorsSection = (name: string, oldIndex: number, newIndex: number) => {
        model.me.myApps.applications.splice(oldIndex + $scope.applications.length, 1);
        model.me.myApps.applications.splice(newIndex + $scope.applications.length, 0, name);
        const app: App = $scope.connectors.find((app: App) => app.name === name);
        $scope.connectors.splice(oldIndex, 1);
        $scope.connectors.splice(newIndex, 0, app);
        http()
            .putJson('/userbook/preference/apps', model.me.myApps)
            .done(() => $scope.$apply());
    };

    $scope.searchDisplayName = (item: App): boolean => {
        return !$scope.display.searchText ||
                lang.removeAccents(lang.translate(item.displayName)).toLowerCase().indexOf(
                    lang.removeAccents($scope.display.searchText).toLowerCase()
            ) !== -1;
    };

    $scope.translatedDisplayName = (app: App): string => {
        return lang.translate(app.displayName);
    }
    
    $scope.toggleBookmarkEdition = (): void => {
        $scope.display.modeBookmarkEdition = !$scope.display.modeBookmarkEdition;
        if ($scope.display.modeBookmarkEdition) {
            $scope.display.modeBookmarkEditionButtonLabel = 'apps.bookmarks.button.edit.end';
        } else {
            $scope.display.modeBookmarkEditionButtonLabel = 'apps.bookmarks.button.edit.start';
        }
    }
    
    $scope.addBookmark = (app: App, $index: number, $event: Event): void => {
        $event.stopImmediatePropagation();
        
        // add app to bookmarks
        model.me.myApps.bookmarks.push(app.name);
        $scope.bookmarks.push(app);
        
        // remove app from applications (or connectors) list
        if ($scope.display.showConnectorSection && app.isExternal) {
            model.me.myApps.applications.splice($scope.applications.length + $index, 1);
            $scope.connectors.splice($index, 1);
        } else {
            model.me.myApps.applications.splice($index, 1);
            $scope.applications.splice($index, 1);
        }
        
        const correspondingApp = model.me.apps.find(a => a.name === app.name);
        if (correspondingApp && !model.me.bookmarkedApps.find(bmApp => bmApp.name === app.name)) {
            model.me.bookmarkedApps.push(correspondingApp);
        }
        http()
            .putJson('/userbook/preference/apps', model.me.myApps)
            .done(() => notify.info('apps.bookmarks.notify.add', 1000));
    }
    
    $scope.removeBookmark = (app: App, $index: number, $event: Event): void => {
        $event.stopImmediatePropagation();
        
        // remove app from bookmarks
        model.me.myApps.bookmarks.splice($index, 1);
        $scope.bookmarks.splice($index, 1);
        
        // add app to applications (or connectors) list
        if ($scope.display.showConnectorSection && app.isExternal) {
            model.me.myApps.applications.push(app.name);
            $scope.connectors.push(app);
        } else if ($scope.display.showConnectorSection && !app.isExternal) {
            model.me.myApps.applications.splice($scope.applications.length + 1, 0, app.name);
            $scope.applications.push(app);
        } else {
            model.me.myApps.applications.push(app.name);
            $scope.applications.push(app);
        }
        
        const itemIndex = model.me.bookmarkedApps.findIndex(bmApp => bmApp.name == app.name);
        if (itemIndex !== -1) {
            model.me.bookmarkedApps.splice(itemIndex, 1);
        }
        http()
            .putJson('/userbook/preference/apps', model.me.myApps)
            .done(() => notify.info('apps.bookmarks.notify.remove', 1000));
    }
    
    $scope.showConnectorSection = (): boolean => {
        return $scope.display.showConnectorSection && $scope.connectors && $scope.connectors.length > 0;
    }
    
    $scope.isIconUrl = (app: App): boolean => app.icon && app.icon.startsWith("/");
    
    // Utils functions
    const isBookmark = (app: App): boolean => model.me.bookmarkedApps.find(bookmarkedApp => bookmarkedApp.name === app.name);
    const isApplication = (app: App): boolean => !isBookmark(app) && !app.isExternal;
    const isConnector = (app: App): boolean => !isBookmark(app) && app.isExternal;
    const sortApp = (a: App, b: App) => model.me.myApps.applications.indexOf(a.name) > model.me.myApps.applications.indexOf(b.name)? 1: -1;
    const pushToMyApps = (app: App) => {
        if (model.me.myApps.applications.indexOf(app.name) == -1) {
            model.me.myApps.applications.push(app.name);
        }
    }
    
    const isConnectorBeforeLastAppInMyApps = (apps: Array<App>, connectors: Array<App>) => {
        let res = false;
        let lastAppIndex = 0;
        model.me.myApps.applications.forEach((appName: string, index) => {
            if (apps.find(app => appName === app.name)) {
                lastAppIndex = index;
            }
        });
        model.me.myApps.applications.forEach((appName: String, index) => {
            if (connectors.find(conn => appName === conn.name) && index < lastAppIndex) {
                res = true;
            }
        });
        return res;
    }
    
    const orderAppsFirstInMyApps = (apps: Array<App>) => {
        const orderedApplications = [];
        const orderedConnectors = [];
        model.me.myApps.applications.forEach((myAppsAppName: string) => {
            if (apps.find(app => myAppsAppName === app.name)) {
                orderedApplications.push(myAppsAppName);
            } else {
                orderedConnectors.push(myAppsAppName);
            }
        });
        model.me.myApps.applications = [...orderedApplications, ...orderedConnectors];   
    }
}]);