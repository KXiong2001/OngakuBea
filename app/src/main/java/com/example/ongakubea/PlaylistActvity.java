package com.example.ongakubea;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.api.services.youtube.model.Playlist;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PlaylistActvity extends Activity {
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    RecyclerView playlistView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_list);
        playlistView = findViewById(R.id.playlistRows);
        fetchPlaylists();
    }

    private List<Playlist> fetchPlaylists() {
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = getSharedPreferences("p1", Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName == null) {
            return null;
        }

        mCredential.setSelectedAccountName(accountName);

        new ListPlaylists(mCredential);
        return null;
    }

    private class ListPlaylists implements Runnable {
        private com.google.api.services.youtube.YouTube mService = null;
        static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
        private Exception mLastError = null;

        ListPlaylists(GoogleAccountCredential credential) {
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
            List<Playlist> output = null;
            try {
                output = getPlaylists();
            } catch (Exception e) {
                mLastError = e;
                canceled();
            }

            List<Playlist> finalOutput = output;
            PlaylistActvity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PlaylistAdapter playlistAdapter = new PlaylistAdapter(finalOutput);
                    playlistView.setAdapter(playlistAdapter);
                    playlistView.setLayoutManager(new LinearLayoutManager(PlaylistActvity.this));
                }
            });
        }


        private void canceled() {
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
                    System.out.println("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                System.out.println("Request cancelled.");
            }
        }

        /**
         * Fetch information about the "GoogleDevelopers" YouTube channel.
         * @return List of Strings containing information about the channel.
         * @throws IOException
         */
        private List<Playlist> getPlaylists() throws IOException {
            List<Playlist> pls = mService.playlists()
                    .list("snippet,contentDetails")
                    .setMine(true)
                    .execute()
                    .getItems();
            return pls;
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
                    PlaylistActvity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }

    }
}
