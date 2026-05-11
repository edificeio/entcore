# Feeder Manual Actions — DTO Migration Plan

## Context

The `ManualFeeder` class handles all manual data operations (create/update/delete structures, classes, users, groups, etc.) currently backed by Neo4j. The goal is to define typed input contracts (DTOs) for every action in preparation for a migration to PostgreSQL.

The `handleAction` switch in `Feeder.java` dispatches event-bus messages to `ManualFeeder` methods. Each case maps to a method that reads its inputs directly from the raw `JsonObject` message body. The migration introduces:

1. **DTO classes** — typed, explicit contracts per operation
2. **Mapper classes** — translate message body → DTO and DTO → JsonObject (for the existing Neo4j layer during transition)
3. **Refactored method signatures** — methods take a DTO + a `Handler<JsonObject>` reply callback instead of the raw `Message<JsonObject>`

The pattern for each operation, once fully migrated:

```
handleAction (Feeder.java)
  → StructureMapper.toXxxDTO(message.body())   [transport → domain]
  → manual.someMethod(dto, json -> message.reply(json))
      → DTO fields used directly (no more JsonObject parsing inside the method)
      → replyHandler.handle(result)             [domain → transport]
```

---

## Status

| Action | DTO created | Mapper created | Method refactored |
|---|:---:|:---:|:---:|
| `manual-create-structure` | ✅ | ✅ | ✅ |
| `manual-update-structure` | ✅ | ✅ | ✅ |
| `manual-create-class` | ✅ | ✅ | ✅ |
| `manual-update-class` | ✅ | ✅ | ✅ |
| `manual-remove-class` | ✅ | ✅ | ✅ |
| `manual-create-user` | ✅ | ✅ | ✅ |
| `manual-update-user` | ✅ | ✅ | ✅ |
| `manual-update-user-login` | ✅ | ✅ | ✅ |
| `manual-add-user` | ✅ | ✅ | ✅ |
| `manual-add-users` | ✅ | ✅ | ✅ |
| `manual-remove-user` | ✅ | ✅ | ✅ |
| `manual-remove-users` | ✅ | ✅ | ✅ |
| `manual-delete-user` | ✅ | ✅ | ✅ |
| `manual-restore-user` | ✅ | ✅ | ✅ |
| `manual-create-function` | ✅ | ✅ | ✅ |
| `manual-delete-function` | ✅ | ✅ | ✅ |
| `manual-delete-function-group` | ✅ | ✅ | ✅ |
| `manual-create-group` | ✅ | ✅ | ✅ |
| `manual-delete-group` | ✅ | ✅ | ✅ |
| `manual-add-group-users` | ✅ | ✅ | ✅ |
| `manual-remove-group-users` | ✅ | ✅ | ✅ |
| `manual-relative-student` | ✅ | ✅ | ✅ |
| `manual-unlink-relative-student` | ✅ | ✅ | ✅ |
| `manual-add-user-function` | ❌ | ❌ | ❌ |
| `manual-add-head-teacher` | ❌ | ❌ | ❌ |
| `manual-update-head-teacher` | ❌ | ❌ | ❌ |
| `manual-add-subject` | ❌ | ❌ | ❌ |
| `manual-update-subject` | ❌ | ❌ | ❌ |
| `manual-delete-subject` | ❌ | ❌ | ❌ |
| `manual-add-direction` | ❌ | ❌ | ❌ |
| `manual-remove-direction` | ❌ | ❌ | ❌ |
| `manual-remove-user-function` | ❌ | ❌ | ❌ |
| `manual-add-user-group` | ❌ | ❌ | ❌ |
| `manual-remove-user-group` | ❌ | ❌ | ❌ |
| `manual-update-email-group` | ❌ | ❌ | ❌ |
| `manual-create-tenant` | ❌ | ❌ | ❌ |
| `manual-structure-attachment` | ❌ | ❌ | ❌ |
| `manual-structure-detachment` | ❌ | ❌ | ❌ |
| `manual-link-user-positions` | ❌ | ❌ | ❌ |
| `manual-update-group-linked-positions` | ❌ | ❌ | ❌ |

