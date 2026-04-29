package org.entcore.feeder.mapper;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.feeder.dto.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Non-regression tests for all Mapper classes.
 *
 * Each mapper translates a raw EventBus JsonObject body into a typed DTO.
 * The tests here verify the non-obvious mappings: nested field extraction,
 * key renames, default values, and props serialisation (DTO → JsonObject).
 *
 * No DB or Vertx needed — mappers are pure functions.
 */
public class MapperTest {

    // ─── StructureMapper ──────────────────────────────────────────────────────

    @Test
    public void createStructure_extractsFieldsFromDataBlock() {
        JsonObject body = new JsonObject()
                .put("data", new JsonObject()
                        .put("name", "Lycée Pasteur")
                        .put("UAI", "0123456A")
                        .put("hasApp", true))
                .put("transactionId", 42)
                .put("commit", false);

        CreateStructureDTO dto = StructureMapper.toCreateStructureDTO(body);

        assertEquals("Lycée Pasteur", dto.getName());
        assertEquals("0123456A", dto.getUai());
        assertTrue(dto.getHasApp());
        assertEquals(Integer.valueOf(42), dto.getTransactionId());
        assertFalse(dto.getCommit());
    }

    @Test
    public void createStructure_commitDefaultsToTrue() {
        CreateStructureDTO dto = StructureMapper.toCreateStructureDTO(new JsonObject());
        assertTrue(dto.getCommit());
    }

    @Test
    public void updateStructure_extractsStructureIdFromRoot_dataFieldsFromBlock() {
        JsonObject body = new JsonObject()
                .put("structureId", "struct-1")
                .put("userLogin", "jdoe")
                .put("userId", "user-1")
                .put("data", new JsonObject()
                        .put("name", "Collège Curie")
                        .put("UAI", "9999999Z")
                        .put("hasApp", false)
                        .put("ignoreMFA", true));

        UpdateStructureDTO dto = StructureMapper.toUpdateStructureDTO(body);

        assertEquals("struct-1", dto.getStructureId());
        assertEquals("Collège Curie", dto.getName());
        assertEquals("9999999Z", dto.getUai());
        assertFalse(dto.getHasApp());
        assertTrue(dto.getIgnoreMFA());
        assertEquals("jdoe", dto.getUserLogin());
        assertEquals("user-1", dto.getUserId());
    }

    @Test
    public void updateStructure_userLoginAndUserIdDefaultToEmptyString() {
        UpdateStructureDTO dto = StructureMapper.toUpdateStructureDTO(new JsonObject());
        assertEquals("", dto.getUserLogin());
        assertEquals("", dto.getUserId());
    }

    @Test
    public void createStructureProps_usesUAIKeyNotUai_omitsNullFields() {
        CreateStructureDTO dto = new CreateStructureDTO()
                .setName("Test")
                .setUai("0000001A");
        // hasApp is null → must be absent from props

        JsonObject props = StructureMapper.toStructureProps(dto);

        assertEquals("Test", props.getString("name"));
        assertEquals("0000001A", props.getString("UAI"));
        assertFalse("null hasApp must be omitted", props.containsKey("hasApp"));
    }

    @Test
    public void updateStructureProps_usesUAIKeyNotUai_omitsNullFields() {
        UpdateStructureDTO dto = new UpdateStructureDTO()
                .setUai("0000002B")
                .setIgnoreMFA(true);
        // name and hasApp are null

        JsonObject props = StructureMapper.toStructureProps(dto);

        assertEquals("0000002B", props.getString("UAI"));
        assertTrue(props.getBoolean("ignoreMFA"));
        assertFalse("null name must be omitted", props.containsKey("name"));
        assertFalse("null hasApp must be omitted", props.containsKey("hasApp"));
    }

    // ─── ClassMapper ─────────────────────────────────────────────────────────

    @Test
    public void createClass_extractsNameFromDataBlock() {
        JsonObject body = new JsonObject()
                .put("structureId", "struct-1")
                .put("transactionId", 7)
                .put("data", new JsonObject().put("name", "6ème A"));

        CreateClassDTO dto = ClassMapper.toCreateClassDTO(body);

        assertEquals("struct-1", dto.getStructureId());
        assertEquals("6ème A", dto.getName());
        assertEquals(Integer.valueOf(7), dto.getTransactionId());
        assertTrue(dto.getCommit());  // default
    }

    @Test
    public void createClass_commitDefaultsToTrue() {
        CreateClassDTO dto = ClassMapper.toCreateClassDTO(new JsonObject());
        assertTrue(dto.getCommit());
    }

