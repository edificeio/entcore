import { ng, template, idiom as lang } from 'entcore';
import { appController } from './myapps.controller';

const mainController = ng.controller('MainController', ['$scope', ($scope) => {
    template.open('main', 'applications');
    $scope.template = template;
    $scope.translatedDisplayName = function(app){
        return lang.translate(app.displayName);
    }

    $scope.getIconClass = (app):string => {
		const appCode = $scope.getIconCode(app);
		return `ic-app-${appCode} color-app-${appCode}`;
	}

    $scope.getIconCode = (app):string => {
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
}]);

ng.controllers.push(mainController);
ng.controllers.push(appController);
