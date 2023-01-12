package com.example.ongakubea;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class APIActivity extends Activity {
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    private Button button;
    private TextView mOutputText;
    ProgressDialog mProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api);

        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = getSharedPreferences("p1", Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName == null) {
            System.out.println("accountName is null!");
            System.out.println(getSharedPreferences("p1", Context.MODE_PRIVATE).getAll());
            return;
        }

        mCredential.setSelectedAccountName(accountName);

        mOutputText = findViewById(R.id.callAPIText);
        button = findViewById(R.id.callAPIButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MakeRequestThread(mCredential);
            }
        });
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling YouTube Data API ...");

    }

    private class MakeRequestThread implements Runnable {
        private com.google.api.services.youtube.YouTube mService = null;
        private Exception mLastError = null;

        MakeRequestThread(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("YouTube Data API Android Quickstart")
                    .build();
            new Thread(this, "MakeRequest").start();
        }


        @Override
        public void run() {
            preExecute();
            List<String> output = null;
            try {
                output = getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                canceled();
            }
            postExecute(output);
        }

        private void preExecute() {

            APIActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOutputText.setText("");
                    mProgress.show();
                }
            });
        }

        private void postExecute(List<String> output) {
            if (output == null) {
                return;
            }

            APIActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.hide();
                    if (output == null || output.size() == 0) {
                        mOutputText.setText("No results returned.");
                    } else {
                        output.add(0, "Data retrieved using the YouTube Data API:");
                        mOutputText.setText(TextUtils.join("\n", output));
                    }
                }
            });
        }

        private void canceled() {

            APIActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.hide();
                    if (mLastError != null) {
                        if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                            showGooglePlayServicesAvailabilityErrorDialog(
                                    ((GooglePlayServicesAvailabilityIOException) mLastError)
                                            .getConnectionStatusCode());
                        } else if (mLastError instanceof UserRecoverableAuthIOException) {
                            startActivityForResult(
                                    ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                    LoginActivity.REQUEST_AUTHORIZATION);
                        } else {
                            mOutputText.setText("The following error occurred:\n"
                                    + mLastError.getMessage());
                        }
                    } else {
                        mOutputText.setText("Request cancelled.");
                    }
                }
            });
        }

        /**
          * Fetch information about the "GoogleDevelopers" YouTube channel.
          * @return List of Strings containing information about the channel.
          * @throws IOException
          */
        private List<String> getDataFromApi() throws IOException {
            // Get a list of up to 10 files.
            List<String> channelInfo = new ArrayList<String>();
            ChannelListResponse result = mService.channels().list("snippet,contentDetails,statistics")
                    .setForUsername("GoogleDevelopers")
                    .execute();
            List<Channel> channels = result.getItems();
            if (channels != null) {
                Channel channel = channels.get(0);
                channelInfo.add("This channel's ID is " + channel.getId() + ". " +
                        "Its title is '" + channel.getSnippet().getTitle() + ", " +
                        "and it has " + channel.getStatistics().getViewCount() + " views.");
            }
            return channelInfo;
        }

        /**
          * Display an error dialog showing that Google Play Services is missing
          * or out of date.
          * @param connectionStatusCode code describing the presence (or lack of)
          *     Google Play Services on this device.
          */
        void showGooglePlayServicesAvailabilityErrorDialog(
                final int connectionStatusCode) {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog dialog = apiAvailability.getErrorDialog(
                    APIActivity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }

    }
}