    @Test
    public void updateClass_extractsNameAndLevelFromDataBlock() {
        JsonObject body = new JsonObject()
                .put("classId", "class-1")
                .put("data", new JsonObject()
                        .put("name", "5ème B")
                        .put("level", "5"));

        UpdateClassDTO dto = ClassMapper.toUpdateClassDTO(body);

        assertEquals("class-1", dto.getClassId());
        assertEquals("5ème B", dto.getName());
        assertEquals("5", dto.getLevel());
    }

    @Test
    public void createClassProps_stripsStructureIdTransactionIdAndCommit() {
        CreateClassDTO dto = new CreateClassDTO()
                .setName("3ème C")
                .setStructureId("struct-42")
                .setTransactionId(1)
                .setCommit(true);

        JsonObject props = ClassMapper.toClassProps(dto);

        assertEquals("3ème C", props.getString("name"));
        assertFalse(props.containsKey("structureId"));
        assertFalse(props.containsKey("transactionId"));
        assertFalse(props.containsKey("commit"));
    }

    @Test
    public void updateClassProps_stripsClassId() {
        UpdateClassDTO dto = new UpdateClassDTO()
                .setClassId("class-99")
                .setName("Terminale")
                .setLevel("T");

        JsonObject props = ClassMapper.toClassProps(dto);

        assertEquals("Terminale", props.getString("name"));
        assertFalse(props.containsKey("classId"));
    }

    // ─── FunctionMapper ───────────────────────────────────────────────────────

    @Test
    public void createFunction_extractsProfileFromRoot_externalIdAndNameFromData() {
        JsonObject body = new JsonObject()
                .put("profile", "Teacher")
                .put("data", new JsonObject()
                        .put("externalId", "FUNC-01")
                        .put("name", "Enseignant"));

        CreateFunctionDTO dto = FunctionMapper.toCreateFunctionDTO(body);

        assertEquals("Teacher", dto.getProfile());
        assertEquals("FUNC-01", dto.getExternalId());
        assertEquals("Enseignant", dto.getName());
    }

    @Test
    public void functionData_stripsProfileFromJson() {
        CreateFunctionDTO dto = new CreateFunctionDTO()
                .setProfile("Admin")
                .setExternalId("FUNC-02")
                .setName("Administrateur");

        JsonObject data = FunctionMapper.toFunctionData(dto);

        assertFalse("profile must be stripped from function data", data.containsKey("profile"));
        assertEquals("FUNC-02", data.getString("externalId"));
        assertEquals("Administrateur", data.getString("name"));
    }

    // ─── SubjectMapper ────────────────────────────────────────────────────────

    @Test
    public void createSubject_readsFromSubjectNestedObject() {
        JsonObject body = new JsonObject()
                .put("subject", new JsonObject()
                        .put("structureId", "struct-1")
                        .put("label", "Mathématiques")
                        .put("code", "MATH"));

        CreateSubjectDTO dto = SubjectMapper.toCreateSubjectDTO(body);

        assertEquals("struct-1", dto.getStructureId());
        assertEquals("Mathématiques", dto.getLabel());
        assertEquals("MATH", dto.getCode());
    }

    @Test
    public void updateSubject_readsFromSubjectNestedObject() {
        JsonObject body = new JsonObject()
                .put("subject", new JsonObject()
                        .put("id", "subj-99")
                        .put("label", "Histoire")
                        .put("code", "HIST"));

        UpdateSubjectDTO dto = SubjectMapper.toUpdateSubjectDTO(body);

        assertEquals("subj-99", dto.getId());
        assertEquals("Histoire", dto.getLabel());
        assertEquals("HIST", dto.getCode());
    }

    @Test
    public void createSubject_emptyBodyProducesEmptyDTO() {
        CreateSubjectDTO dto = SubjectMapper.toCreateSubjectDTO(new JsonObject());
        assertNull(dto.getStructureId());
        assertNull(dto.getLabel());
    }

    // ─── TimetableMapper ──────────────────────────────────────────────────────

    @Test
    public void edtDTO_mapsUAIKeyExplicitly() {
        JsonObject body = new JsonObject()
                .put("UAI", "0123456A")
                .put("path", "/tmp/edt.xml")
                .put("language", "fr");

        EdtDTO dto = TimetableMapper.toEdtDTO(body);

        assertEquals("0123456A", dto.getUai());
        assertEquals("/tmp/edt.xml", dto.getPath());
        assertEquals("fr", dto.getLanguage());
    }

    @Test
    public void udtDTO_mapsUAIKeyExplicitly() {
        JsonObject body = new JsonObject().put("UAI", "9876543Z").put("path", "/tmp/udt.xml");

        UdtDTO dto = TimetableMapper.toUdtDTO(body);

        assertEquals("9876543Z", dto.getUai());
    }

