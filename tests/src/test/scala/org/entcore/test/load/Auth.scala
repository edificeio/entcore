package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


import org.entcore.test.load.Headers._

object Auth {

  val entAccess =
    exec(http("Accès à l'ENT")
    .get("""/""")
    .headers(headers_1))
    .pause(10 milliseconds)
    .exec(http("Détermination du thème")
    .get("""/skin""")
    .headers(headers_3))

  val activationForm =
    exec(http("Ouverture du formulaire d'activation")
    .get("""/auth/activation""")
    .headers(headers_1))
    .pause(10 milliseconds)
    .exec(http("Détermination du thème")
    .get("""/skin""")
    .headers(headers_3))

  val cgu =
    exec(http("Ouverture et lecture de la charte")
    .get("""/auth/cgu""")
    .headers(headers_1))

  def activate(login: String, code: String, password: String, acceptCgu: String = "true") =
    exec(http("Activation du compte")
    .post("""/auth/activation""")
    .headers(headers_9)
    .formParam("login", login)
    .formParam("activationCode", code)
    .formParam("password", password)
    .formParam("confirmPassword", password)
    .formParam("acceptCGU", acceptCgu))
    .pause(10 milliseconds)
    .exec(http("Détermination du thème")
    .get("/skin")
    .headers(headers_3))

  def login(login: String, password: String) =
    exec(http("Connexion")
    .post("""/auth/login""")
    .headers(headers_17)
    .formParam("""callBack""", """""")
    .formParam("""email""", login)
    .formParam("password", password))
    .exitHereIfFailed
    .pause(71 milliseconds)
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_3))
    .pause(34 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(71 milliseconds)
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(18 milliseconds)
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(42 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48"""))

  val logout =
    exec(http("Déconnexion")
      .get("""/auth/logout"""))
}
