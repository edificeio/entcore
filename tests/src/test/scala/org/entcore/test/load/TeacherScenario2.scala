package org.entcore.test.load

import Auth._
import Directory._
import Conversation._
import org.entcore.test.workspace.Workspace
import Workspace._
import io.gatling.core.Predef._
import bootstrap._
import scala.collection.mutable.ArrayBuffer

object TeacherScenario2 {

  val scn = scenario("Scenario enseignant")
    .exec(entAccess)
    .pause(2)
    .exec(activationForm)
    .pause(30)
    .exec(cgu)
    .pause(180)
    .feed(ssv("students.csv"))
    .exec{(session: Session) =>
      val code =  session("code").as[String]
      session.set("password", code+code)
    }
    .exec(activate("${login}", "${code}", "${password}"))
    .pause(2)
    .exec(login("${login}", "${password}"))
    .pause(10)
    .exec(myAccountAccess)
    .feed(citations)
    .exec{session =>
      session.set("motto", session("citation").as[String] + " - " + session("author").as[String])
    }
    .exec(updateMotto("${userId}", "${motto}"))
    .pause(15)
    .feed(users)
    .exec(updateDisplayName)
    .pause(15)
    .exec(updateEmail)
    .pause(1)
    .exec(apps)
    .pause(3)
    .exec(conversationAccess)
    .pause(3)
    .exec{session:Session =>
      session("inbox").asOption[ArrayBuffer[String]] match {
        case Some(l) =>
          if (l.size < 5) {
            session.set("nbReadable", l.size)
          } else {
            session.set("nbReadable", 5)
          }
        case _ => session.set("nbReadable", 0)
      }
    }
    .repeat("${nbReadable}", "countRead") {
      exec(readMessage("${inbox(countRead)}"))
      .pause(32)
    }
    .exec{session:Session =>
      session("visible").asOption[ArrayBuffer[String]].map[Session]{v =>
        if (v.size >= 2) {
          val to = "\"" + List(v.head, v.last).mkString("\",\"") + "\""
          session.set("to", to)
        } else {
          session
        }
      }.getOrElse[Session](session)
    }
    .repeat(3) {
      pause(90)
      .feed(emails)
      .exec(sendMessage("${to}", "${subject}", "${body}"))
    }
    .pause(5)
    .exec(deleteMessage("${outbox(0)}"))
    .exec(myClass)
    .pause(5)
    .exec(openCard("${classMembers(0)}"))
    .pause(10)
    .exec(apps)
    .pause(3)
    .exec(workspaceAccess)
    .pause(3)
    .exec(sharedDocuments)
    .pause(3)
    .exec(iconView)
    .pause(5)
    .exec{session:Session =>
      session("sharedDocuments").asOption[ArrayBuffer[String]] match {
        case Some(l) =>
          if (l.size < 3) {
            session.set("nbDownloadable", l.size)
          } else {
            session.set("nbDownloadable", 3)
          }
        case _ => session.set("nbDownloadable", 0)
      }
    }
    .repeat("${nbDownloadable}", "countDownload") {
      exec(downloadDocument("${sharedDocuments(countDownload)}"))
      .pause(5)
    }
    .feed(folders)
    .exec(createFolder("${folder}"))
    .pause(8)
    .exec(uploadDocument("RecordSimulation_request_202.txt"))
    .pause(5)
    .exec(shareDocumentToUserAndGroup("${uploadedDocumentId}"))
    .pause(5)
    .exec(logout)
}