    @Test
    public void initTimetableStructure_readsFromConfBlock() {
        JsonObject body = new JsonObject()
                .put("conf", new JsonObject()
                        .put("type", "EDT")
                        .put("structureId", "struct-edt"));

        InitTimetableStructureDTO dto = TimetableMapper.toInitTimetableStructureDTO(body);

        assertEquals("EDT", dto.getType());
        assertEquals("struct-edt", dto.getStructureId());
    }

    @Test
    public void initTimetableStructure_emptyBodyProducesEmptyDTO() {
        InitTimetableStructureDTO dto = TimetableMapper.toInitTimetableStructureDTO(new JsonObject());
        assertNull(dto.getType());
        assertNull(dto.getStructureId());
    }

    // ─── ClassesMappingMapper ─────────────────────────────────────────────────

    @Test
    public void classesMappingMapper_mapsTypoLangageToLanguageField() {
        JsonObject body = new JsonObject()
                .put("langage", "fr")    // intentional typo — legacy protocol field
                .put("path", "/tmp/mapping.csv");

        ClassesMappingDTO dto = ClassesMappingMapper.toClassesMappingDTO(body);

        assertEquals("fr", dto.getLanguage());
        assertEquals("/tmp/mapping.csv", dto.getPath());
    }

    @Test
    public void classesMappingMapper_absentLangageProducesNullLanguage() {
        ClassesMappingDTO dto = ClassesMappingMapper.toClassesMappingDTO(new JsonObject());
        assertNull(dto.getLanguage());
    }

    // ─── TenantMapper ─────────────────────────────────────────────────────────

    @Test
    public void createTenant_readsFromDataBlock() {
        JsonObject body = new JsonObject()
                .put("data", new JsonObject()
                        .put("externalId", "tenant-1")
                        .put("name", "Académie de Paris")
                        .put("shortName", "Ac. Paris")
                        .put("email", "contact@ac-paris.fr")
                        .put("url", "https://ac-paris.fr")
                        .put("entUrl", "https://ent.ac-paris.fr"));

        CreateTenantDTO dto = TenantMapper.toCreateTenantDTO(body);

        assertEquals("tenant-1", dto.getExternalId());
        assertEquals("Académie de Paris", dto.getName());
        assertEquals("Ac. Paris", dto.getShortName());
        assertEquals("contact@ac-paris.fr", dto.getEmail());
        assertEquals("https://ac-paris.fr", dto.getUrl());
        assertEquals("https://ent.ac-paris.fr", dto.getEntUrl());
    }

    @Test
    public void createTenant_emptyBodyProducesEmptyDTO() {
        CreateTenantDTO dto = TenantMapper.toCreateTenantDTO(new JsonObject());
        assertNull(dto.getName());
    }

    // ─── ValidateMapper ───────────────────────────────────────────────────────

    @Test
    public void validate_mapsAdmlStructuresWithDashKeyToCamelField() {
        JsonObject body = new JsonObject()
                .put("feeder", "CSV")
                .put("adml-structures", new JsonArray(Arrays.asList("struct-a", "struct-b")));

        ValidateDTO dto = ValidateMapper.toValidateDTO(body);

        assertEquals("CSV", dto.getFeeder());
        List<String> adml = dto.getAdmlStructures();
        assertNotNull(adml);
        assertEquals(2, adml.size());
        assertTrue(adml.contains("struct-a"));
        assertTrue(adml.contains("struct-b"));
    }

    @Test
    public void validate_absentAdmlStructuresLeavesFieldNull() {
        ValidateDTO dto = ValidateMapper.toValidateDTO(new JsonObject().put("feeder", "AAF"));
        assertNull(dto.getAdmlStructures());
    }

    // ─── ValidateWithIdMapper ─────────────────────────────────────────────────

    @Test
    public void validateWithId_mapsAdmlStructuresWithDashKeyToCamelField() {
        JsonObject body = new JsonObject()
                .put("id", "import-123")
                .put("adml-structures", new JsonArray(Arrays.asList("struct-x")));

        ValidateWithIdDTO dto = ValidateWithIdMapper.toValidateWithIdDTO(body);

        assertEquals("import-123", dto.getId());
        assertNotNull(dto.getAdmlStructures());
        assertTrue(dto.getAdmlStructures().contains("struct-x"));
    }

    @Test
    public void validateWithId_absentAdmlStructuresLeavesFieldNull() {
        ValidateWithIdDTO dto = ValidateWithIdMapper.toValidateWithIdDTO(new JsonObject().put("id", "x"));
        assertNull(dto.getAdmlStructures());
    }
}
