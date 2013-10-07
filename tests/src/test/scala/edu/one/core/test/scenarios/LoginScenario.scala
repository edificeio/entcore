package edu.one.core.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._

object LoginScenario {

	val headers_1 = Map("""Accept""" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""")

	val headers_2 = Map("""Accept""" -> """text/css,*/*;q=0.1""")

	val headers_4 = Map("""Accept""" -> """image/png,image/*;q=0.8,*/*;q=0.5""")

	val headers_7 = Map(
		"""Accept""" -> """text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8""",
		"""Content-Type""" -> """application/x-www-form-urlencoded""")

	val headers_18 = Map("""X-Requested-With""" -> """XMLHttpRequest""")

	val headers_21 = Map("""If-Modified-Since""" -> """Fri, 04 Oct 2013 12:29:18 GMT""")

	val headers_27 = Map(
		"""Cache-Control""" -> """no-cache""",
		"""Content-Type""" -> """application/x-www-form-urlencoded; charset=UTF-8""",
		"""Pragma""" -> """no-cache""",
		"""X-Requested-With""" -> """XMLHttpRequest""")

	val scn = exec(http("Get admin page")
			.get("""/admin""")
			.headers(headers_1)
		.check(status.is(302)))
		.exec(http("Authenticate user")
			.post("""/auth/login""")
			.headers(headers_7)
			.param("""callBack""", """http%3A%2F%2Fone%2Fadmin""")
			.param("""email""", """tom.mate""")
			.param("""password""", """password""")
		.check(status.is(302)))
		.exec(http("Directory : get admin page")
			.get("""/directory/admin""")
			.headers(headers_1)
		.check(status.is(200)))
		.exec(http("Directory : list Schools")
			.get("""/directory/api/ecole""")
			.headers(headers_18)
		.check(status.is(200), jsonPath("status").is("ok"), jsonPath("$.result.0.id").find.saveAs("schoolId")))
		.exec(http("Directory : list classes")
			.get("""/directory/api/classes?id=${schoolId}""")
			.headers(headers_18)
		.check(status.is(200), jsonPath("status").is("ok"), jsonPath("$.result.0.classId").find.saveAs("classId")))
		.exec(http("Directory : list students in class")
			.get("""/directory/api/personnes?id=${classId}&type=ELEVE""")
			.headers(headers_18)
		.check(status.is(200), jsonPath("status").is("ok")))
		.exec(http("Directory : create manual user")
			.post("""/directory/api/user""")
			.headers(headers_27)
			.param("""classId""", """${classId}""")
			.param("""lastname""", """toto""")
			.param("""firstname""", """titi""")
			.param("""type""", """ENSEIGNANT""")
		.check(status.is(200), jsonPath("status").is("ok")))
		.exec(http("Auth : Logout user")
			.get("""/auth/logout""")
			.headers(headers_1)
      .check(status.is(302)))

}
