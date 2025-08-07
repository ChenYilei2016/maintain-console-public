package cn.chenyilei.maintain.manager.utils;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;

import java.util.List;

/**
 * @author chenyilei
 * @date 2022/08/25 14:13
 */
public class StrUtils {
    private static final Cache<String, String> camel2UpperUnderLineCachedMap = CacheBuilder.newBuilder().maximumSize(2000).build();

    private static final Cache<String, String> upperUnderLine2CamelCachedMap = CacheBuilder.newBuilder().maximumSize(2000).build();

    public static Joiner commaJoiner = Joiner.on(",").skipNulls();

    public static Splitter commaSplitter = Splitter.on(",").omitEmptyStrings().trimResults();

    public static Splitter hashSplitter = Splitter.on("#").omitEmptyStrings().trimResults();


    /**
     * 大写下划线 -> 小写驼峰
     */
    @SneakyThrows
    public static String upperUnderLine2CamelCached(String str) {
        if (null == str) {
            return null;
        }
        return upperUnderLine2CamelCachedMap.get(str, () -> {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str);
        });
    }

    public static void upperUnderLine2CamelCached(List<String> strList) {
        if (strList == null) {
            return;
        }
        for (int i = 0; i < strList.size(); i++) {
            strList.set(i, StrUtils.upperUnderLine2CamelCached(strList.get(i)));
        }
    }

}
