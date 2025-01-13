import http from "axios";
import { ng, idiom as lang, model, notify, skin } from 'entcore';
import { create } from 'sortablejs';
import { App } from "./myapps.types";
import { AppsService } from './myapps.service';

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
    themeAssetsPath: string;
    searchDisplayName: (app: App) => boolean;
    translatedDisplayName: (app: App) => string;
    toggleBookmarkEdition: () => void;
    addBookmark: (app: App, $index: number, $event: Event) => void;
    removeBookmark: (app: App, $index: number, $event: Event) => void;
    showConnectorSection: () => boolean;
    isIconUrl: (app: App) => boolean;
    getIconClass: (app: App) => string;
    getIconCode: (app: App) => string;
    $apply: () => any;
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
    
    $scope.isIconUrl = (app: App): boolean => {
        return app.icon && (app.icon.startsWith("/") || app.icon.startsWith("http://") || app.icon.startsWith("https://"));
    }

    $scope.getIconClass = (app:App):string => {
		const appCode = $scope.getIconCode(app);
		return `ic-app-${appCode} color-app-${appCode}`;
	}

    $scope.getIconCode = (app:App):string => {
        let appCode = app.icon.trim().toLowerCase() || "";
        if( appCode && appCode.length > 0 ) {
            if(appCode.endsWith("-large"))  appCode = appCode.replace("-large", "");
        } else {
            appCode = app.displayName.trim().toLowerCase();
        }
        appCode = lang.removeAccents(appCode);
		// @see distinct values for app's displayName is in query /auth/oauth2/userinfo
		switch( appCode ) {
			case "admin.title": 	    appCode = "admin"; break;
            case "banques des savoirs": appCode = "banquesavoir"; break;
            case "collaborativewall":   appCode = "collaborative-wall"; break;
            case "communautés":         appCode = "community"; break;
			case "directory.user":	    appCode = "userbook"; break;
            case "emploi du temps":     appCode = "edt"; break;
			case "messagerie": 		    appCode = "conversation"; break;
            case "news":                appCode = "actualites"; break;
            case "homeworks":
            case "cahier de texte":     appCode = "cahier-de-texte"; break;
            case "diary":
            case "cahier de texte 2d":  appCode = "cahier-textes"; break;
			default: break;
		}
		return appCode;
	}

    $scope.themeAssetsPath = skin.getBootstrapAssetsPath();

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
    
    $scope.addBookmark = async (app: App, $index: number, $event: Event): Promise<void> => {
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
        await http.put('/userbook/preference/apps', model.me.myApps);
        notify.info('apps.bookmarks.notify.add', 1000);
    }
    
    $scope.removeBookmark = async (app: App, $index: number, $event: Event): Promise<void> => {
        $event.stopImmediatePropagation();
        
        // remove app from bookmarks
        model.me.myApps.bookmarks.splice($index, 1);
        $scope.bookmarks.splice($index, 1);
        
        // add app to applications (or connectors) list
        if ($scope.display.showConnectorSection && app.isExternal) {
            model.me.myApps.applications.push(app.name);
            $scope.connectors.push(app);
        } else if ($scope.display.showConnectorSection && !app.isExternal) {
            model.me.myApps.applications.splice($scope.applications.length, 0, app.name);
            $scope.applications.push(app);
        } else {
            model.me.myApps.applications.push(app.name);
            $scope.applications.push(app);
        }
        
        const itemIndex = model.me.bookmarkedApps.findIndex(bmApp => bmApp.name == app.name);
        if (itemIndex !== -1) {
            model.me.bookmarkedApps.splice(itemIndex, 1);
        }
        
        await http.put('/userbook/preference/apps', model.me.myApps);
        notify.info('apps.bookmarks.notify.remove', 1000);
    }

    // Async checks and final rendering
    (async () => {
        try {
            const [connectorsThreshold, applicationList, is1DTheme] = await Promise.all([
                AppsService.getInstance().getConnectorsThresold(),
                AppsService.getInstance().getApplicationsList(),
                AppsService.getInstance().is1DTheme()
            ]);
            const {
                isApplication,
                isBookmark,
                isConnector,
                isConnectorBeforeLastAppInMyApps,
                orderAppsFirstInMyApps,
                pushToMyApps,
                sortApp,
                syncUserPrefAppsWith,
                } = AppsService.getInstance();
        
            if (applicationList) {
                const displayedApplications: Array<App> = applicationList.
                    filter(app => app.display).
                    sort((a, b) => lang.translate(a.displayName).toLowerCase() > lang.translate(b.displayName).toLowerCase()? 1: -1);
        
                await syncUserPrefAppsWith(displayedApplications);
        
                const connectors = displayedApplications.filter(app => app.isExternal);
                if (connectors && connectors.length > connectorsThreshold) {
                    $scope.display.showConnectorSection = true;
                }
                
                // model.me.myApps.applications will contain apps and connectors not bookmarked
                if ($scope.display.showConnectorSection) {
                    // first apps then connectors
                    displayedApplications
                        .filter(app => isApplication(app, is1DTheme))
                        .forEach(app => pushToMyApps(app));
                    displayedApplications
                        .filter(app => isConnector(app, is1DTheme))
                        .forEach(app => pushToMyApps(app));
                } else {
                    displayedApplications
                        .filter(app => !isBookmark(app, is1DTheme))
                        .forEach(app => pushToMyApps(app));
                }
                
                // if user is switching from no connector section to connector section,
                // and a connector is before an app in model.me.myApps.applications
                // then reorder myApps to have apps first and then connectors
                const applications = displayedApplications.filter(app => !app.isExternal);
                if ($scope.display.showConnectorSection && isConnectorBeforeLastAppInMyApps(applications, connectors)) {
                    orderAppsFirstInMyApps(applications);
                    await http.put('/userbook/preference/apps', model.me.myApps);
                }
        
                // lists for template
                $scope.bookmarks = displayedApplications
                    .filter(app => isBookmark(app, is1DTheme))
                    .sort((a, b) => model.me.myApps.bookmarks.indexOf(a.name) > model.me.myApps.bookmarks.indexOf(b.name)? 1: -1);
                    
                if ($scope.display.showConnectorSection) {
                    $scope.applications = displayedApplications
                        .filter(app => isApplication(app, is1DTheme))
                        .sort((a, b) => sortApp(a, b));
                    $scope.connectors = displayedApplications
                        .filter(app => isConnector(app, is1DTheme))
                        .sort((a, b) => sortApp(a, b));
                } else {
                    $scope.applications = displayedApplications
                        .filter(app => !isBookmark(app, is1DTheme))
                        .sort((a, b) => sortApp(a, b));
                }
            }
        } catch {
            $scope.bookmarks = [];
            $scope.applications = [];
            if ($scope.display.showConnectorSection)
                $scope.connectors = [];
        } finally {
            $scope.$apply();
        }
    
        // Elements for D&D list
        const bookmarkedAppsElement: Element = document.getElementById('bookmarked-apps');
        if (bookmarkedAppsElement) {
            create(bookmarkedAppsElement, {
                animation: 150,
                ghostClass: 'blue-background-class',
                delay: 150,
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
                delay: 150,
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
                delay: 150,
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

        const updateSort = async (name: string, oldIndex: number, newIndex: number, collection: string) => {
            model.me.myApps[collection].splice(oldIndex, 1);
            model.me.myApps[collection].splice(newIndex, 0, name);
            const app: App = $scope[collection].find((app: App) => app.name === name);
            $scope[collection].splice(oldIndex, 1);
            $scope[collection].splice(newIndex, 0, app);
            await http.put('/userbook/preference/apps', model.me.myApps);
            $scope.$apply();
        };
        
        const updateSortConnectorsSection = async (name: string, oldIndex: number, newIndex: number) => {
            model.me.myApps.applications.splice(oldIndex + $scope.applications.length, 1);
            model.me.myApps.applications.splice(newIndex + $scope.applications.length, 0, name);
            const app: App = $scope.connectors.find((app: App) => app.name === name);
            $scope.connectors.splice(oldIndex, 1);
            $scope.connectors.splice(newIndex, 0, app);
            await http.put('/userbook/preference/apps', model.me.myApps);
            $scope.$apply();
        };
    })();

}]);