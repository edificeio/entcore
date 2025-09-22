package org.entcore.common.http.filter;

import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.security.ActionType;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.regex.Pattern;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.when;

public class AbstractActionFilterTest  extends AbstractActionFilter{

    private JsonObject session;
    private HttpServerRequest request;
    private Boolean result = null;


    @Before
    public void setup() {
        session = new JsonObject();
        result = null;
        request = Mockito.mock(HttpServerRequest.class);
    }

    public AbstractActionFilterTest() {
        super(new HashSet<>(), Mockito.mock(ResourcesProvider.class));
    }

    @Test
    public void userIsAuthorized_workflowActionInSession() {
        Handler<Boolean> handler = (e) -> result = e;
        //GIVEN
        session.put("authorizedActions", JsonArray.of(new JsonObject("{ \"name\" : \"info.read\" }")));
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.path()).thenReturn("/my-uri");
        bindings.add(new Binding(fr.wseduc.webutils.http.HttpMethod.GET, Pattern.compile("/my-uri"), "info.read", ActionType.WORKFLOW));

        //WHEN
        this.userIsAuthorized(request, session, handler);

        //THEN
        MatcherAssert.assertThat("Info.read should be authorized", result, equalTo(true) );
    }

    @Test
    public void userIsAuthorized_workflowActionNotInSession() {
        Handler<Boolean> handler = (e) -> result = e;
        //GIVEN
        session.put("authorizedActions", JsonArray.of(new JsonObject("{ \"name\" : \"info.read\" }")));
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.path()).thenReturn("/my-uri");
        bindings.add(new Binding(fr.wseduc.webutils.http.HttpMethod.GET, Pattern.compile("/my-uri"), "info.write", ActionType.WORKFLOW));

        //WHEN
        this.userIsAuthorized(request, session, handler);

        //THEN
        MatcherAssert.assertThat("Info.write should not be authorized", result, equalTo(false) );
    }

    @Test
    public void userIsAuthorized_workflowAction_right_inSession() {
        Handler<Boolean> handler = (e) -> result = e;
        //GIVEN
        session.put("authorizedActions", JsonArray.of(new JsonObject("{ \"name\" : \"info.read\" }")));
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.path()).thenReturn("/my-uri");
        Binding binding = new Binding(fr.wseduc.webutils.http.HttpMethod.GET, Pattern.compile("/my-uri"), "info.write", ActionType.WORKFLOW);
        binding.setRight("info.read");

        bindings.add(binding);

        //WHEN
        this.userIsAuthorized(request, session, handler);

        //THEN
        MatcherAssert.assertThat("Info.read should be authorized by the right value", result, equalTo(true) );
    }



    @Override
    public void canAccess(HttpServerRequest request, Handler<Boolean> handler) {

    }

    @Override
    public void deny(HttpServerRequest request) {

    }
}
