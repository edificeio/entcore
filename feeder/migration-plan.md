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
| `manual-create-class` | ❌ | ❌ | ❌ |
| `manual-update-class` | ❌ | ❌ | ❌ |
| `manual-remove-class` | ❌ | ❌ | ❌ |
| `manual-create-user` | ❌ | ❌ | ❌ |
| `manual-update-user` | ❌ | ❌ | ❌ |
| `manual-update-user-login` | ❌ | ❌ | ❌ |
| `manual-add-user` | ❌ | ❌ | ❌ |
| `manual-add-users` | ❌ | ❌ | ❌ |
| `manual-remove-user` | ❌ | ❌ | ❌ |
| `manual-remove-users` | ❌ | ❌ | ❌ |
| `manual-delete-user` | ❌ | ❌ | ❌ |
| `manual-restore-user` | ❌ | ❌ | ❌ |
| `manual-create-function` | ❌ | ❌ | ❌ |
| `manual-delete-function` | ❌ | ❌ | ❌ |
| `manual-delete-function-group` | ❌ | ❌ | ❌ |
| `manual-create-group` | ❌ | ❌ | ❌ |
| `manual-delete-group` | ❌ | ❌ | ❌ |
| `manual-add-group-users` | ❌ | ❌ | ❌ |
| `manual-remove-group-users` | ❌ | ❌ | ❌ |
| `manual-relative-student` | ❌ | ❌ | ❌ |
| `manual-unlink-relative-student` | ❌ | ❌ | ❌ |
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
Validated by: `dictionary/schema/Structure.json`

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `name` | `String` | `data.name` | ✅ | |
| `externalId` | `String` | `data.externalId` | — | auto-generated UUID if absent |
| `feederName` | `String` | `data.feederName` | — | |
| `siret` | `String` | `data.SIRET` | — | |
| `siren` | `String` | `data.SIREN` | — | |
| `joinKey` | `List<String>` | `data.joinKey` | — | |
| `uai` | `String` | `data.UAI` | — | |
| `type` | `String` | `data.type` | — | |
| `address` | `String` | `data.address` | — | |
| `postbox` | `String` | `data.postbox` | — | |
| `zipCode` | `String` | `data.zipCode` | — | |
| `city` | `String` | `data.city` | — | |
| `phone` | `String` | `data.phone` | — | |
| `accountable` | `String` | `data.accountable` | — | |
| `email` | `String` | `data.email` | — | |
| `website` | `String` | `data.website` | — | |
| `contact` | `String` | `data.contact` | — | |
| `ministry` | `String` | `data.ministry` | — | |
| `contract` | `String` | `data.contract` | — | |
| `administrativeAttachment` | `List<String>` | `data.administrativeAttachment` | — | |
| `functionalAttachment` | `List<String>` | `data.functionalAttachment` | — | |
| `area` | `String` | `data.area` | — | |
| `town` | `String` | `data.town` | — | |
| `district` | `String` | `data.district` | — | |
| `sector` | `String` | `data.sector` | — | |
| `rpi` | `String` | `data.rpi` | — | |
| `academy` | `String` | `data.academy` | — | |
| `hasApp` | `Boolean` | `data.hasApp` | — | |
| `groups` | `List<String>` | `data.groups` | — | |
| `ignoreMFA` | `Boolean` | `data.ignoreMFA` | — | |
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
Validated by: `dictionary/schema/Class.json`

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `structureId` | `String` | `structureId` | ✅ |
| `name` | `String` | `data.name` | ✅ |
| `externalId` | `String` | `data.externalId` | — |
| `level` | `String` | `data.level` | — |
| `transactionId` | `Integer` | `transactionId` | — |
| `commit` | `Boolean` | `commit` | — |

#### `manual-update-class`
Modifiable fields per schema: `name`, `level`

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
Validated by profile-specific schema (see below).

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `profile` | `String` | `profile` | ✅ | enum: `Teacher`, `Personnel`, `Student`, `Relative`, `Guest` |
| `structureId` | `String` | `structureId` | ✅* | *either structureId or classId required |
| `classId` | `String` | `classId` | ✅* | |
| `classesNames` | `List<String>` | `classesNames` | — | only with structureId |
| `callerId` | `String` | `callerId` | — | for user position audit |
| `userData` | see below | `data` | ✅ | profile-dependent |

**Teacher / Personnel** (`data`, validated by `Personnel.json`):

| Field | Java type | Required |
|---|---|:---:|
| `firstName` | `String` | ✅ |
| `lastName` | `String` | ✅ |
| `externalId` | `String` | — |
| `displayName` | `String` | — |
| `birthDate` | `String` | — |
| `title` | `String` | — |
| `email` / `emailAcademy` / `emailInternal` | `String` | — |
| `homePhone` / `workPhone` / `mobile` | `String` | — |
| `address` / `postbox` / `zipCode` / `city` / `country` | `String` | — |
| `subjectTaught` / `headTeacher` / `classCategories` / `modules` | `List<String>` | — |
| `teaches` / `isTeacher` | `Boolean` | — |
| `loginAlias` | `String` | — |
| `userPositionIds` | `List<String>` | — | Personnel only |

