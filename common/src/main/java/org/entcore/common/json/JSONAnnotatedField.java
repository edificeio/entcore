package org.entcore.common.json;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.ParseException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JSONAnnotatedField
{
    public Field field;

    public boolean ignore;
    public String name;
    public Object defaultValue;

    public JSONAnnotatedField(Field field)
    {
        this(field, field.getAnnotation(JSONIgnore.class), field.getAnnotation(JSONRename.class), field.getAnnotation(JSONDefault.class));
    }

    public JSONAnnotatedField(Field field, JSONIgnore ignoreA, JSONRename renameA, JSONDefault defaultA)
    {
        this(field, ignoreA != null, renameA == null ? null : renameA.value(), defaultA == null ? null : defaultA.value());
    }

    public JSONAnnotatedField(Field field, boolean ignore, String rename, String defaultValue)
    {
        this.field = field;
        this.ignore = ignore;

        if(rename != null && "".equals(rename) == false)
            this.name = rename;
        else
            this.name = field.getName();

        this.defaultValue = this.getDefault(defaultValue);
    }

    private Object getDefault(String defVal)
    {
        if(defVal == null)
            return null;
        else if(defVal.startsWith("{"))
            return new JsonObject(defVal);
        else if(defVal.startsWith("["))
            return new JsonArray(defVal);
        else if("true".equals(defVal))
            return true;
        else if("false".equals(defVal))
            return false;
        else if("null".equals(defVal))
            return null;
        else
        {
            try
            {
                return NumberFormat.getInstance().parse(defVal);
            }
            catch (ParseException e)
            {
                return defVal;
            }
        }
    }
}