---

## Full DTO Specifications

### Structures

#### `manual-create-structure` → `CreateStructureDTO`
Package: `org.entcore.feeder.dto`

The DTO only captures the fields that are manually settable at creation time. `externalId` is auto-generated server-side and not accepted from the caller. All other schema fields (feederName, siret, address, etc.) are not part of the manual create contract. `Structure.json` is used for internal validation of the persisted node but is not the source of truth for the DTO.

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `name` | `String` | `data.name` | ✅ | |
| `uai` | `String` | `data.UAI` | — | |
| `hasApp` | `Boolean` | `data.hasApp` | — | |
| `transactionId` | `Integer` | `transactionId` | — | infrastructure, may be removed in PG |
| `commit` | `Boolean` | `commit` | — | default `true`, infrastructure |

#### `manual-update-structure` → `UpdateStructureDTO`
Package: `org.entcore.feeder.dto`
Modifiable fields per schema: `name`, `UAI`, `hasApp`, `ignoreMFA`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `structureId` | `String` | `structureId` | ✅ | |
| `name` | `String` | `data.name` | — | also renames all child ProfileGroups |
| `uai` | `String` | `data.UAI` | — | |
| `hasApp` | `Boolean` | `data.hasApp` | — | |
| `ignoreMFA` | `Boolean` | `data.ignoreMFA` | — | |
| `userLogin` | `String` | `userLogin` | — | audit log only |
| `userId` | `String` | `userId` | — | audit log only |

---

### Classes

#### `manual-create-class`

`externalId` is auto-generated server-side as `structureId + "$" + name` and is not accepted from the caller. `Class.json` is used for internal node validation, not as the DTO contract.

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `structureId` | `String` | `structureId` | ✅ |
| `name` | `String` | `data.name` | ✅ |
| `transactionId` | `Integer` | `transactionId` | — |
| `commit` | `Boolean` | `commit` | — |

#### `manual-update-class`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `classId` | `String` | `classId` | ✅ |
| `name` | `String` | `data.name` | — |
| `level` | `String` | `data.level` | — |

#### `manual-remove-class`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `classId` | `String` | `classId` | ✅ |

---

### Users

#### `manual-create-user`

Callers: `DefaultUserService.createInStructure/createInClass` (via `DirectoryController`), `SSOAzure.createUserIfNeeded`, `CanopeCasClient.createUserClass`. The JSON schema validators (`Personnel.json`, `Student.json`, `User.json`) are applied inside `ManualFeeder` but are not the source of truth for the DTO contract.

**Outer fields** (message body):

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `profile` | `String` | `profile` | ✅ | enum: `Teacher`, `Personnel`, `Student`, `Relative`, `Guest` |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classId required |
| `classId` | `String` | `classId` | ✅* | |
| `classesNames` | `List<String>` | `classesNames` | — | SSOAzure only; requires structureId |
| `callerId` | `String` | `callerId` | — | DirectoryController only; used for position audit |

**`data` fields** (union of what all callers actually send):

| Field | Java type | JSON key | Sent by | Notes |
|---|---|---|---|---|
| `firstName` | `String` | `data.firstName` | all | |
| `lastName` | `String` | `data.lastName` | all | |
| `birthDate` | `String` | `data.birthDate` | DirectoryController (optional), SSOAzure (required for Student) | |
| `childrenIds` | `List<String>` | `data.childrenIds` | DirectoryController | Relative only |
| `userPositionIds` | `List<String>` | `data.userPositionIds` | DirectoryController | Personnel only |
| `externalId` | `String` | `data.externalId` | SSOAzure, Canope | auto-generated UUID if absent |
| `email` | `String` | `data.email` | SSOAzure, Canope | |
| `emailAcademy` | `String` | `data.emailAcademy` | SSOAzure, Canope | |
| `source` | `String` | `data.source` | SSOAzure | hardcoded `"SSO"`; controls userSource inside ManualFeeder |
| `profile` | `String` | `data.profile` | SSOAzure, Canope | duplicated inside data by those callers |
| `profiles` | `List<String>` | `data.profiles` | all (added by DefaultUserService or caller) | legacy field, not used by feeder logic |
| `type` | `String` | `data.type` | DirectoryController | same value as outer `profile`; added before passing to DefaultUserService |

