package io.github.chenyilei2016.maintain.manager.caller.http;

import com.alibaba.fastjson.support.retrofit.Retrofit2ConverterFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * @author chenyilei
 * @since 2024/05/21 10:13
 */
public class RetrofitHttpProxyFactory {
    static OkHttpClient commonDefaultClient = new OkHttpClient.Builder()
            .addInterceptor(new OkHttpUrlSelectionInterceptor())
            .callTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build();

    /**
     * default fast json
     */
    public static <T> T getProxy(Class<T> proxyClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OkHttpUrlSelectionInterceptor.DUMMY_URL)
                .callFactory(commonDefaultClient)
                .addConverterFactory(new ToStringConverterFactory()) //处理text/html
                .addConverterFactory(Retrofit2ConverterFactory.create()) //这个json的
                .build();
        return retrofit.create(proxyClass);
    }

    public static class ToStringConverterFactory extends Converter.Factory {
        static final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                                Retrofit retrofit) {
            if (String.class.equals(type)) {
                return new Converter<ResponseBody, String>() {
                    @Override
                    public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                };
            }
            return null;
        }

        @Override
        public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            if (String.class.equals(type)) {
                return (Converter<String, RequestBody>) value -> RequestBody.create(MEDIA_TYPE, value);
            }
            return null;
        }
    }

}
