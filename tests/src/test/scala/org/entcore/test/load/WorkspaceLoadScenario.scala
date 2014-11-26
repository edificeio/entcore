/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.test.load

import Auth._
import org.entcore.test.workspace.Workspace
import Workspace._
import io.gatling.core.Predef._

import scala.collection.mutable.ArrayBuffer

object WorkspaceLoadScenario {

  val scn = scenario("Workspace Scenario")
    .feed(ssv("users.csv"))
    .exec{(session: Session) =>
      val code =  session("code").as[String]
      session.set("password", code+code)
    }
    .exec(activate("${login}", "${code}", "${password}"))
    .exec(login("${login}", "${password}"))
    .exec(workspaceAccess)
    .pause(1)
    .exec(sharedDocuments)
    .pause(1)
    .exec(iconView)
    .pause(1)
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
      .pause(1)
    }
    .exec(uploadDocument("RecordSimulation_request_202.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_3.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_7.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_8.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_9.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_10.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_11.txt"))
    .exec(uploadDocument("WorkspaceSimulation_request_12.txt"))
    .pause(1)
    .exec(sendToRack("RecordSimulation_request_220.txt", "${availableRack(0)}"))
    .pause(1)
    .exec(logout)

}