#### `manual-update-user`

Single caller: `DefaultUserService.update`, invoked by `UserController.update`. The request body is validated against `directory/src/main/resources/jsonschema/updateUser.json` (`additionalProperties: false`) before reaching the feeder — that schema is the source of truth for `data` fields.

**Outer fields:**

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userId` | `String` | `userId` | ✅ | from path param |
| `callerId` | `String` | `callerId` | — | from caller's UserInfos |

**`data` fields** (constrained by `updateUser.json`):

| Field | Java type | JSON key | Notes |
|---|---|---|---|
| `firstName` | `String` | `data.firstName` | stripped by controller for non-admins/non-class-admins |
| `lastName` | `String` | `data.lastName` | stripped by controller for non-admins/non-class-admins |
| `displayName` | `String` | `data.displayName` | |
| `birthDate` | `String` | `data.birthDate` | nullable |
| `address` | `String` | `data.address` | nullable |
| `zipCode` | `String` | `data.zipCode` | nullable |
| `city` | `String` | `data.city` | nullable |
| `loginAlias` | `String` | `data.loginAlias` | nullable |
| `email` | `String` | `data.email` | nullable |
| `homePhone` | `String` | `data.homePhone` | nullable |
| `mobile` | `String` | `data.mobile` | nullable; normalized to E.164 by controller before sending |
| `childrenIds` | `String` | `data.childrenIds` | nullable; plain string in schema (not array) |
| `positionIds` | `List<String>` | `data.positionIds` | stripped by controller for non-admins |

#### `manual-update-user-login`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `login` | `String` | `login` | ✅ |

#### `manual-add-user`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userId` | `String` | `userId` | ✅ | |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classId |
| `classId` | `String` | `classId` | ✅* | |

#### `manual-add-users`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userIds` | `List<String>` | `userIds` | ✅ | |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classId |
| `classId` | `String` | `classId` | ✅* | |

#### `manual-remove-user`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userId` | `String` | `userId` | ✅ | |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classId |
| `classId` | `String` | `classId` | ✅* | |

#### `manual-remove-users`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userIds` | `List<String>` | `userIds` | ✅ | |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classIds |
| `classIds` | `List<String>` | `classIds` | ✅* | parallel array with `userIds` |

#### `manual-delete-user`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `users` | `List<String>` | `users` | ✅ | list of user IDs |

#### `manual-restore-user`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `users` | `List<String>` | `users` | ✅ | list of user IDs |

---

### Functions

#### `manual-create-function`

Single caller: `DefaultProfileService.createFunction`, invoked by `ProfileController.createFunction`. The request body is validated against `directory/src/main/resources/jsonschema/createFunction.json` (`additionalProperties: false`) before reaching the feeder — that schema is the source of truth for `data` fields.

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `profile` | `String` | `profile` | ✅ | from path param; enum: Teacher, Personnel, Student, Relative, Guest |
| `externalId` | `String` | `data.externalId` | ✅ | required by schema |
| `name` | `String` | `data.name` | ✅ | required by schema |

#### `manual-delete-function`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `functionCode` | `String` | `functionCode` | ✅ |

#### `manual-delete-function-group`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `groupId` | `String` | `groupId` | ✅ |

---

### Groups

#### `manual-create-group`

