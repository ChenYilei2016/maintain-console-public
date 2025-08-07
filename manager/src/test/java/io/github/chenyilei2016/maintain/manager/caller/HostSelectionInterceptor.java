package io.github.chenyilei2016.maintain.manager.caller;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;

import java.io.IOException;

/**
 * @author chenyilei
 * @since 2024/05/21 10:00
 */
public class HostSelectionInterceptor implements Interceptor {
    public static final String HEADER_BASE_URL = "HEADER_BASE_URL";
    public static final String DUMMY_URL = "http://byreplace.com/";

    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String headerBaseUrl = request.header(HEADER_BASE_URL);
        if (headerBaseUrl != null) {
            HttpUrl newUrl = HttpUrl.get(request.url().toString().replace(DUMMY_URL, headerBaseUrl));
            request = request.newBuilder().url(newUrl).build();
        }
        return chain.proceed(request);
    }
}
