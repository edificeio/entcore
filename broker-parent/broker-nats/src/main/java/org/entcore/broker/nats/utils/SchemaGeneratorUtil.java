package org.entcore.broker.nats.utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

public class SchemaGeneratorUtil {

  public SchemaGeneratorUtil() {

  }


  public List<Map<String, Object>> compact(final List<Map<String, Object>> exportedSchemas) {
    final Map<String, Map<String, Object>> compacted = new HashMap<>();
    return compact(exportedSchemas, compacted);
  }

  private List<Map<String, Object>> compact(final List<Map<String, Object>> exportedSchemas, final Map<String, Map<String, Object>> compacted) {
    for (Map<String, Object> exportedSchema : exportedSchemas) {
      compact(exportedSchema, compacted);
    }
    return compacted.values().stream().collect(Collectors.toList());
  }
  public void compact(final Map<String, Object> schema, final Map<String, Map<String, Object>> compacted) {
    final String title = (String) schema.getOrDefault("title", "");
    if (title.isEmpty()) {
      if("object".equals(schema.getOrDefault("type", ""))) {
        Object props = schema.getOrDefault("properties", null);
        if(props instanceof Map) {
          final Map properties = (Map) props;
          final Set<String> keys = properties.keySet();
          for (String key : keys) {
            final Object field = properties.getOrDefault(key, null);
            compact((Map)field, compacted);
          }
        }
        Object addProps = schema.getOrDefault("additionalProperties", null);
        if(addProps instanceof Map) {
          final Map additionalProperties = (Map) addProps;
          final String titleAddPros = (String)additionalProperties.getOrDefault("title", null);
          if(titleAddPros != null) {
            compact(additionalProperties, compacted);
          }
        }
      }
    } else if (compacted.containsKey(title)) {
      // Already seen so nothing to do
    } else {
      compacted.put(title, schema);
      final Map<String, Map<String, Object>> references = new HashMap<>();
      getReferences(schema, references);
      clearReferences(schema);
      List<Map<String, Object>> uniqReferences = references.entrySet().stream()
        .filter(entry -> !compacted.containsKey(entry.getKey()))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
      compact(uniqReferences, compacted);
    }
  }

  /**
   * Generates a JSON schema from a TypeMirror.
   *
   * @param typeMirror The TypeMirror to generate the schema for.
   * @return A JSON schema as a string.
   */
  public Map<String, Object> generateJsonSchemaFromTypeMirror(TypeMirror typeMirror) {
    System.out.println("Generating schema for " + typeMirror.toString() + " : " + typeMirror.getKind());
    Map<String, Object> schema = new LinkedHashMap<>(); // Use LinkedHashMap here just to keep the order of the fields
    final String typeMirrorAsString = typeMirror.toString();
    switch (typeMirror.getKind()) {
      case BOOLEAN:
        schema.put("type", "boolean");
        break;
      case BYTE:
      case SHORT:
      case INT:
      case LONG:
        schema.put("type", "integer");
        break;
      case FLOAT:
      case DOUBLE:
        schema.put("type", "number");
        break;
      case CHAR:
        schema.put("type", "string");
        break;
      case ARRAY:
        schema.put("type", "array");
        ArrayType arrayType = (ArrayType) typeMirror;
        schema.put("items", generateJsonSchemaFromTypeMirror(arrayType.getComponentType()));
        break;
      case DECLARED:
        if ("java.lang.String".equals(typeMirrorAsString)) {
          schema.put("type", "string");
        } else if ("java.lang.Object".equals(typeMirrorAsString)) {
          schema.put("type", "object");
        } else if (isArrayLike(typeMirror)) {
          schema.put("type", "array");
          schema.put("items", getElementTypeOfArrayLike(typeMirror));
        } else if (isMapLike(typeMirror)) {
          schema.put("type", "object");
          schema.put("additionalProperties", getElementTypeOfMap(typeMirror));
        } else if(typeMirrorAsString.startsWith("io.vertx.core.Future<"))  {
          schema.putAll(getElementTypeOfFuture(typeMirror));
        } else {
          // Handle declared types (records or classes)
          final String customType = typeMirrorAsString;
          schema.put("$id", customType);
          schema.put("type", "object");
          schema.put("title", customType);

          // Extract properties for records or classes
          Map<String, Object> properties = new LinkedHashMap<>(); // Use LinkedHashMap here just to keep the order of the fields
          TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();

          if (typeElement.getKind() == ElementKind.CLASS) {
            // Handle classes
            for (Element enclosedElement : typeElement.getEnclosedElements()) {
              if (enclosedElement.getKind() == ElementKind.FIELD) {
                properties.put(enclosedElement.getSimpleName().toString(),
                  generateJsonSchemaFromTypeMirror(enclosedElement.asType()));
              }
            }
          }
          schema.put("properties", properties);
        }
        break;
      default:
        schema.put("type", "unknown");
        break;
    }
    return schema;
  }

