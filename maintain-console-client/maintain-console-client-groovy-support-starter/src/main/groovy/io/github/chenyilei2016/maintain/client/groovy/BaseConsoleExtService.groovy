package io.github.chenyilei2016.maintain.client.groovy

import com.alibaba.fastjson.JSON

abstract class BaseConsoleExtService extends Script {

    public def a = "";

    public def b = 'ddd'

    public def c = """  
666
6  """

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
