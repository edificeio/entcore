package org.entcore.test.workspace

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._


import scala.collection.mutable.ArrayBuffer
import scala.Some

import org.entcore.test.load.Headers._

object Workspace {

  lazy val folders = csv("folders.csv").random

  val workspaceAccess =
    exec(http("Accès à l'espace doc")
    .get("""/workspace/workspace""")
    .headers(headers_1))
    .pause(62 milliseconds)
    .exec(http("Détermination de la locale")
    .get("""/locale""")
    .headers(headers_1))
    .pause(15 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(47 milliseconds)
    .exec(http("Détermination du chemin vers le thème")
    .get("""/theme""")
    .headers(headers_3))
    .pause(66 milliseconds)
    .exec(http("Infos de la session de l'utilisateur")
    .get("""/auth/oauth2/userinfo""")
    .headers(headers_3))
    .pause(11 milliseconds)
    .exec(http("Liste les dossiers de l'utilisateur")
    .get("""/workspace/folders/list?filter=owner""")
    .headers(headers_3))
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))
    .pause(107 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .exec(http("Affichage du nombre de messages non lus")
    .get("""/conversation/count/INBOX?unread=true""")
    .headers(headers_3))
    .pause(14 milliseconds)
    .exec(http("Infos de l'utilisateur")
    .get("""/userbook/api/person""")
    .headers(headers_3))
    .pause(97 milliseconds)
    .exec(http("Récupération de l'avatar")
    .get("""/userbook/document/no-avatar.jpg?userbook-dimg=public/img/no-avatar.jpg&v=&thumbnail=48x48""")
    .headers(headers_2))
    .pause(90 milliseconds)
    .exec(http("Liste les casiers utilisateur visibles")
    .get("""/workspace/users/available-rack""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("availableRack")))

  val sharedDocuments =
    exec(http("Liste les documents partagés avec l'utilisateur")
    .get("""/workspace/documents?filter=shared""")
    .headers(headers_3)
    .check(status.is(200), jsonPath("$.._id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("sharedDocuments")))

  def downloadDocument(documentId: String) =
    exec(http("Ouverture d'un document")
    .get("/workspace/document/" + documentId)
    .headers(headers_1))

  val iconView =
    foreach("${sharedDocuments}", "doc") {
      exec(http("Affichage de la vignette")
        .get("""/workspace/document/${doc}?thumbnail=120x120""")
        .headers(headers_2))
    }

  def createFolder(name: String) =
    exec(http("Création d'un répertoire")
    .post("""/workspace/folder""")
    .headers(headers_96)
    .formParam("""name""", name))
    .pause(4 milliseconds)
    .exec(http("Liste les dossiers de l'utilisateur")
    .get("""/workspace/folders/list?filter=owner""")
    .headers(headers_3))

  def uploadDocument(document: String) =
    exec(http("Chargement d'un document depuis le bureau")
    .post("""/workspace/document?thumbnail=120x120""")
    .headers(headers_202)
    .body(RawFileBody(document))
    .check(status.is(201), jsonPath("$._id").find.saveAs("uploadedDocumentId")))
    .pause(20 milliseconds)
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))

  def sendToRack(document: String, userId: String) =
    exec(http("Envoi d'un document dans le casier")
    .post("/workspace/rack/" + userId + "?thumbnail=120x120")
    .headers(headers_220)
    .body(RawFileBody(document)))
    .pause(51 milliseconds)
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))

  def shareDocumentToUserAndGroup(documentId: String) =
    exec(http("Récupération des partages d'un document")
    .get("""/workspace/share/json/""" + documentId)
    .headers(headers_3)
    .check(status.is(200), jsonPath("$..id").findAll
      .transformOption(_.orElse(Some(ArrayBuffer.empty[String]))).saveAs("toShare")))
    .exec{session:Session =>
      session("toShare").asOption[ArrayBuffer[String]].map[Session]{v =>
        if (v.size >= 2) {
          session.set("shareGroupId", v.last).set("shareUserId", v.head)
        } else {
          session
        }
      }.getOrElse[Session](session)
    }
    .exec(http("Partage d'un document à un groupe")
    .put("""/workspace/share/json/""" + documentId)
    .bodyPart(StringBodyPart("""groupId""", "${shareGroupId}"))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|commentDocument"""))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|copyDocuments"""))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|getDocument""")))
    .pause(58 milliseconds)
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))
    .pause(19 milliseconds)
    .exec(http("Partage d'un document à un utilisateur")
    .put("""/workspace/share/json/""" + documentId)
    .bodyPart(StringBodyPart("""userId""", "${shareUserId}"))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|commentDocument"""))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|copyDocuments"""))
    .bodyPart(StringBodyPart("""actions""", """org-entcore-workspace-service-WorkspaceService|getDocument""")))
    .pause(65 milliseconds)
    .exec(http("Liste les documents de l'utilisateur")
    .get("""/workspace/documents?filter=owner&hierarchical=true""")
    .headers(headers_3))

}
