package org.entcore.directory.listeners;

import io.edifice.directory.v1.GetUsersByIdsRequest;
import io.edifice.directory.v1.GetUsersByIdsRequestBuilder;
import io.edifice.directory.v1.User;
import io.edifice.directory.v1.UserField;
import io.edifice.events.vertx.BrokerResponseCodes;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.directory.services.ClassService;
import org.entcore.directory.services.GroupService;
import org.entcore.directory.services.SchoolService;
import org.entcore.directory.services.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DirectoryBrokerListenerImplTest {

    private UserService userService;
    private DirectoryBrokerListenerImpl listener;

    @Before
    public void setUp() {
        userService = Mockito.mock(UserService.class);
        listener = new DirectoryBrokerListenerImpl(Mockito.mock(Vertx.class), Mockito.mock(GroupService.class), userService,
            Mockito.mock(SchoolService.class), Mockito.mock(ClassService.class));
        // What the query returns when every optional field is asked for
        final JsonObject user = new JsonObject()
            .put("id", "user-1")
            .put("displayName", "Jane DOE")
            .put("profiles", new JsonArray().add("Teacher"))
            .put("functions", new JsonArray().add(new JsonArray().add("ADMIN_LOCAL").add(new JsonArray().add("structure-1"))))
            .put("firstName", "Jane")
            .put("lastName", "DOE")
            .put("login", "jane.doe")
            .put("email", "jane.doe@example.com");
        Mockito.when(userService.getUsersByIds(Mockito.any(), Mockito.any())).thenReturn(Future.succeededFuture(new JsonArray().add(user)));
    }

    @SuppressWarnings("unchecked")
    private Set<UserField> askedFields() {
        final ArgumentCaptor<Set<UserField>> fields = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(userService).getUsersByIds(Mockito.eq(new JsonArray().add("user-1")), fields.capture());
        return fields.getValue();
    }

    @Test
    public void getUsersByIdsWithoutFieldsReturnsTheBaseFields() {
        final GetUsersByIdsRequest request = new GetUsersByIdsRequestBuilder().withUserIds(Collections.singletonList("user-1")).build();
        final User user = listener.onGetUsersByIds(request).result().getUsers().get(0);
        assertEquals(EnumSet.noneOf(UserField.class), askedFields());
        assertEquals("user-1", user.getId());
        assertEquals("Jane DOE", user.getDisplayName());
        assertEquals("Teacher", user.getProfile());
        assertEquals(Collections.singletonList("structure-1"), user.getFunctions().get("ADMIN_LOCAL"));
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getLogin());
        assertNull(user.getEmail());
    }

    @Test
    public void getUsersByIdsReturnsOnlyTheFieldsAskedFor() {
        // null is a value of the enum that this version does not know
        final GetUsersByIdsRequest request = new GetUsersByIdsRequestBuilder()
            .withUserIds(Collections.singletonList("user-1"))
            .withFields(Arrays.asList(UserField.email, null, UserField.login))
            .build();
        final User user = listener.onGetUsersByIds(request).result().getUsers().get(0);
        assertEquals(EnumSet.of(UserField.email, UserField.login), askedFields());
        assertEquals("jane.doe@example.com", user.getEmail());
        assertEquals("jane.doe", user.getLogin());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }

    @Test
    public void getUsersByIdsWithoutIdsIsInvalid() {
        final Future<?> response = listener.onGetUsersByIds(new GetUsersByIdsRequestBuilder().build());
        assertTrue(response.failed());
        assertTrue(response.cause() instanceof IllegalArgumentException);
        assertEquals(BrokerResponseCodes.INVALID_PARAMETERS, response.cause().getMessage());
        Mockito.verify(userService, Mockito.never()).getUsersByIds(Mockito.any(), Mockito.any());
    }
}
