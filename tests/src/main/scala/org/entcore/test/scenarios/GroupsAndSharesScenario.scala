/*
 * Copyright © WebServices pour l'Éducation, 2018
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

package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._


object GroupsAndSharesScenario {

  val scn =
    exec(http("Login teacher")
      .post( """/auth/login""")
      .formParam( """email""", """julie.devost""")
      .formParam( """password""", """blipblop""")
      .check(status.is(302)))
    .exec(http("Get search criteria")
      .get("/userbook/search/criteria"))
    .exec(http("Search users and groups")
      .post("/communication/visible")
      .body(StringBody("""{"profiles" : ["Teacher", "Personnel"]}"""))
      .check(jsonPath("$.groups[*].id").findAll.saveAs("shareGroupIds"), jsonPath("$.users[*].id").findAll.saveAs("shareUserIds")))
    .exec{ session =>
      val shareUserIds = session("shareUserIds").as[Seq[String]].mkString("\",\"")
      val shareAllIds = session("shareGroupIds").as[Seq[String]].mkString("\",\"") + "\",\"" + shareUserIds
      session.set("shareUserIds", shareUserIds).set("shareAllIds", shareAllIds)
    }
    .exec(http("Create share bookmark")
      .post("/directory/sharebookmark")
      .body(StringBody("""{"name" : "Mon favoris de partage", "members": ["${shareUserIds}"]}"""))
      .check(status.is(201), jsonPath("$.id").find.saveAs("shareBookmarkId")))
    .exec(http("Update share bookmark")
      .put("/directory/sharebookmark/${shareBookmarkId}")
      .body(StringBody("""{"name" : "Mon favoris de partage renommé", "members": ["${shareAllIds}"]}"""))
      .check(status.is(200)))
    .exec(http("Get share bookmark")
      .get("/directory/sharebookmark/${shareBookmarkId}")
      .check(status.is(200)))
    .exec(http("List share bookmark")
      .get("/directory/sharebookmark/all")
      .check(status.is(200), jsonPath("$..id").findAll.transform(_.size).saveAs("sbCount")))
    .exec(http("Delete share bookmark")
      .delete("/directory/sharebookmark/${shareBookmarkId}")
      .check(status.is(200)))
    .exec(http("List share bookmark")
      .get("/directory/sharebookmark/all")
      .check(status.is(200), jsonPath("$..id").findAll.transform(l => (l.size + 1).toString).is("${sbCount}")))

}