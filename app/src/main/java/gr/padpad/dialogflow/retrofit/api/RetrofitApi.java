package gr.padpad.dialogflow.retrofit.api;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface RetrofitApi {

    @Streaming
    @GET("https://translate.google.com/translate_tts")
    Observable<Response<ResponseBody>> getTTS(@Query(value = "ie", encoded = true) String ie, @Query("tl") String language, @Query("client") String client, @Query("q") String text);

}
