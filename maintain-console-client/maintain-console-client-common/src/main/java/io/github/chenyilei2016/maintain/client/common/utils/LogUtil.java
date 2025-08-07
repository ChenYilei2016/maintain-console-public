package io.github.chenyilei2016.maintain.client.common.utils;

import org.slf4j.Logger;

/**
 * @author chenyilei
 * @since 2024/05/16 19:23
 */
public class LogUtil {

    private static final String LOG_PREFIX = "[maintain-console] ";

    static String withPackage(String originMsg) {
        if (null == originMsg) {
            return null;
        }
        if (originMsg.startsWith(LOG_PREFIX)) {
            return originMsg;
        }
        return LOG_PREFIX + originMsg;
    }

    public static void trace(Logger logger, String msg, Object... params) {
        logger.trace(withPackage(msg), params);
    }

    public static void info(Logger logger, String msg, Object... params) {
        logger.info(withPackage(msg), params);
    }

    public static void warn(Logger logger, String msg, Object... params) {
        logger.warn(withPackage(msg), params);
    }

    public static void error(Logger logger, String msg, Object... params) {
        logger.error(withPackage(msg), params);
    }


}
