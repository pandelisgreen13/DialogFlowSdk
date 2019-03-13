package gr.padpad.dialogflow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;
import com.google.protobuf.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gr.padpad.dialogflow.model.RequestQuery;
import gr.padpad.dialogflow.retrofit.client.RetrofitClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
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
    void listen() {
        startSpeechRecognizer();
    }

    private SessionsClient sessionsClient;
    private SpeechClient text;
    private SessionName session;
    private String uuid = UUID.randomUUID().toString();
    private TextToSpeech textToSpeech;
    private String mAnswer = "";
    private final static int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private CompositeDisposable compositeDisposable;
    private String directoryRoot = "DialogFlow/";
    private MediaPlayer mediaPlayer;
    private String audioFilePath = "/DialogFlow/dialog.mp3";
    private Translate translate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RetrofitClient.initRetrofit();
        compositeDisposable = new CompositeDisposable();
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        initChatbot();
        initTTS();
        initTranslate();
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

    private void initTranslate() {
        translate = TranslateOptions.newBuilder().setApiKey("myKey").build().getService();
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

            SpeechSettings.Builder speechBuilder = SpeechSettings.newBuilder();
            SpeechSettings speechSettings = speechBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();


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

    private void textToSpeech(String message, boolean allRequiredParamsPresent) {
//        Translation translation = translate.translate(message,
//                Translate.TranslateOption.sourceLanguage(detectedLanguage(message)),
//                Translate.TranslateOption.targetLanguage("el"));

        compositeDisposable.add(RetrofitClient.getTTS("UTF-8", "el", "tw-ob", message)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response == null) {
                        return;

                    }
                    if (response.body() != null) {
                        FileUtils.saveFile(directoryRoot, response.body());
                    }
                    if (FileUtils.checkIfFileExists(FileUtils.getDirectoryRoot(directoryRoot), "dialog", ".mp3")) {
                        mediaPlayer = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().getPath() + audioFilePath));
                        mediaPlayer.start();
                    }

                }, Timber::e));
    }

    private String detectedLanguage(String message) {
        Detection detection = translate.detect(message);
        return detection.getLanguage();
    }

    public void onPause() {
        super.onPause();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


}
