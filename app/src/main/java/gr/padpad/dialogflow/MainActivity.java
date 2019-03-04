package gr.padpad.dialogflow;

import ai.api.android.AIService;
import ai.api.model.Result;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
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

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import gr.padpad.dialogflow.model.RequestQuery;
import gr.padpad.dialogflow.retrofit.client.RetrofitClient;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R2.id.listenTextView)
    AppCompatTextView listenTextView;

    @BindView(R2.id.responseTextView)
    AppCompatTextView responseTextView;

    @BindView(R2.id.typeEdiText)
    AppCompatEditText typeEditext;

    @BindView(R2.id.progressBar)
    ProgressBar progressBar;

    @OnClick(R2.id.listenButton)
    void listen() {
        sendMessage(Objects.requireNonNull(typeEditext.getText()).toString());
        progressBar.setVisibility(View.VISIBLE);
    }

    private SessionsClient sessionsClient;
    private SessionName session;
    private String uuid = UUID.randomUUID().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        RetrofitClient.initRetrofit();
        initChatbot();
    }

    private void initChatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.test_agent_credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials)credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String msg) {
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            typeEditext.setText("");
            QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
            new RequestQuery(MainActivity.this, session, sessionsClient, queryInput).execute();
        }
    }

    public void getResponse(DetectIntentResponse response) {
        progressBar.setVisibility(View.GONE);
        if (response != null) {
            String botReply = response.getQueryResult().getFulfillmentText();
            Timber.d( "Bot Reply: %s", botReply);
            responseTextView.setText(botReply);
        } else {
            Timber.d( "Bot Reply: Null");
            responseTextView.setText("There was some communication issue. Please Try again!");
        }
    }
}
