# Feeder DTO Migration — Skills Reference

How to migrate a `manual-*` action from raw `Message<JsonObject>` to typed DTOs.

---

## Step 1 — Create the DTO

Create `feeder/src/main/java/org/entcore/feeder/dto/XxxDTO.java`.

```java
package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class XxxDTO {

    private String fieldName;   // one entry per JSON field

    public XxxDTO() {}

    public XxxDTO(JsonObject json) {
        XxxDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        XxxDTOConverter.toJson(this, json);
        return json;
    }

    public String getFieldName() { return fieldName; }
    public XxxDTO setFieldName(String fieldName) { this.fieldName = fieldName; return this; }
}
```

**Rules:**
- `@DataObject` + `@JsonGen` together trigger vertx-codegen to generate `XxxDTOConverter` at compile time.
- `@JsonInclude(NON_NULL)` keeps null fields out of serialized JSON.
- Setters must return `this` (fluent).
- The `package-info.java` with `@ModuleGen` is already in place — no changes needed there.

### Type mapping

| JSON type | Java field type |
|---|---|
| string | `String` |
| number (int) | `int` / `Integer` |
| boolean | `boolean` / `Boolean` |
| array of strings | `List<String>` |
| nested object | `JsonObject` (or a nested DTO) |

---

## Step 2 — Refactor the ManualFeeder method

Replace the old `executeTransaction(message, ...)` body with the new pattern.

**Before:**
```java
public void doSomething(Message<JsonObject> message) {
    final String fieldName = getMandatoryString("fieldName", message);
    executeTransaction(message, new VoidFunction<TransactionHelper>() {
        @Override
        public void apply(TransactionHelper tx) throws ValidationException {
            SomeClass.someMethod(fieldName, tx);
        }
    });
}
```

**After:**
```java
public void doSomething(final XxxDTO dto, final Handler<JsonObject> replyHandler) {
    final String fieldName = dto.getFieldName();
    if (fieldName == null) {
        replyHandler.handle(new JsonObject().put("status", "error").put("message", "fieldName must be specified"));
        return;
    }
    try {
        TransactionHelper tx = TransactionManager.getInstance().begin((Integer) null);
        tx.setAutoSend(true);
        SomeClass.someMethod(fieldName, tx);
        tx.commit(m -> replyHandler.handle(m.body()));
    } catch (TransactionException e) {
        logger.error("Error in transaction when doing something", e);
        replyHandler.handle(new JsonObject().put("status", "error").put("message", e.getMessage()));
    }
}
```

**Rules:**
- One null-check + early return per required field.
- `TransactionManager.getInstance().begin((Integer) null)` opens a new transaction.
- `tx.setAutoSend(true)` makes the transaction send statements immediately.
- `tx.commit(m -> replyHandler.handle(m.body()))` wires the response.

### List<String> → JsonArray

When a Neo4j method expects `JsonArray` but the DTO holds `List<String>`:
```java
new JsonArray(dto.getUserIds())
```

---

## Step 3 — Add DTO import to ManualFeeder.java

```java
import org.entcore.feeder.dto.XxxDTO;
```

---

## Step 4 — Update Feeder.java dispatch

Add import:
```java
import org.entcore.feeder.dto.XxxDTO;
```

Update the switch case:
```java
// Before
case "manual-action-name" : manual.doSomething(message);
    break;

// After
case "manual-action-name" : manual.doSomething(new XxxDTO(message.body()), message::reply);
    break;
```

---

## Step 5 — Update migration-plan.md

Mark the action row as `✅ | ✅ | ✅` in the status table.

---

## When a mapper is needed

Most simple actions don't need a mapper — `new XxxDTO(message.body())` is enough.

A **mapper** is only needed when the incoming JSON has a non-flat structure that the generated converter can't handle directly:

| Case | Solution |
|---|---|
| Fields nested under a `data` sub-object | Hand-written mapper: `body.getJsonObject("data")` |
| JSON key differs from DTO field name (e.g. `UAI` → `uai`) | Hand-written mapper with explicit key rename |
| Multiple fields aggregated from different sub-objects | Hand-written mapper |

Mappers live in `org.entcore.feeder.mapper` (`feeder/src/main/java/org/entcore/feeder/mapper/`).

---

## Package structure

```
org.entcore.feeder.dto/
  package-info.java          ← @ModuleGen (do not edit)
  XxxDTO.java                ← @DataObject + @JsonGen
  XxxDTOConverter.java       ← generated at compile time (do not edit)

org.entcore.feeder.mapper/
  StructureMapper.java       ← hand-written, handles nested `data` + UAI rename
  ClassMapper.java           ← hand-written, handles nested `data`
  FunctionMapper.java        ← hand-written, handles nested `data`
```