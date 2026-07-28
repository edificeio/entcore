package org.entcore.directory.services.impl;

import static org.entcore.directory.services.impl.DefaultUserService.extractPreferredStructureId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

/**
 * Unit tests for the parsing of the preferred structure out of the user's {@code widgets} preference.
 * <p>
 * That preference is a JSON blob written by clients and is not formally typed, so the parsing must never
 * throw: any unexpected shape has to degrade to an empty result so that a single odd user cannot break a
 * whole batch. These tests pin that contract, including on shapes that are not expected today but that
 * nothing prevents from appearing.
 */
public class DefaultUserServicePreferredStructureTest {

    private static final String STRUCTURE_ID = "9b1c7d3e-5f4a-4b2c-9d8e-7a6b5c4d3e2f";

    @Test
    public void extractPreferredStructureId_whenPreferenceIsSet_shouldReturnIt() {
        final String widgets = "{\"school-widget\":{\"schoolId\":\"" + STRUCTURE_ID + "\"}}";

        final Optional<String> preferred = extractPreferredStructureId(widgets);

        assertTrue(preferred.isPresent());
        assertEquals(STRUCTURE_ID, preferred.get());
    }

    @Test
    public void extractPreferredStructureId_whenOtherWidgetsAreAlsoConfigured_shouldReturnTheSchoolOne() {
        final String widgets = "{\"birthday\":{\"enabled\":true},"
                + "\"school-widget\":{\"schoolId\":\"" + STRUCTURE_ID + "\",\"index\":2},"
                + "\"my-apps\":{\"enabled\":false}}";

        final Optional<String> preferred = extractPreferredStructureId(widgets);

        assertTrue(preferred.isPresent());
        assertEquals(STRUCTURE_ID, preferred.get());
    }

    @Test
    public void extractPreferredStructureId_whenSchoolWidgetIsAbsent_shouldReturnEmpty() {
        final String widgets = "{\"birthday\":{\"enabled\":true},\"my-apps\":{\"enabled\":false}}";

        assertFalse(extractPreferredStructureId(widgets).isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenSchoolWidgetHasNoStructureId_shouldReturnEmpty() {
        final String widgets = "{\"school-widget\":{\"index\":2}}";

        assertFalse(extractPreferredStructureId(widgets).isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenStructureIdIsBlank_shouldReturnEmpty() {
        assertFalse(extractPreferredStructureId("{\"school-widget\":{\"schoolId\":\"\"}}").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenStructureIdIsNull_shouldReturnEmpty() {
        assertFalse(extractPreferredStructureId("{\"school-widget\":{\"schoolId\":null}}").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenJsonIsMalformed_shouldReturnEmptyRatherThanThrow() {
        assertFalse(extractPreferredStructureId("{\"school-widget\":{\"schoolId\":").isPresent());
        assertFalse(extractPreferredStructureId("not json at all").isPresent());
        assertFalse(extractPreferredStructureId("{{{").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenBlobIsNotAnObject_shouldReturnEmpty() {
        // A JSON array, or a bare scalar, parses as valid JSON but not as the expected object.
        assertFalse(extractPreferredStructureId("[{\"school-widget\":{\"schoolId\":\"x\"}}]").isPresent());
        assertFalse(extractPreferredStructureId("42").isPresent());
        assertFalse(extractPreferredStructureId("\"a string\"").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenSchoolWidgetIsNotAnObject_shouldReturnEmpty() {
        // Older or buggy clients could store the id directly, or anything else, under the widget key.
        assertFalse(extractPreferredStructureId("{\"school-widget\":\"" + STRUCTURE_ID + "\"}").isPresent());
        assertFalse(extractPreferredStructureId("{\"school-widget\":[\"" + STRUCTURE_ID + "\"]}").isPresent());
        assertFalse(extractPreferredStructureId("{\"school-widget\":true}").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenStructureIdIsNotAString_shouldReturnEmpty() {
        assertFalse(extractPreferredStructureId("{\"school-widget\":{\"schoolId\":42}}").isPresent());
        assertFalse(extractPreferredStructureId("{\"school-widget\":{\"schoolId\":{\"id\":\"x\"}}}").isPresent());
    }

    @Test
    public void extractPreferredStructureId_whenPreferenceIsMissing_shouldReturnEmpty() {
        assertFalse(extractPreferredStructureId(null).isPresent());
        assertFalse(extractPreferredStructureId("").isPresent());
        assertFalse(extractPreferredStructureId("{}").isPresent());
    }
}