Four callers, all routed through `DefaultGroupService.createOrUpdateManual` which always adds `groupDisplayName = group.name` before sending. All `group` fields land as ManualGroup node properties.

- **`GroupController.create`** — validates body with `createManualGroup.json` (`additionalProperties: false`, requires `name`); then strips autolink fields for non-super-admins; adds `createdById`, `createdByName`, `createdAt`; passes `structureId`/`classId` separately.
- **`GroupController.update`** — validates body with `updateManualGroup.json` (`additionalProperties: false`, requires `name`); then strips autolink fields for non-super-admins; adds `id` (path param), `modifiedById`, `modifiedByName`, `modifiedAt`; always passes `structureId=null`, `classId=null`.
- **`DefaultSchoolService.activateGar`** — sends `name`, `id`, `lockDelete=true`; passes `structureId`.
- **`DirectoryBrokerListenerImpl.createManualGroup`** — sends `name`, `filter`, `id=""` (empty = create), optionally `externalId`; passes `structureId`/`classId`.
- **`DirectoryBrokerListenerImpl.updateManualGroup`** — sends `id`, optionally `name`; passes empty `structureId`/`classId`.

**Outer fields:**

| Field | Java type | JSON key | Notes |
|---|---|---|---|
| `structureId` | `String` | `structureId` | links group to structure on create |
| `classId` | `String` | `classId` | links group to class on create |

**`group` fields** (union of all callers):

| Field | Java type | JSON key | Notes |
|---|---|---|---|
| `name` | `String` | `group.name` | required by all HTTP paths |
| `id` | `String` | `group.id` | empty string or absent = create; non-empty = upsert |
| `groupDisplayName` | `String` | `group.groupDisplayName` | always set to `name` by `DefaultGroupService` |
| `filter` | `String` | `group.filter` | broker path only |
| `externalId` | `String` | `group.externalId` | broker path only |
| `subType` | `String` | `group.subType` | `createManualGroup.json` |
| `autolinkTargetAllStructs` | `Boolean` | `group.autolinkTargetAllStructs` | super-admin HTTP only |
| `autolinkTargetStructs` | `List<String>` | `group.autolinkTargetStructs` | super-admin HTTP only |
| `autolinkUsersFromGroups` | `List<String>` | `group.autolinkUsersFromGroups` | super-admin HTTP only |
| `autolinkUsersFromPositions` | `List<String>` | `group.autolinkUsersFromPositions` | super-admin HTTP only |
| `autolinkUsersFromLevels` | `List<String>` | `group.autolinkUsersFromLevels` | super-admin HTTP only |
| `lockDelete` | `Boolean` | `group.lockDelete` | GAR path and `updateManualGroup.json` |
| `lockCompose` | `Boolean` | `group.lockCompose` | `updateManualGroup.json` |
| `createdById` | `String` | `group.createdById` | `GroupController.create` |
| `createdByName` | `String` | `group.createdByName` | `GroupController.create` |
| `createdAt` | `Long` | `group.createdAt` | `GroupController.create` (epoch ms) |
| `modifiedById` | `String` | `group.modifiedById` | `GroupController.update` |
| `modifiedByName` | `String` | `group.modifiedByName` | `GroupController.update` |
| `modifiedAt` | `Long` | `group.modifiedAt` | `GroupController.update` (epoch ms) |

#### `manual-delete-group`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `groupId` | `String` | `groupId` | ✅ |

#### `manual-add-group-users`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `groupId` | `String` | `groupId` | ✅ |
| `userIds` | `List<String>` | `userIds` | ✅ |

#### `manual-remove-group-users`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `groupId` | `String` | `groupId` | ✅ |
| `userIds` | `List<String>` | `userIds` | ✅ |

#### `manual-update-email-group`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `groupId` | `String` | `groupId` | ✅ |
| `email` | `String` | `email` | ✅ |

---

