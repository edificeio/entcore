package org.entcore.test.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


import org.entcore.test.load.Headers._
import scala.collection.mutable.ArrayBuffer
import net.minidev.json.{JSONObject, JSONValue}

object Directory {

  val moodFeeder = Array(Map("mood" -> "happy"), Map("mood" -> "proud"), Map("mood" -> "dreamy"),
    Map("mood" -> "love"), Map("mood" -> "tired"), Map("mood" -> "angry"),
    Map("mood" -> "worried"), Map("mood" -> "sick"), Map("mood" -> "joker"), Map("mood" -> "sad"))

  val citations = ssv("citations.csv").circular

  val users = csv("users.csv").circular

  val myAccountAccess =
    exec(http("Accès à mon compte")
    .get("""/userbook/mon-compte""")
    .headers(headers_1))
    .pause(37 milliseconds)
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_1))
    .pause(129 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("userId").find.saveAs("userId")))
    .pause(70 milliseconds)
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(57 milliseconds)
    .exec(http("Infos userbook")
    .get("""/directory/userbook/${userId}""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$").find.saveAs("userbookJson")))
    .pause(14 milliseconds)
    .exec(http("Infos user")
    .get("""/directory/user/${userId}""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$").find.transformOption(_.map{ j =>
      val json = JSONValue.parse(j).asInstanceOf[JSONObject]
      json.remove("sector")
      json.remove("classes")
      json.remove("checksum")
      json.remove("type")
      json.remove("password")
      json.remove("externalId")
      json.remove("structures")
      json.remove("id")
      json.remove("level")
      json.remove("login")
      json
    }).saveAs("userJson")))
    .pause(113 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .pause(42 milliseconds)
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .pause(25 milliseconds)
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(162 milliseconds)
    .exec(http("Recherche tous les utilisateurs visibles")
    .get("""/userbook/api/search?name=""")
    .headers(headers_3))
    .pause(117 milliseconds)
    .exec(http("Récupération de l'avatar 290")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=0&thumbnail=290x290""")
    .headers(headers_2))
    .pause(20 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .pause(66 milliseconds)

  def updateMotto(userId: String, motto: String) =
    exec(http("Changement devise")
      .put("/directory/userbook/" + userId)
      .headers(headers_42)
      .body(StringBody("""{"motto":"""" + motto + """"}""")))

  def updateMood(userId: String, mood: String) =
    exec(http("Changement d'humeur")
      .put("/directory/userbook/" + userId)
      .headers(headers_42)
      .body(StringBody("""{"mood":"""" + mood + """"}""")))

  val apps =
    exec(http("Accès à mes applis")
    .get("""/apps""")
    .headers(headers_1))
    .pause(34 milliseconds)
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_1))
    .pause(122 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(38 milliseconds)
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(21 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(45 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))

  val myClass =
    exec(http("Accès à la classe")
    .get("""/userbook/annuaire?myClass=""")
    .headers(headers_1))
    .pause(55 milliseconds)
    .exec(http("Liste des membres de la classe")
    .get("""/userbook/api/class""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("classMembers")))
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_1))
    .pause(38 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(5 milliseconds)
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(2 milliseconds)
    .exec(http("Recherche tous les utilisateurs visibles")
    .get("""/userbook/api/search?name=""")
    .headers(headers_3))
    .pause(64 milliseconds)
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(33 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .pause(58 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .foreach("${classMembers}", "member") {
      exec(http("Récupération de l'avatar 290")
        .get("/userbook/avatar/${member}?thumbnail=290x290")
        .headers(headers_2))
    }

  def openCard(userId: String) =
    exec(http("Ouverture fiche d'un utilisateur")
    .get("/userbook/api/person?id=" + userId)
    .headers(headers_3))
    .pause(30 milliseconds)
    .foreach("${classMembers}", "member") {
      exec(http("Récupération de l'avatar 100")
        .get("/userbook/avatar/${member}?thumbnail=100x100")
        .headers(headers_2))
    }

  val updateDisplayName =
    exec(http("Mise à jour des infos d'userbook")
    .put("""/directory/userbook/${userId}""")
    .headers(headers_42)
    .body(StringBody("${userbookJson}")))
    .pause(21 milliseconds)
    .exec{session:Session =>
      session("userJson").asOption[JSONObject].map{json =>
        json.put("displayName", session("username").as[String])
        session.set("userJson", json).set("userJsonString", json.toJSONString)
      }.getOrElse[Session](session)
    }
    .exec(http("Modification du nom d'affichage")
    .put("""/directory/user/${userId}""")
    .headers(headers_42)
    .body(StringBody("${userJsonString}")))

  val updateEmail =
    exec(http("Mise à jour des infos d'userbook")
    .put("""/directory/userbook/${userId}""")
    .headers(headers_42)
    .body(StringBody("${userbookJson}")))
    .pause(21 milliseconds)
    .exec{session:Session =>
      session("userJson").asOption[JSONObject].map{json =>
        json.put("email", session("email").as[String])
        session.set("userJson", json).set("userJsonString", json.toJSONString)
      }.getOrElse[Session](session)
    }
    .exec(http("Saisie adresse mail")
    .put("""/directory/user/${userId}""")
    .headers(headers_42)
    .body(StringBody("${userJsonString}")))

}
