/* Interface of the tracking service. */
export interface ITracker {
    trackEvent( category:string, action:string, name?:string, value?:number );
}
/* Tracking events for timeline (home page) */
export const TRACK = {
    event: "Page d'accueil NEO",
    nameForModule: function(name:string, variable?:string) {
        return (variable && variable.length>0) ? name.replace("[module]", variable) : name;
    },
    nameForSkin: function(name:string, variable?:string) {
        return (variable && variable.length>0) ? name.replace("[skin]", variable) : name;
    },
    nameForWidget: function(name:string, variable?:string) {
        return (variable && variable.length>0) ? name.replace("[widget]", variable) : name;
    },
    nameForLang: function(name:string, variable?:string) {
        return (variable && variable.length>0) ? name.replace("[Langue]", variable) : name;
    }
    ,DISCOVER: { action: "Découvrir l'ENT" 
        ,QUICKSTART_START: "Home - Quickstart - Clique"
        ,QUICKSTART_END: "Home - Quickstart - Terminé"
    }
    ,FILTER: { action: "Filtrer les notifications"
        ,SHOW: "Home - Filtrer sur (afficher)"
        ,HIDE: "Home - Filtrer sur (masquer)"
        ,SHOW_TYPE: "Home - Filtrer sur - [module] (afficher)"
        ,HIDE_TYPE: "Home - Filtrer sur - [module] (masquer)"
    }
    ,SETTINGS: { action: "Personnaliser la page d'accueil"
        ,OPEN: "Home - Personnaliser"
        ,SKIN_CHANGE: "Home - Personnaliser - Mode [skin]"
        ,WIDGET_SHOW: "Home - Personnaliser - Afficher widget [widget]"
        ,WIDGET_HIDE: "Home - Personnaliser - Masquer widget [widget]"
        ,CHANGE_LANG: "Home - Personnaliser - [Langue]"
        ,MOVE_WIDGET: "Home - Widget - Changer de place"
    }
    ,PROFILE: { action: "Accéder à mon compte"
        ,FROM_SCHOOL_WIDGET: "Home - Widget Profil - Mon profil"
        ,FROM_MENU_PROFILE: "Home - Bandeau - Mon compte"
    }
    ,USER: { action: "Accéder à une fiche utilisateur"
        ,OPEN_PROFILE: "Home - Notification - Profil utilisateur"
    }
    ,SEARCH: { action: "Lancer une recherche"
        ,GO: "Home - Bandeau - Recherche transverse"
    }
    ,NOTIF_SIGNAL: { action: "Signaler une notification"
        ,CLICK: "Home - Notification - Signaler"
    }
    ,NOTIF_DELETE: { action: "Supprimer une notification"
        ,CLICK: "Home - Notification - Supprimer du fil"
    }
    ,RECORD_SOUND: { action: "Enregistrer du son"
        ,START: "Home - Widget Dictaphone - Captation son"
    }
    ,OPEN_APP: { action: "Accéder à une appli"
        ,FROM_NEWS_LINK: "Home - Widget Actualité - Redirection"
        ,FROM_AGENDA_MORE: "Home - Widget Agenda - Vers module Agenda"
        ,FROM_AGENDA_EVENT: "Home - Widget Agenda - Redirection évènement"
        ,FROM_MYAPPS_WIDGET: "Home - Widget Mes applis - [module]"
        ,FROM_MYAPPS_WIDGET_MORE: "Home - Widget Mes applis - Plus"
        ,FROM_SCHOOL_MY_CLASSES: "Home - Widget Profil - Mes classes"
        ,FROM_SCHOOL_TEAM: "Home - Widget Profil - Equipe pédagogique"
        ,FROM_SCHOOL_DIRECTION: "Home - Widget Profil - Direction"
        ,FROM_NOTIF_LINK: "Home - Notification - Redirection"
        ,FROM_MENU_MYAPPS: "Home - Bandeau - Mes applis"
        ,FROM_MENU_MYAPPS_MORE: "Home - Bandeau - Mes applis - Plus"
        ,FROM_MENU_MYAPPS_APP: "Home - Bandeau - Mes applis - [module]"
        ,FROM_MENU_MAIL: "Home - Bandeau - Messagerie"
        ,FROM_MENU_COMMUNITY: "Home - Bandeau - Communautés"
    }
    ,HOME: { action: "Revenir à la page d'accueil"
        ,FROM_MENU_HOME: "Home - Bandeau - Accueil (maison)"
        ,FROM_LOGO: "Home - Bandeau - Accueil (logo)"
    }
    ,NAVIGATE: { action: "Redirection vers l'extérieur"
        ,FROM_BOOKMARK: "Home - Widget Signets - Redirection"
        ,FROM_QWANT: "Home - Widget Qwant - Mots clefs"
        ,FROM_RSS: "Home - Widget RSS - Redirection"
    }
    ,CARNET_DE_BORD: { action: "Accéder à la vie scolaire"
        ,NAVIGATE: "Home - Widget Carnet de bord - Navigation"
        ,REDIRECT: "Home - Widget Carnet de bord - Redirection"
    }
};