### Relatives / Head Teachers / Direction

#### `manual-relative-student`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `relativeId` | `String` | `relativeId` | ✅ |
| `studentId` | `String` | `studentId` | ✅ |

#### `manual-unlink-relative-student`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `relativeId` | `String` | `relativeId` | ✅ |
| `studentId` | `String` | `studentId` | ✅ |

#### `manual-add-user-function`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `userId` | `String` | `userId` | ✅ | |
| `function` | `String` | `function` | ✅ | function code |
| `scope` | `List<String>` | `scope` | — | structure IDs |
| `inherit` | `String` | `inherit` | — | enum: `"s"` (child structures), `"sc"` (structures + classes), `""` |

#### `manual-remove-user-function`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `function` | `String` | `function` | ✅ |

#### `manual-add-head-teacher`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `classExternalId` | `String` | `classExternalId` | ✅ |
| `structureExternalId` | `String` | `structureExternalId` | ✅ |

#### `manual-update-head-teacher`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `classExternalId` | `String` | `classExternalId` | ✅ |
| `structureExternalId` | `String` | `structureExternalId` | ✅ |

#### `manual-add-direction`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `structureExternalId` | `String` | `structureExternalId` | ✅ |

#### `manual-remove-direction`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `structureExternalId` | `String` | `structureExternalId` | ✅ |

#### `manual-add-user-group`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `groupId` | `String` | `groupId` | ✅ |

#### `manual-remove-user-group`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `groupId` | `String` | `groupId` | ✅ |

---

### Subjects

#### `manual-add-subject`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `structureId` | `String` | `subject.structureId` | ✅ |
| `label` | `String` | `subject.label` | ✅ |
| `code` | `String` | `subject.code` | ✅ |

#### `manual-update-subject`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `id` | `String` | `subject.id` | ✅ |
| `label` | `String` | `subject.label` | ✅ |
| `code` | `String` | `subject.code` | ✅ |

#### `manual-delete-subject`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `subjectId` | `String` | `subjectId` | ✅ |

---

### Tenant

#### `manual-create-tenant`
Validated by: `dictionary/schema/Tenant.json`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `externalId` | `String` | `data.externalId` | ✅ |
| `name` | `String` | `data.name` | ✅ |
| `shortName` | `String` | `data.shortName` | — |
| `email` | `String` | `data.email` | — |
| `url` | `String` | `data.url` | — |
| `entUrl` | `String` | `data.entUrl` | — |
| `linkRules` | `List<String>` | `data.linkRules` | — |

---

### Structure Hierarchy

#### `manual-structure-attachment`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `structureId` | `String` | `structureId` | ✅ |
| `parentStructureId` | `String` | `parentStructureId` | ✅ |
| `transactionId` | `Integer` | `transactionId` | — |
| `commit` | `Boolean` | `commit` | — |
| `autoSend` | `Boolean` | `autoSend` | — |

#### `manual-structure-detachment`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `structureId` | `String` | `structureId` | ✅ |
| `parentStructureId` | `String` | `parentStructureId` | ✅ |

---

### User Positions

#### `manual-link-user-positions`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `groupId` | `String` | `groupId` | ✅ | |
| `manualGroupAutolinkUsersPositions` | `List<String>` | `manualGroupAutolinkUsersPositions` | ✅ | list of position names |

#### `manual-update-group-linked-positions`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userPosition` | `String` | `userPosition` | ✅ |

---

## Implementation Notes

### DTO pattern: vertx-codegen @DataObject

