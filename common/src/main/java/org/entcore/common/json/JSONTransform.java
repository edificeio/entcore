package org.entcore.common.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface JSONTransform
{
    public Object toJson();
    public default void fromJson(Object jsonValue)
    {
        this.fromJson(jsonValue, this.getClass());
    }
    public void fromJson(Object jsonValue, Type declaredType);

    // Create a new instance of destClass from a json
    //TODO: Look for a Constructor that accepts one of value's classes
    public static JSONTransform fromJson(Class<? extends JSONTransform> destClass, Type declaredType, Object value)
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

        JSONTransform instance = null;
        if(emptyConstructor != null)
        {
            try
            {
                boolean wasAccessible = emptyConstructor.isAccessible();
                emptyConstructor.setAccessible(true);
                instance = (JSONTransform) emptyConstructor.newInstance();
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
        }
        else
            throw new JSONReflectionException("No suitable constructor for class " + destClass + " found (empty)", null);

        instance.fromJson(value, declaredType);

        return instance;
    }

    public default Object __convertGeneric(Object jsonValue, ParameterizedType declaredType)
    {
        return __convertGeneric(jsonValue, declaredType, 0);
    }

    public default Object __convertGeneric(Object jsonValue, ParameterizedType declaredType, int genericIndex)
    {
        Type[] generics = declaredType.getActualTypeArguments();
        if(generics.length == 0)
            return jsonValue;
        else
        {
            Class tClass;
            try
            {
                tClass = Class.forName(generics[genericIndex].getTypeName());
            }
            catch (ClassNotFoundException e)
            {
                throw new JSONReflectionException("No class " + generics[genericIndex].getTypeName() + " found for generic type " + declaredType.getTypeName(), null);
            }

            if(tClass.isInstance(jsonValue))
                return tClass.cast(jsonValue);
            else
            {
                Constructor objectConstructor = null;
                Constructor stringConstructor = null;
                try
                {
                    objectConstructor = tClass.getConstructor(jsonValue.getClass());
                }
                catch(NoSuchMethodException e) {}
                try
                {
                    stringConstructor = tClass.getConstructor(String.class);
                }
                catch(NoSuchMethodException e) {}

                Object converted = jsonValue;
                if(objectConstructor != null)
                {
                    try
                    {
                        boolean wasAccessible = objectConstructor.isAccessible();
                        objectConstructor.setAccessible(true);
                        converted = objectConstructor.newInstance(jsonValue);
                        objectConstructor.setAccessible(wasAccessible);
                    }
                    catch(InvocationTargetException e)
                    {
                        throw new JSONReflectionException(jsonValue.getClass() + " constructor for class " + tClass + " threw an exception", e.getCause());
                    }
                    catch(Exception e)
                    {
                        throw new JSONReflectionException(jsonValue.getClass() + " constructor for class " + tClass + " failed to create a new instance", e);
                    }
                }
                else if(stringConstructor != null)
                {
                    try
                    {
                        boolean wasAccessible = stringConstructor.isAccessible();
                        stringConstructor.setAccessible(true);
                        converted = stringConstructor.newInstance(jsonValue.toString());
                        stringConstructor.setAccessible(wasAccessible);
                    }
                    catch(InvocationTargetException e)
                    {
                        throw new JSONReflectionException("String constructor for class " + tClass + " threw an exception", e.getCause());
                    }
                    catch(Exception e)
                    {
                        throw new JSONReflectionException("String constructor for class " + tClass + " failed to create a new instance", e);
                    }
                }
                return converted;
            }
        }
    }
}
