package org.entcore.broker.nats.dummy;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AllTypeClass {
  private final String stringField;
  private final int intField;
  private final boolean booleanField;
  private final double doubleField;
  private final long longField;
  private final float floatField;
  private final Object objectField;
  private final String[] stringArrayField;
  private final int[] intArrayField;
  private final boolean[] booleanArrayField;
  private final double[] doubleArrayField;
  private final long[] longArrayField;
  private final float[] floatArrayField;
  private final List<String> stringListField;
  private final List<Integer> intListField;
  private final List<Boolean> booleanListField;
  private final List<Double> doubleListField;
  private final List<Long> longListField;
  private final List<Float> floatListField;
  private final List<Object> objectListField;
  private final SimpleNestedClass nestedClassField;
  private final List<SimpleNestedClass> nestedClassListField;
  private final Set<SimpleNestedClass> nestedClassSetField;
  private final Map<String, SimpleNestedClass> nestedClassMapField;

  public AllTypeClass(String stringField, int intField, boolean booleanField, double doubleField, long longField, float floatField, Object objectField, String[] stringArrayField, int[] intArrayField, boolean[] booleanArrayField, double[] doubleArrayField, long[] longArrayField, float[] floatArrayField, List<String> stringListField, List<Integer> intListField, List<Boolean> booleanListField, List<Double> doubleListField, List<Long> longListField, List<Float> floatListField, List<Object> objectListField, SimpleNestedClass nestedClassField, List<SimpleNestedClass> nestedClassListField, Set<SimpleNestedClass> nestedClassSetField, Map<String, SimpleNestedClass> nestedClassMapField) {
    this.stringField = stringField;
    this.intField = intField;
    this.booleanField = booleanField;
    this.doubleField = doubleField;
    this.longField = longField;
    this.floatField = floatField;
    this.objectField = objectField;
    this.stringArrayField = stringArrayField;
    this.intArrayField = intArrayField;
    this.booleanArrayField = booleanArrayField;
    this.doubleArrayField = doubleArrayField;
    this.longArrayField = longArrayField;
    this.floatArrayField = floatArrayField;
    this.stringListField = stringListField;
    this.intListField = intListField;
    this.booleanListField = booleanListField;
    this.doubleListField = doubleListField;
    this.longListField = longListField;
    this.floatListField = floatListField;
    this.objectListField = objectListField;
    this.nestedClassField = nestedClassField;
    this.nestedClassListField = nestedClassListField;
    this.nestedClassSetField = nestedClassSetField;
    this.nestedClassMapField = nestedClassMapField;
  }

  public String getStringField() {
    return stringField;
  }

  public int getIntField() {
    return intField;
  }

  public boolean isBooleanField() {
    return booleanField;
  }

  public double getDoubleField() {
    return doubleField;
  }

  public long getLongField() {
    return longField;
  }

  public float getFloatField() {
    return floatField;
  }

  public Object getObjectField() {
    return objectField;
  }

  public String[] getStringArrayField() {
    return stringArrayField;
  }

  public int[] getIntArrayField() {
    return intArrayField;
  }

  public boolean[] getBooleanArrayField() {
    return booleanArrayField;
  }

  public double[] getDoubleArrayField() {
    return doubleArrayField;
  }

  public long[] getLongArrayField() {
    return longArrayField;
  }

  public float[] getFloatArrayField() {
    return floatArrayField;
  }

  public List<String> getStringListField() {
    return stringListField;
  }

  public List<Integer> getIntListField() {
    return intListField;
  }

  public List<Boolean> getBooleanListField() {
    return booleanListField;
  }

  public List<Double> getDoubleListField() {
    return doubleListField;
  }

  public List<Long> getLongListField() {
    return longListField;
  }

  public List<Float> getFloatListField() {
    return floatListField;
  }

  public List<Object> getObjectListField() {
    return objectListField;
  }

  public SimpleNestedClass getNestedClassField() {
    return nestedClassField;
  }

  public List<SimpleNestedClass> getNestedClassListField() {
    return nestedClassListField;
  }

  public Set<SimpleNestedClass> getNestedClassSetField() {
    return nestedClassSetField;
  }

  public Map<String, SimpleNestedClass> getNestedClassMapField() {
    return nestedClassMapField;
  }
}