All DTOs use the vertx-codegen `@DataObject` + `@JsonGen` annotations, matching the pattern established in the communication module. The `CodeGenProcessor` annotation processor (already configured in the parent pom's `maven-compiler-plugin`) generates a `*Converter` class alongside each DTO during compilation. The generated converter handles all `fromJson`/`toJson` field mapping automatically.

**Build setup required in feeder:**
- Add `vertx-codegen` dependency to `feeder/pom.xml` (same as `communication/pom.xml`)
- Add `package-info.java` in `org.entcore.feeder.dto` with `@ModuleGen(name = "feeder-dto", groupPackage = "org.entcore.feeder")`

**Every DTO must follow this structure:**

```java
@DataObject
@JsonGen
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SomeDTO {
    // fields ...

    public SomeDTO() {}

    public SomeDTO(JsonObject json) {
        SomeDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SomeDTOConverter.toJson(this, json);
        return json;
    }

    // fluent setters (return `this`)
}
```

For DTOs that extend another `@DataObject` class, add `@JsonGen(inheritConverter = true)` and call `super(json)` in the JsonObject constructor.

**Existing plain-POJO DTOs** (CreateStructureDTO, UpdateStructureDTO, CreateClassDTO, UpdateClassDTO, RemoveClassDTO, CreateUserDTO, UserDataDTO, UpdateUserDTO, UpdateUserDataDTO, UpdateUserLoginDTO, AddUserDTO, AddUsersDTO, RemoveUserDTO, RemoveUsersDTO, DeleteUserDTO, RestoreUserDTO, CreateFunctionDTO, DeleteFunctionDTO, DeleteFunctionGroupDTO, CreateGroupDTO, GroupDataDTO) must be migrated to this pattern as part of the ongoing work.

### Mapper naming convention

One mapper class per domain entity, placed in `org.entcore.feeder.dto`:
- `StructureMapper` — structures (exists, plain POJO — migrate to @DataObject)
- `ClassMapper` — classes (exists, plain POJO — migrate to @DataObject)
- `UserMapper` — users (exists, plain POJO — migrate to @DataObject; complex: profile-dependent)
- `GroupMapper` — groups, functions, head teachers, direction, user-group relations (exists, partial — migrate to @DataObject)
- `SubjectMapper` — subjects
- `TenantMapper` — tenants

### Mapper responsibilities

Mappers remain hand-written because the incoming JSON structure does not always match the DTO field layout:
- nested `data` sub-objects must be extracted and flattened explicitly (e.g., `body.getJsonObject("data").getString("UAI")` → `dto.setUai(...)`)
- JSON keys with non-standard casing (e.g., `"UAI"`) are renamed to camelCase DTO fields
- some JSON arrays require stream-based conversion

The `JSON → DTO` direction (e.g., `StructureMapper.toCreateStructureDTO(body)`) stays fully hand-written.

The `DTO → JsonObject` direction is handled by the generated `dto.toJson()`. The `to*Props()` methods in mappers act as thin selectors on top of it: they call `dto.toJson()` and remove infrastructure-only fields (`transactionId`, `commit`, `autoSend`) that must not become Neo4j node properties.

### Handling `transactionId` / `commit` / `autoSend`
These are infrastructure fields from the Neo4j transaction API. They have no equivalent in PostgreSQL and should be dropped from DTOs once the underlying persistence layer is replaced. During the transition period they are kept in the DTO.

### Validation
Currently validation happens inside `ManualFeeder` using the `Validator` class and JSON schemas. During transition, the `toStructureProps(dto)` round-trip in `StructureMapper` recreates the `JsonObject` needed by the existing validator. Once persistence is migrated, the JSON schema validators should be replaced by a dedicated service-layer validation step operating on the DTO directly.

### Profile-dependent user DTO
`manual-create-user` and `manual-update-user` are the most complex cases because the `data` shape depends on `profile`. Options:
1. One DTO per profile (`CreateTeacherDTO`, `CreateStudentDTO`, etc.) — most type-safe, most verbose
2. One shared `UserDataDTO` with a union of all optional fields — simpler, less precise
3. A sealed hierarchy (Java 17+) — best long-term if the JDK allows it

Current codebase targets Java 11, so option 2 (shared DTO with nullable fields per profile) is the most pragmatic choice.