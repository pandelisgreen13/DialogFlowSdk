package gr.padpad.dialogflow.retrofit.interceptor;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInjectInterceptor implements Interceptor {

    private Map<String, String> headerMaps;

    public HeaderInjectInterceptor(Map<String, String> headerMaps) {
        this.headerMaps = headerMaps != null ? new HashMap<>(headerMaps) : new HashMap<String, String>();
    }

    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        for (Map.Entry<String, String> entry : headerMaps.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        request = builder.build();
        return chain.proceed(request);
    }
}
