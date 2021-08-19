import { Subject } from "rxjs";
import { ClassRoom, Network, User, PersonApiResult, School, UserTypes } from "../model";

/* Interface of the tracker service from infra-front */
export interface ITracker {
    trackEvent( category:string, action:string, name?:string, value?:number );
}
export const TRACK = {
    event: "Paramétrage de la classe",
    name: function(name:string, profil?:UserTypes) {
        if( profil ) {
            switch( profil ) {
                case "Student":     name = name.replace("[profil]", "élèves"); break;
                case "Relative":    name = name.replace("[profil]", "parents"); break;
                case "Teacher":     name = name.replace("[profil]", "enseignants"); break;
                case "Personnel":   name = name.replace("[profil]", "personnels"); break;
                case "Guest":
                default: break;
            }
        }
        return name;
    },
    PUBLIPOSTAGE: { action: "Publipostage", 
         BATCH_DETAIL: "[profil]_LotsFichesConnexionDétailléesPDF"
        ,BATCH_SIMPLE: "[profil]_LotsFichesConnexionSimplifiéesPDF"
        ,BATCH_MAIL: "[profil]_LotsFichesConnexionDétailléesMAIL"
        ,BATCH_CSV: "[profil]_LotsFichesConnexionExportCSV"
        ,DETAILED_PDF_ONE: "[profil]_IndividuelleFicheConnexionDétailléePDF"
        ,CODE_RENEW: "CodeRenouveléFicheConnexionDétailléePDF"
        ,TAILORED_DETAIL: "SurmesureFicheConnexionDétailléesPDF"
        ,TAILORED_SIMPLE: "SurmesureFicheConnexionSimplifiéesPDF"
        ,TAILORED_MAIL_PRINT: "SurmesureFicheConnexionDétailléesMAIL_Impression"
        ,TAILORED_MAIL_SEND: "SurmesureFicheConnexionDétailléesMAIL_Envoi"
    }
    ,ACCOUNT_CREATION: { action: "Créer un compte", 
         CREATE: "[profil]AjoutUtilisateur_CréerCompte"
        ,ADD: "[profil]AjoutUtilisateur_CréerAjouterAutreCompte"
    }
    ,USERS_IMPORT: { action: "Importer des utilisateurs", 
         IMPORT: "AjoutUtilisateur_ImporterFichier_Importer"
        ,ERROR: "AjoutUtilisateur_ImporterFichier_ImportCodeErreur"
    }
    ,ClASS_ATTACHMENT: { action: "Rattachement à une classe", 
         ADD_USER: "AjoutUtilisateur_RechercherUtilisateurs_AjouterUtilisateur_AjouterAMaClasse"
        ,ADD_ALL: "AjoutUtilisateur_RechercherUtilisateurs_ToutAjouter_AjouterAMaClasse"
        ,ADD: "AjoutUtilisateur_RechercherUtilisateurs_AjouterAMaClasse"
    }
    ,AUTH_MODIFICATION: { action: "Modifier l'authentification", 
         ID_USER: "[profil]_Ficheutilisateur_ModifierIdentifiant"
        ,PWD_USER: "[profil]_Ficheutilisateur_ReinitialisationMdp"
        ,PWD: "[profil]_RéinitialiserMDP"
        ,CODE: "GénérerCodeRenouvellement"
    }
    ,USER_BLOCK: { action: "Restreindre un utilisateur", 
         BLOCK: "Bloquer (toaster)"
        ,SUPPRESS_SUPPRESS: "[profil]_Supprimer_Supprimer"
        ,SUPPRESS_REMOVE_CLASS: "[profil]_Supprimer_RetirerClasse"
        ,REMOVE_CLASS: "RetirerClasse"
    }
    ,CSV_EXPORT: { action: "Export CSV", 
        CONNECTION: "FicheConnexionExportCSV"
    }
};

export interface EventDelegateScope {
    //Class events
    queryClassRefresh: Subject<ClassRoom>;
    onClassLoaded: Subject<ClassRoom>;
    onClassRefreshed: Subject<ClassRoom>;
    //Network events
    onSchoolLoaded: Subject<School[]>;
    //User events 
    onSelectionChanged: Subject<User[]>;
    onUserCreated: Subject<User[]>;
    onUserUpdate: Subject<User>;
    safeApply(fn?);
    //Event tracker
    tracker:ITracker;
}
export function EventDelegate($scope: EventDelegateScope, tracker:ITracker) {
    $scope.queryClassRefresh = new Subject();
    $scope.onClassLoaded = new Subject();
    $scope.onClassRefreshed = new Subject();
    $scope.onSchoolLoaded = new Subject();
    $scope.onSelectionChanged = new Subject();
    $scope.onUserCreated = new Subject();
    $scope.onUserUpdate = new Subject;
    $scope.safeApply = function (fn) {
        const phase = this.$root.$$phase;
        if (phase == '$apply' || phase == '$digest') {
            if (fn && (typeof (fn) === 'function')) {
                fn();
            }
        } else {
            this.$apply(fn);
        }
    };
    $scope.tracker = tracker;
}