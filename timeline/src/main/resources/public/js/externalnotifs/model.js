//dissocier filtres et notifsxt
//recup tte infos de la tl
function Preference(){
}

// propriete preference
// contient seulement les notifsxt
function Config(){
}
//sous categorie des applis
function AppAction(){
}

//applis
function Appli(data){
    //place les objets dans untableau
    this.collection(AppAction)
    //recupere le AppAction de data pour l'inserer dans la collection
    this.appActions.load(data.appActions)
    //data.appActions contient le _.map et est mis dans la collec via load

}

//recup pref et met à jour
Preference.prototype.getinfo = function(){
    //recup en json preferences
    http().get('/userbook/preference/timeline').done(function(data){
        this.content = data;
    }.bind(this))
}

// appeler à la fin
// cree l'objet de base defini la structure front

model.build = function(){
	this.makeModels([Preference, Config, Appli, AppAction]);
    //je cree un objet Preference dans this.preference
    this.preference = new Preference();
    //j'affiche son contenu
    this.preference.getinfo();
    //recupère la liste des applis
    this.collection(Appli, {
        sync: function(){
            http().get('/timeline/registeredNotifications').done(function(data){

                //on recupe la liste des TYPES d'action et les groupes par appli > (objet qui contient tableaux)
                //map permet d'attribuer les noms en tableaux (tableau qui contient les objets)

                data=_.map(_.groupBy(data, 'type'), function(item){
                    return{
                        appActions: item,
                        appName: item[0]['app-name'],
                        type: item[0]['type'],
                        appAddress: item[0]['app-address']
                    }
                })

                this.load(data);

            }.bind(this))
        }
    })
};