**Student** (`data`, validated by `Student.json`):

| Field | Java type | Required |
|---|---|:---:|
| `firstName` | `String` | ✅ |
| `lastName` | `String` | ✅ |
| `birthDate` | `String` | ✅ |
| `externalId` | `String` | — |
| `level` / `sector` / `classType` / `module` / `moduleName` | `String` | — |
| `email` / `emailInternal` | `String` | — |
| `homePhone` / `workPhone` / `mobile` | `String` | — |
| `address` / `postbox` / `zipCode` / `city` / `country` | `String` | — |
| `ine` / `status` / `attachmentId` / `attachmentMENAgriId` | `String` | — |
| `relative` / `relativeAddress` / `fieldOfStudy` / `fieldOfStudyLabels` | `List<String>` | — |
| `transport` / `schoolCanteen` / `supervisedStudy` / `morningChildcare` / `afternoonChildcare` / `scholarshipHolder` | `Boolean` | — |
| `loginAlias` | `String` | — |

**Relative / Guest** (`data`, validated by `User.json`):

| Field | Java type | Required | Notes |
|---|---|:---:|---|
| `firstName` | `String` | ✅ | |
| `lastName` | `String` | ✅ | |
| `childrenIds` | `List<String>` | — | Relative only |
| `externalId` | `String` | — | |
| `email` / `emailInternal` | `String` | — | |
| `homePhone` / `workPhone` / `mobile` / `mobilePhone` | `String`/`List<String>` | — | |
| `address` / `postbox` / `zipCode` / `city` / `country` | `String` | — | |
| `birthDate` | `String` | — | |
| `loginAlias` | `String` | — | |

#### `manual-update-user`
Modifiable fields are identical across all profiles.

| Field | Java type | JSON key | Required |
|---|---|---|:---:|
| `userId` | `String` | `userId` | ✅ |
| `callerId` | `String` | `callerId` | — |
| `firstName` | `String` | `data.firstName` | — |
| `lastName` | `String` | `data.lastName` | — |
| `password` | `String` | `data.password` | — |
| `displayName` | `String` | `data.displayName` | — |
| `surname` / `otherNames` | `String`/`List<String>` | `data.*` | — |
| `address` / `postbox` / `zipCode` / `city` / `country` | `String` | `data.*` | — |
| `homePhone` / `workPhone` / `mobile` | `String` | `data.*` | — |
| `email` / `emailInternal` | `String` | `data.*` | — |
| `birthDate` | `String` | `data.birthDate` | — |
| `loginAlias` | `String` | `data.loginAlias` | — |
| `positionIds` | `List<String>` | `data.positionIds` | — |
| `transactionId` | `Integer` | `transactionId` | — |
| `commit` | `Boolean` | `commit` | — |

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

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `profile` | `String` | `profile` | ✅ | enum: Teacher, Personnel, Student, Relative, Guest |
| `externalId` | `String` | `data.externalId` | — | used for deletion |
| `name` | `String` | `data.name` | — | |
| *(other props)* | `String` | `data.*` | — | persisted as-is on Neo4j node |

Note: no schema validation — all `data` fields land as Function node properties.

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
No schema validation — all fields in `group` land as ManualGroup node properties.

| Field | Java type | JSON key | Required | Notes |
|---|---|---|:---:|---|
| `name` | `String` | `group.name` | ✅ | |
| `id` | `String` | `group.id` | — | if present: upsert; if absent: create |
| `filter` | `String` | `group.filter` | — | defaults to `"Manual"` |
| `autolinkUsersFromGroups` | `List<String>` | `group.autolinkUsersFromGroups` | — | |
| `autolinkUsersFromPositions` | `List<String>` | `group.autolinkUsersFromPositions` | — | |
| `autolinkTargetAllStructs` | `Boolean` | `group.autolinkTargetAllStructs` | — | |
| `autolinkTargetStructs` | `List<String>` | `group.autolinkTargetStructs` | — | |
| `lockDelete` | `Boolean` | `group.lockDelete` | — | |
| `structureId` | `String` | `structureId` | — | links group to structure on create |
| `classId` | `String` | `classId` | — | links group to class on create |

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

### Mapper naming convention
One mapper class per domain entity, placed in `org.entcore.feeder.dto`:
- `StructureMapper` — structures (done)
- `ClassMapper` — classes
- `UserMapper` — users (complex: profile-dependent)
- `GroupMapper` — groups, functions, head teachers, direction, user-group relations
- `SubjectMapper` — subjects
- `TenantMapper` — tenants

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