# Tracer

## Tester

(Verticle de Test en cours)
1. Envoyer un log :
`vertx.eventBus().publish(config.getString("log-address"), new JsonObject().putString("appli", "<currentApplication>").putString("message", "<message>"));`
2. Vérifier que le nouveau log est bien ajouté dans le fichier <appli>.trace et dans le fichier all.trace, sous la forme :
`{"date":<date>, "appli":<appli>, "message":<message>}`

## Fonctionnement
- Verticle BuseModeBase qui implémente un Handler
- Initialise un Logger Java (JUL)
- Reçoit des messages des autres applications via l'EventBus
- Les messages sont en Json sous la forme : { appli:appli , message:message }
- Ils sont formattés sous la forme : { date:date, appli:appli, message:message }
- Ils sont ajoutés au fichier de log de leur appli : /data/dev/<appli>.trace et au fichier global /data/dev/all.trace
- Il y a donc un Handler par fichier, et un Handler global
- Chaque Handler a un ApplicationLogFilter associé, qui étend la classe Filter
- L'ApplicationLogFilter dispatche les messages en fonction du nom de l'application vers le fichier correspondant


# History

## Tester

0. Si pas de fichier de log dispo, en créer dans /data/dev/ de type <appli>.trace et all.trace, et les remplir avec des logs de type : 
`{"date":<date>, "appli":<appli>, "message":<message>}`
1. Tester sur [http://localhost:8006/history/admin]
2. Les logs doivent s'afficher en-dessous du sélecteur, avec dans l'ordre <DATE>,<APPLI> (en rouge),<MESSAGE-DE-LOG>

##Fonctionnement

- Verticle History qui étend Controller
- View admin.html qui affiche un sélecteur qui permet de choisir l'appli
- En fonction de l'option sélectionnée, une requête AJAX GET est faite sur http://localhost:8006/?app=<appli> et renvoie le contenu du fichier de log sous forme de JsonArray `[ {...} , {...}, {...} ]`
- la View affiche la réponse : un log par ligne, avec le nom de l'application en rouge
