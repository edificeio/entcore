package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


import org.entcore.test.load.Headers._
import scala.collection.mutable.ArrayBuffer

object Conversation {

  val emails = csv("emails.csv").circular

  val conversationAccess =
    exec(http("Accès messagerie")
    .get("""/conversation/conversation""")
    .headers(headers_1))
    .pause(40 milliseconds)
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_63))
    .pause(6 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(6 milliseconds)
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(20 milliseconds)
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .pause(9 milliseconds)
    .exec(http("Affichage de la liste des messages reçus")
    .get("""/conversation/list/INBOX?page=0""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("inbox")))
    .pause(84 milliseconds)
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(21 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_77))
    .pause(24 milliseconds)
    .exec(http("Liste des groupes et des utilisateurs visibles")
    .get("""/conversation/visible""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("visible")))

  def readMessage(messageId: String) =
    exec(http("Ouvrir un message et le lire")
    .get("/conversation/message/" + messageId)
    .headers(headers_3))
    .pause(18 milliseconds)
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .pause(25 milliseconds)
    .exec(http("Affichage de la liste des messages reçus")
    .get("""/conversation/list/INBOX?page=0""")
    .headers(headers_3))

  def sendMessage(to: String, subject: String, body:String) = {
    exec(http("Envoyer un message")
    .post("""/conversation/send""")
    .headers(headers_42)
    .body(StringBody("""{"subject":"""" + subject + """","body":"""" + body +
      """","cc":[],"to":[""" + to + """]}""")))
    .pause(30 milliseconds)
    .exec(http("Affichage de la liste des messages envoyés")
    .get("""/conversation/list/OUTBOX?page=0""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("outbox")))
    .exec(http("Affichage de la liste des messages à la corbeille")
    .get("""/conversation/list/DRAFT?page=0""")
    .headers(headers_3))
  }

  def deleteMessage(id: String) =
    exec(http("Mettre un message à la corbeille")
    .put("/conversation/trash?id=" + id)
    .headers(headers_4))
    .pause(8 milliseconds)
    .exec(http("Affichage de la liste des messages à la corbeille")
    .get("""/conversation/list/TRASH?page=0""")
    .headers(headers_3))
    .exec(http("Supprimer un message")
    .delete("/conversation/delete?id=" + id)
    .headers(headers_3))

}