  private Object getElementTypeOfMap(TypeMirror typeMirror) {
    final List<? extends TypeMirror> typeArgs = ((DeclaredType) typeMirror).getTypeArguments();
    if (typeArgs == null || typeArgs.isEmpty()) {
      throw new RuntimeException("Cannot serialize a schema with an unparametrized Map : " + typeMirror);
    }
    if (!typeArgs.get(0).toString().equals(String.class.getCanonicalName())) {
      throw new RuntimeException("Cannot serialize a schema with a Map whose keys are not String instances : " + typeMirror);
    }
    return generateJsonSchemaFromTypeMirror(typeArgs.get(typeArgs.size() - 1));
  }

  private Object getElementTypeOfArrayLike(TypeMirror typeMirror) {
    final List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
    if (typeArguments == null || typeArguments.isEmpty()) {
      return of("type", "object");
    } else {
      return generateJsonSchemaFromTypeMirror(typeArguments.get(0));
    }
  }

  private Map<String, Object> getElementTypeOfFuture(TypeMirror typeMirror) {
    final List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
    if (typeArguments == null || typeArguments.isEmpty()) {
      return of("type", "object");
    } else {
      return generateJsonSchemaFromTypeMirror(typeArguments.get(0));
    }
  }

  private boolean isMapLike(TypeMirror typeMirror) {
    final String type = typeMirror.toString();
    return type.startsWith("java.util.Map");
  }

  private boolean isArrayLike(TypeMirror typeMirror) {
    final String type = typeMirror.toString();
    return type.startsWith("java.util.Set") ||
      type.startsWith("java.util.List") ||
      type.startsWith("java.util.Collection");
  }

  private void clearReferences(Map<String, Object> exportedSchema) {
    Object props = exportedSchema.getOrDefault("properties", null);
    if(props instanceof Map) {
      final Map properties = (Map) props;
      final Set<String> keys = properties.keySet();
      for (String key : keys) {
        final Object field = properties.getOrDefault(key, null);
        if(field instanceof Map) {
          final Map fieldMap = (Map) field;
          final String title = (String) fieldMap.getOrDefault("title", "");
          if(title.isEmpty()) {
            clearReferences((Map<String, Object>) field);
          } else {
            properties.put(key, of("$ref", /*"#/definitions/" + */title));
          }
        }
      }
    }
    Object addProps = exportedSchema.getOrDefault("additionalProperties", null);
    if(addProps instanceof Map) {
      final Map additionalProperties = (Map) addProps;
      final Object addPropType = additionalProperties.getOrDefault("type", null);
      if(addPropType != null && addPropType instanceof Map && ((Map)addPropType).containsKey("title")) {
        Map additionalPropertyType = (Map) addPropType;
        final String titleOfMapValueType = (String)additionalPropertyType.get("title");
        exportedSchema.put("additionalProperties", of("$ref", /*"#/definitions/" + */titleOfMapValueType));
      }
    }
  }

  private void getReferences(Map<String, Object> exportedSchema,
                                                  final Map<String, Map<String, Object>> references) {
    Object props = exportedSchema.getOrDefault("properties", null);
    if(props instanceof Map) {
      final Map<String, Object> properties = (Map<String, Object>) props;
      final Set<String> keys = properties.keySet();
      for (String key : keys) {
        final Object field = properties.getOrDefault(key, null);
        if(field instanceof Map) {
          final Map fieldMap = (Map) field;
          final String title = (String) fieldMap.get("title");
          if(title == null) {
            if("object".equals(fieldMap.get("type"))) {
              getReferences(fieldMap, references);
            }
          } else if(!references.containsKey(title)) {
            references.put(title, fieldMap);
          }
        }
      }
    }
  }

  final Map<String, Object> of(final String key, final Object value) {
    final Map<String, Object> map = new HashMap<>();
    map.put(key, value);
    return map;
  }
}