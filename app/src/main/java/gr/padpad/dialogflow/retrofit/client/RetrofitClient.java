package gr.padpad.dialogflow.retrofit.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import gr.padpad.dialogflow.BuildConfig;
import gr.padpad.dialogflow.retrofit.api.RetrofitApi;
import gr.padpad.dialogflow.retrofit.error.RxErrorHandlingCallAdapterFactory;
import gr.padpad.dialogflow.retrofit.interceptor.HeaderInjectInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static RetrofitApi retrofitApi;

    public static void initRetrofit() {

        if (retrofitApi == null) {
            Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
            retrofitBuilder.baseUrl("https://dialogflow.googleapis.com/");
            retrofitBuilder.client(getOkHttpClientBuilder().build());
            retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
            retrofitBuilder.addCallAdapterFactory(RxErrorHandlingCallAdapterFactory.create());
            Retrofit retrofit = retrofitBuilder.build();
            retrofitApi = retrofit.create(RetrofitApi.class);
        }

    }

    @NonNull
    private static OkHttpClient.Builder getOkHttpClientBuilder() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.connectTimeout(120, TimeUnit.SECONDS);
        httpClientBuilder.readTimeout(120, TimeUnit.SECONDS);
        httpClientBuilder.addInterceptor((new HttpLoggingInterceptor()).setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE));
        httpClientBuilder.addInterceptor(new HeaderInjectInterceptor(getInjectableHeaders()));
        return httpClientBuilder;
    }

    private static Map<String, String> getInjectableHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Bearer", "");
        headers.put("Contenty-Type", "application/json");
        return headers;
    }

}
