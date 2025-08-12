package io.github.chenyilei2016.maintain.client.groovy

import com.alibaba.fastjson.JSON

abstract class BaseConsoleExtService extends Script {

    protected static boolean isNull(final Object cs) {
        return cs == null || "null".equalsIgnoreCase(cs.toString())
    }

    protected static boolean isNotNull(final Object cs) {
        return !isNull(cs)
    }

    protected static boolean isEmpty(final Object cs) {
        if (cs instanceof CharSequence)
            return cs == null || cs.length() == 0;
        else if (cs instanceof Collection)
            return cs == null || cs.isEmpty();
        else if (cs instanceof Map)
            return cs == null || cs.isEmpty();
        else if (cs == null)
            return true
        else if (cs.getClass().isArray())
            return cs == null || java.lang.reflect.Array.getLength(cs) == 0;
        else {
            throw new IllegalArgumentException("Unsupported type: " + cs.getClass().getName());

        }
    }

    protected static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    protected static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    protected static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    protected static String str(Object object) {
        if (String.class.isInstance(object)) {
            return object;
        }
        return "'" + object + "'"
    }

    protected static String toStr(Object object) {
        return str(object)
    }

    protected static String toJson(Object object, Boolean pretty = false) {
        return JSON.toJSONString(object, pretty)
    }

    protected static String toJSON(Object object, Boolean pretty = false) {
        return toJson(object, pretty)
    }

    protected static List<String> str2List(String str, String delimiter) {
        return strToList(str, delimiter)
    }

    //将字符串分割成List, 考虑边界和异常情况
    protected static List<String> strToList(String str, String delimiter) {
        if (str == null || str.isEmpty()) {
            return []
        }
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("Delimiter cannot be null or empty")
        }
        return str.split(delimiter).collect { it.trim() }.findAll { !it.isEmpty() }
    }


}
