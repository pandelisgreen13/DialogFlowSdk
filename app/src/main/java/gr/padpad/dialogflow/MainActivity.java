package gr.padpad.dialogflow;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Value;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gr.padpad.dialogflow.model.RequestQuery;
import gr.padpad.dialogflow.retrofit.client.RetrofitClient;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_SPEECH_RECOGNIZER = 3000;

    @BindView(R2.id.listenTextView)
    AppCompatTextView listenTextView;

    @BindView(R2.id.responseTextView)
    AppCompatTextView responseTextView;

    @BindView(R2.id.typeEdiText)
    AppCompatEditText typeEdiText;

    @BindView(R2.id.progressBar)
    ProgressBar progressBar;

    @OnClick(R2.id.wrikeButton)
    void type() {
        sendMessage(Objects.requireNonNull(typeEdiText.getText()).toString());
    }

    @OnClick(R2.id.listenButton)
    void listen() { startSpeechRecognizer(); }

    private SessionsClient sessionsClient;
    private SessionName session;
    private String uuid = UUID.randomUUID().toString();
    private TextToSpeech textToSpeech;
    private String mAnswer = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RetrofitClient.initRetrofit();
        initChatbot();
        initTTS();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SPEECH_RECOGNIZER) {
            if (resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mAnswer = results.get(0);
                requestQueryMessage(mAnswer);
                responseTextView.setText(mAnswer);
            }
        }
    }

    private void initTTS() {
        textToSpeech("Hello i am the Cortana, how can i help you?", false);
    }

    private void initChatbot() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(getResources().openRawResource(R.raw.test_agent_credentials));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER);
    }

    private void sendMessage(String msg) {
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your requestQueryMessage!", Toast.LENGTH_LONG).show();
        } else {
            Timber.d("Bot message: %s", msg);
            typeEdiText.setText("");
            requestQueryMessage(msg);
        }
    }

    private void requestQueryMessage(String msg) {
        progressBar.setVisibility(View.VISIBLE);
        QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
        new RequestQuery(MainActivity.this, session, sessionsClient, queryInput).execute();
    }

    public void getResponse(DetectIntentResponse response) {
        progressBar.setVisibility(View.GONE);
        if (response != null) {
            String botReply = response.getQueryResult().getFulfillmentText();
            Timber.d("Bot Reply: %s", botReply);
            Map<String, Value> fields = response.getQueryResult().getParameters().getFields();
            textToSpeech(botReply, response.getQueryResult().getAllRequiredParamsPresent());
            responseTextView.setText(botReply);
        } else {
            Timber.d("Bot Reply: Null");
            responseTextView.setText("There was some communication issue. Please Try again!");
        }
    }

    private void textToSpeech(String msg, boolean allRequiredParamsPresent) {
        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.UK);
                textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "String id");
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (!allRequiredParamsPresent)
                            startSpeechRecognizer();
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }
                });
            }
        });
    }

    public void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
