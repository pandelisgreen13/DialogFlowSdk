package gr.padpad.dialogflow;

import ai.api.android.AIService;
import ai.api.model.Result;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import androidx.appcompat.widget.AppCompatTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gr.padpad.dialogflow.retrofit.client.RetrofitClient;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R2.id.listenTextView)
    AppCompatTextView listenTextView;

    @BindView(R2.id.responseTextView)
    AppCompatTextView responseTextView;

    @OnClick(R2.id.listenButton)
    void listen() {
        aiService.startListening();
    }

    private AIService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RetrofitClient.initRetrofit();

        final AIConfiguration config = new AIConfiguration("",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(new AIListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResult(AIResponse result) {
                Result currentResult = result.getResult();
                listenTextView.setText(currentResult.getResolvedQuery() + "Action: " + currentResult.getAction());
                Timber.d("DialogFlow -->" + result.getResult().getResolvedQuery());
                aiService.stopListening();
            }

            @Override
            public void onError(AIError error) {

            }

            @Override
            public void onAudioLevel(float level) {

            }

            @Override
            public void onListeningStarted() {

            }

            @Override
            public void onListeningCanceled() {

            }

            @Override
            public void onListeningFinished() {

            }
        });
    }


}
