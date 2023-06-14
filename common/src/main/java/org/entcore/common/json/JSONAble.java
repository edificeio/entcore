package org.entcore.common.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface JSONAble
{
    // Create a new instance of destClass from a json
    public static JSONAble fromJson(Class<? extends JSONAble> destClass, JsonObject o)
    {
        Constructor emptyConstructor = null;
        try
        {
            emptyConstructor = destClass.getConstructor();
        }
        catch(NoSuchMethodException e)
        {
            Constructor[] ctrs = destClass.getDeclaredConstructors();
            for(int i = 0; i < ctrs.length; ++i)
                if(ctrs[i].getParameterCount() == 0)
                {
                    emptyConstructor = ctrs[i];
                    break;
                }
        }

        Constructor jsonConstructor = null;
        try
        {
            jsonConstructor = destClass.getConstructor(JsonObject.class);
        }
        catch(NoSuchMethodException e) {}

        JSONAble instance = null;
        if(jsonConstructor != null)
        {
            try
            {
                boolean wasAccessible = jsonConstructor.isAccessible();
                jsonConstructor.setAccessible(true);
                instance = (JSONAble) jsonConstructor.newInstance(o);
                jsonConstructor.setAccessible(wasAccessible);
            }
            catch(InvocationTargetException e)
            {
                throw new JSONReflectionException("JsonObject constructor for class " + destClass + " threw an exception", e.getCause());
            }
            catch(Exception e)
            {
                throw new JSONReflectionException("JsonObject constructor for class " + destClass + " failed to create a new instance", e);
            }
        }
        else if(emptyConstructor != null)
        {
            try
            {
                boolean wasAccessible = emptyConstructor.isAccessible();
                emptyConstructor.setAccessible(true);
                instance = (JSONAble) emptyConstructor.newInstance();
                emptyConstructor.setAccessible(wasAccessible);
            }
            catch(InvocationTargetException e)
            {
                throw new JSONReflectionException("Empty constructor for class " + destClass + " threw an exception", e.getCause());
            }
            catch(Exception e)
            {
                throw new JSONReflectionException("Empty constructor for class " + destClass + " failed to create a new instance", e);
            }
            instance.fromJson(o);
        }
        else
            throw new JSONReflectionException("No suitable constructor for class " + destClass + " found (empty or JsonObject)", null);

        return instance;
    }

    // Set this' fields from a json
    public default JSONAble fromJson(JsonObject o)
    {
        if(o == null)
            return this;

        List<Class> jsonAbleClasses = __getJsonAbleClasses(this);

        Class previousClass = null, currentClass = null;
        for(int i = jsonAbleClasses.size(); i-- > 0;)
        {
            previousClass = currentClass;
            currentClass = jsonAbleClasses.get(i);
            List<JSONAnnotatedField> allFields = new LinkedList<JSONAnnotatedField>();
            this.__addInheritedFields(currentClass, allFields, previousClass);

            Field[] cFields = currentClass.getDeclaredFields();
            for(Field f : cFields)
                allFields.add(new JSONAnnotatedField(f));

            for(JSONAnnotatedField annoField : allFields)
            {
                if(annoField.ignore == true)
                    continue;

                Object value = o.getValue(annoField.name);

                if(value == null)
                    value = annoField.defaultValue;

                try
                {
                    boolean wasAccessible = annoField.field.isAccessible();
                    annoField.field.setAccessible(true);
                    annoField.field.set(this, __valueFromJson(annoField.field.getType(), annoField.field.getGenericType(), value, currentClass, annoField.field.getName()));
                    annoField.field.setAccessible(wasAccessible);
                }
                catch(IllegalAccessException e)
                {
                    throw new JSONReflectionException("Cannot set " + currentClass + " field " + annoField.field.getName(), e);
                }
            }
        }

        return this;
    }

    // Turn this into a JsonObject
    public default JsonObject toJson()
    {
        return this.toJson(false);
    }

    public default JsonObject toJson(boolean includeNulls)
    {
        JsonObject json = new JsonObject();
        List<Class> jsonAbleClasses = __getJsonAbleClasses(this);

        Class previousClass = null, currentClass = null;
        for(int i = jsonAbleClasses.size(); i-- > 0;)
        {
            previousClass = currentClass;
            currentClass = jsonAbleClasses.get(i);
            Class c = jsonAbleClasses.get(i);
            List<JSONAnnotatedField> allFields = new LinkedList<JSONAnnotatedField>();
            this.__addInheritedFields(currentClass, allFields, previousClass);

            Field[] cFields = c.getDeclaredFields();
            for(Field f : cFields)
                allFields.add(new JSONAnnotatedField(f));

            for(JSONAnnotatedField annoField : allFields)
            {
                if(annoField.ignore == true)
                    continue;

                Object fValue = null;
                try
                {
                    boolean wasAccessible = annoField.field.isAccessible();
                    annoField.field.setAccessible(true);
                    fValue = annoField.field.get(this);
                    annoField.field.setAccessible(wasAccessible);
                }
                catch(IllegalAccessException e)
                {
                    throw new JSONReflectionException("Cannot read " + currentClass + " field " + annoField.field.getName(), e);
                }

                if(fValue == null)
                    fValue = annoField.defaultValue;

                fValue = __valueToJson(fValue);
                if(fValue == null)
                {
                    if(includeNulls == true)
                        json.putNull(annoField.name);
                }
                else
                    json.put(annoField.name, fValue);
            }
        }

        return json;
    }

    // List all superclasses that are JSONAble
    // TODO: Make private in Java 9
    public default List<Class> __getJsonAbleClasses(JSONAble base)
    {
        Class currentClass = base.getClass();
        List<Class> jsonAbleClasses = new ArrayList<Class>();
        do
        {
            jsonAbleClasses.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        while(JSONAble.class.isAssignableFrom(currentClass));

        return jsonAbleClasses;
    }

    // Converts a value from JSON format into an instance of destClass
    // TODO: Make private in Java 9
    public default Object __valueFromJson(Class destClass, Type declaredType, Object val, Class srcClass, String fieldName)
    {
        if(val == null)
            return null;
        else if(JSONAble.class.isAssignableFrom(destClass))
            return JSONAble.fromJson(destClass, (JsonObject) val);
        else if(JSONTransform.class.isAssignableFrom(destClass))
            return JSONTransform.fromJson(destClass, declaredType, val);
        else if(Enum.class.isAssignableFrom(destClass))
        {
            Enum[] values = (Enum[]) destClass.getEnumConstants();
            for(Enum v : values)
            {
                try
                {
                    Field f = destClass.getField(v.name());
                    JSONRename rename = f.getAnnotation(JSONRename.class);
                    if(rename != null)
                    {
                        if(rename.value().equals((String) val))
                            return v;
                    }
                    else if(v.toString().equals((String) val))
                        return v;
                }
                catch(NoSuchFieldException e) {}
            }
            return Enum.valueOf(destClass, (String) val);
        }
        else if(Collection.class.isAssignableFrom(destClass))
        {
            destClass = __defaultCollection(destClass);
            Collection newCollection = null;
            try
            {
                newCollection = (Collection) destClass.newInstance();
            }
            catch(Exception e)
            {
                throw new JSONReflectionException("Cannot create new Collection instance for " + destClass + " in " + srcClass + " field " + fieldName, e);
            }

            Class genericTypeClass = null;
            Type genericType = null;
            if(declaredType instanceof ParameterizedType)
            {
                ParameterizedType paramType = (ParameterizedType) declaredType;
                genericType = paramType.getActualTypeArguments()[0];
                try
                {
                    String genericTypeName = genericType.getTypeName();
                    int nextGenericIx = genericTypeName.indexOf("<");
                    genericTypeClass = Class.forName(genericTypeName.substring(0, nextGenericIx == -1 ? genericTypeName.length() : nextGenericIx));
                }
                catch(Exception e)
                {
                    throw new JSONReflectionException("Cannot get Collection generic type for " + destClass + " in " + srcClass + " field " + fieldName, e);
                }
            }

            for(Object o : (JsonArray) val)
                newCollection.add(__valueFromJson(genericTypeClass != null ? genericTypeClass : o.getClass(), genericType, o, destClass, fieldName + ".inner"));

            return newCollection;
        }
        else if(String.class.isAssignableFrom(destClass))
            return val.toString();
        else if(Number.class.isAssignableFrom(destClass))
        {
            try
            {
                return destClass.getConstructor(String.class).newInstance(val.toString());
            }
            catch(InvocationTargetException e)
            {
                throw new JSONReflectionException("New Number instance threw for " + destClass + " in " + srcClass + " field " + fieldName, e);
            }
            catch(Exception e)
            {
                throw new JSONReflectionException("Cannot create new Number instance for " + destClass + " in " + srcClass + " field " + fieldName, e);
            }
        }
        else
        {
            Constructor valueConstructor = null;
            try
            {
                valueConstructor = destClass.getConstructor(val.getClass());
            }
            catch (NoSuchMethodException e) {}

            try
            {
                return valueConstructor != null ? valueConstructor.newInstance(val) : val;
            }
            catch(InvocationTargetException e)
            {
                throw new JSONReflectionException("New default instance threw for " + destClass + " in " + srcClass + " field " + fieldName, e);
            }
            catch(Exception e)
            {
                throw new JSONReflectionException("Cannot create new default instance for " + destClass + " in " + srcClass + " field " + fieldName, e);
            }
        }
    }

    // Converts a value into JSON format
    // TODO: Make private in Java 9
    public default Object __valueToJson(Object val)
    {
        if(val == null)
            return null;
        else if(val instanceof JsonObject || val instanceof JsonArray)
            return val;
        else if(val instanceof JSONAble)
            return ((JSONAble) val).toJson();
        else if(val instanceof JSONTransform)
            return ((JSONTransform) val).toJson();
        else if(val instanceof Enum)
        {
            try
            {
                Field valueField = val.getClass().getField(((Enum) val).name());
                JSONRename rename  = valueField.getAnnotation(JSONRename.class);
                return rename != null ? rename.value() : ((Enum) val).toString();
            }
            catch(NoSuchFieldException e)
            {
                return ((Enum) val).toString();
            }
        }
        else if(val instanceof Iterable)
        {
            JsonArray a = new JsonArray();
            for(Object o : (Iterable) val)
                a.add(__valueToJson(o));
            return a;
        }
        else if(val instanceof Number)
            return val;
        else if(val instanceof Boolean)
            return val;
        else
            return val.toString();
    }

    // Converts a Collection class into an instantiable default class
    // TODO: Make private in Java 9
    public default Class<? extends Collection> __defaultCollection(Class<? extends Collection> declaredClass)
    {
        if(List.class.equals(declaredClass))
            return ArrayList.class;
        else if (Set.class.equals(declaredClass))
            return HashSet.class;
        else
            return declaredClass;
    }

    public default void __addInheritedFields(Class<? extends JSONAble> c, List<JSONAnnotatedField> allFields, Class<? extends JSONAble> srcClass)
    {
        JSONInherit[] inheritedFields = (JSONInherit[]) c.getAnnotationsByType(JSONInherit.class);
        for(JSONInherit inherit : inheritedFields)
        {
            try
            {
                Field field = c.getField(inherit.field());
                allFields.add(new JSONAnnotatedField(field, false, inherit.rename(), inherit.defaultValue()));
            }
            catch(NoSuchFieldException e)
            {
                throw new JSONReflectionException("Cannot find field " + inherit.field() + " on class " + c + " required by class " + srcClass, e);
            }
        }
    }
}
