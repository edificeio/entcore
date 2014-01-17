package org.entcore.test.scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import bootstrap._
import java.net.URLEncoder
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64

object AuthScenario {

	val scn = exec(http("Logout admin user")
      .get("""/auth/logout""")
    .check(status.is(302)))
    .exec(http("Get activation page")
			.get("""/auth/activation""")
		.check(status.is(200)))
		.exec(http("Activate student account")
			.post("""/auth/activation""")
			.param("""login""", """${studentLogin}""")
			.param("""activationCode""", """${studentCode}""")
			.param("""password""", """blipblop""")
      .param("""confirmPassword""", """blipblop""")
		.check(status.is(302)))
    .exec(http("Activate teacher account")
      .post("""/auth/activation""")
      .param("""login""", """${teacherLogin}""")
      .param("""activationCode""", """${teacherCode}""")
      .param("""password""", """blipblop""")
      .param("""confirmPassword""", """blipblop""")
    .check(status.is(302)))
    .exec(http("Get OAuth2 code with disconnected user")
      .get("/auth/oauth2/auth?response_type=code&state=blip&scope=userinfo&client_id=test" +
        AppRegistryScenario.now + "&redirect_uri=http%3A%2F%2Flocalhost%3A1500%2Fcode")
      .check(status.is(200)))
    .exec(http("User login for OAuth2")
      .post("""/auth/login""")
      .param("""email""", """${teacherLogin}""")
      .param("""password""", """blipblop""")
      .param("""callBack""", URLEncoder.encode("http://localhost:8080" +
        "/auth/oauth2/auth?response_type=code&state=blip&scope=userinfo&client_id=test" +
        AppRegistryScenario.now + "&redirect_uri=http%3A%2F%2Flocalhost%3A1500%2Fcode", "UTF-8"))
      .check(status.is(302)))
    .exec(http("Get OAuth2 code with connected user")
      .get("/auth/oauth2/auth?response_type=code&state=blip&scope=userinfo&client_id=test" +
        AppRegistryScenario.now + "&redirect_uri=http%3A%2F%2Flocalhost%3A1500%2Fcode")
      .check(status.is(302), header("Location").find.transform(_.map(location =>
          location.substring(location.indexOf("code=") + 5).substring(0, 36)
      )).saveAs("oauth2Code")))
    .exec(http("Logout teacher user")
      .get("""/auth/logout""")
      .check(status.is(302)))
    .exec(http("Get OAuth2 token")
      .post("""/auth/oauth2/token""")
      .header("Authorization", "Basic " + Base64.encode(("test" + AppRegistryScenario.now +
          ":clientSecret").getBytes("UTF-8")))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Accept", "application/json; charset=UTF-8")
      .param("""grant_type""", """authorization_code""")
      .param("""code""", """${oauth2Code}""")
      .param("""redirect_uri""", """http%3A%2F%2Flocalhost%3A1500%2Fcode""")
      .check(status.is(200), jsonPath("$.token_type").is("Bearer"),
        jsonPath("$.access_token").find.saveAs("oauth2AccessToken")))
    .exec(http("Get userinfo with access token")
      .get("/auth/oauth2/userinfo")
      .header("Authorization", "Bearer ${oauth2AccessToken}")
      .check(status.is(200), jsonPath("$.login").is("${teacherLogin}")))

}
