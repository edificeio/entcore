package org.entcore.directory.services.impl.filter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotVisibleFilterPersonTest {

    private JsonArray sampleUserInfos;

    @Before
    public void setUp() {
        JsonObject sampleUserInfo = new JsonObject()
                .put("id", "123")
                .put("displayName", "John Doe")
                .put("type", "user")
                .put("email", "john.doe@example.com")
                .put("phone", "+1234567890")
                .put("address", "123 Main St")
                .put("ssn", "123-45-6789");

        sampleUserInfos = new JsonArray().add(sampleUserInfo);
    }

    @Test
    public void testApplyFilter_WhenFilterIsEnabled_ShouldOnlyKeepAuthorizedFields() {
        // Given
        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(sampleUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        assertEquals(1, result.size());
        JsonObject filteredUser = result.getJsonObject(0);

        // Authorized fields should remain unchanged
        assertEquals("123", filteredUser.getString("id"));
        assertEquals("John Doe", filteredUser.getString("displayName"));
        assertEquals("user", filteredUser.getString("type"));

        // Unauthorized fields should be null
        assertNull(filteredUser.getString("email"));
        assertNull(filteredUser.getString("phone"));
        assertNull(filteredUser.getString("address"));
        assertNull(filteredUser.getString("ssn"));

        // Verify that unauthorized fields are present but null
        assertTrue(filteredUser.containsKey("email"));
        assertTrue(filteredUser.containsKey("phone"));
        assertTrue(filteredUser.containsKey("address"));
        assertTrue(filteredUser.containsKey("ssn"));
    }

    @Test
    public void testApplyFilter_WhenFilterIsDisabled_ShouldReturnOriginalData() {
        // Given
        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(sampleUserInfos, false);

        // When
        JsonArray result = filter.apply();

        // Then
        assertEquals(1, result.size());
        JsonObject filteredUser = result.getJsonObject(0);

        // All fields should remain unchanged
        assertEquals("123", filteredUser.getString("id"));
        assertEquals("John Doe", filteredUser.getString("displayName"));
        assertEquals("user", filteredUser.getString("type"));
        assertEquals("john.doe@example.com", filteredUser.getString("email"));
        assertEquals("+1234567890", filteredUser.getString("phone"));
        assertEquals("123 Main St", filteredUser.getString("address"));
        assertEquals("123-45-6789", filteredUser.getString("ssn"));
    }

    @Test
    public void testApplyFilter_WithOnlyAuthorizedFields_ShouldNotChangeAnything() {
        // Given
        JsonObject authorizedOnlyUser = new JsonObject()
                .put("id", "456")
                .put("displayName", "Jane Doe")
                .put("type", "admin");
        JsonArray authorizedOnlyUserInfos = new JsonArray().add(authorizedOnlyUser);

        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(authorizedOnlyUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        assertEquals(1, result.size());
        JsonObject filteredUser = result.getJsonObject(0);

        assertEquals("456", filteredUser.getString("id"));
        assertEquals("Jane Doe", filteredUser.getString("displayName"));
        assertEquals("admin", filteredUser.getString("type"));
        assertEquals(3, filteredUser.getMap().size());
    }

    @Test
    public void testApplyFilter_WithEmptyUserInfo_ShouldReturnEmptyObject() {
        // Given
        JsonObject emptyUser = new JsonObject();
        JsonArray emptyUserInfos = new JsonArray().add(emptyUser);

        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(emptyUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        assertEquals(1, result.size());
        JsonObject filteredUser = result.getJsonObject(0);
        assertTrue(filteredUser.isEmpty());
    }

    @Test
    public void testApplyFilter_WithNullValues_ShouldHandleGracefully() {
        // Given
        JsonObject userWithNulls = new JsonObject()
                .put("id", "789")
                .put("displayName", (String) null)
                .put("type", "user")
                .put("email", (String) null)
                .put("phone", "+1234567890");
        JsonArray userInfos = new JsonArray().add(userWithNulls);

        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(userInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        assertEquals(1, result.size());
        JsonObject filteredUser = result.getJsonObject(0);

        assertEquals("789", filteredUser.getString("id"));
        assertNull(filteredUser.getString("displayName"));
        assertEquals("user", filteredUser.getString("type"));
        assertNull(filteredUser.getString("email"));
        assertNull(filteredUser.getString("phone"));
    }

    @Test
    public void testApplyFilter_ShouldNotModifyOriginalObject() {
        // Given
        JsonObject originalUser = new JsonObject()
                .put("id", "999")
                .put("displayName", "Test User")
                .put("email", "test@example.com");
        JsonArray originalUserInfos = new JsonArray().add(originalUser);

        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(originalUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        // Original object should remain unchanged
        assertEquals("test@example.com", originalUser.getString("email"));

        // But the result should have the email nullified
        JsonObject filteredUser = result.getJsonObject(0);
        assertNull(filteredUser.getString("email"));

        // Verify the structure is preserved (JsonArray with one element)
        assertEquals(1, result.size());
        assertTrue(result.getJsonObject(0) instanceof JsonObject);
    }

    @Test
    public void testApplyFilter_PreservesJsonArrayStructure() {
        // Given - Test spécifique pour vérifier la préservation de structure Neo4j
        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(sampleUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        assertNotNull(result);
        assertTrue(result instanceof JsonArray);
        assertEquals(1, result.size());
        assertTrue(result.getValue(0) instanceof JsonObject);
    }

    @Test
    public void testApplyFilter_ContractConsistency_FieldsAreNullified() {
        // Given - Test pour vérifier que les clés restent présentes (contrat API)
        NotVisibleFilterPerson filter = new NotVisibleFilterPerson(sampleUserInfos, true);

        // When
        JsonArray result = filter.apply();

        // Then
        JsonObject filteredUser = result.getJsonObject(0);

        // Vérifier que les champs non autorisés sont bien présents mais null
        assertTrue("email field should be present", filteredUser.containsKey("email"));
        assertTrue("phone field should be present", filteredUser.containsKey("phone"));
        assertTrue("address field should be present", filteredUser.containsKey("address"));
        assertTrue("ssn field should be present", filteredUser.containsKey("ssn"));

        // Vérifier que les valeurs sont null
        assertNull("email should be null", filteredUser.getValue("email"));
        assertNull("phone should be null", filteredUser.getValue("phone"));
        assertNull("address should be null", filteredUser.getValue("address"));
        assertNull("ssn should be null", filteredUser.getValue("ssn"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_WithNullJsonArray_ShouldThrowException() {
        // Given
        JsonArray nullArray = null;

        // When & Then
        new NotVisibleFilterPerson(nullArray, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_WithEmptyJsonArray_ShouldThrowException() {
        // Given
        JsonArray emptyArray = new JsonArray();

        // When & Then
        new NotVisibleFilterPerson(emptyArray, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_WithNonJsonObjectInArray_ShouldThrowException() {
        // Given
        JsonArray arrayWithString = new JsonArray().add("not a json object");

        // When & Then
        new NotVisibleFilterPerson(arrayWithString, true);
    }
}