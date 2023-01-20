package com.example.ongakubea;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoListActivity extends Activity {
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PLAYLIST_ID_EXTRA_NAME = "playlistId";

    private GoogleAccountCredential mCredential;
    private com.google.api.services.youtube.YouTube mService = null;
    private String playlistId;
    private List<Video> musicVideos;

    RecyclerView musicVideosList;
    Button suggestButton;

    public static void start(Context context, String playlistId) {
        Intent intent = new Intent(context, VideoListActivity.class);
        intent.putExtra(PLAYLIST_ID_EXTRA_NAME, playlistId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        Intent intent = this.getIntent();
        String playlistId = intent.getStringExtra(PLAYLIST_ID_EXTRA_NAME);
        this.playlistId = playlistId;
        this.musicVideos = new ArrayList<>();

        this.musicVideosList = findViewById(R.id.videosRecyclerView);
        this.suggestButton = findViewById(R.id.suggestButton);

        setCredentials();
        setButtonOnClick();
        showMusicVideos();
    }

    private void setCredentials() {
         mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = getSharedPreferences("p1", Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName == null) {
            return;
        }

        mCredential.setSelectedAccountName(accountName);

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.youtube.YouTube.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("YouTube Data API Android Quickstart")
                .build();
    }
    private void setButtonOnClick() {
        this.suggestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (VideoListActivity.this.musicVideos == null ||
                        VideoListActivity.this.musicVideos.size() == 0) {
                    String message = "There is no music videos in this playlist :(";
                    Toast.makeText(VideoListActivity.this, message, Toast.LENGTH_SHORT).show();
                    return;
                }

                new GetRecommendations();
            }
        });
    }
    private void showMusicVideos() {
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        String accountName = getSharedPreferences("p1", Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null);
        if (accountName == null) {
            return;
        }

        mCredential.setSelectedAccountName(accountName);
        new GetPlaylistItemDetails();
    }

    private class GetPlaylistItemDetails implements Runnable {
        static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
        private Exception mLastError = null;

        GetPlaylistItemDetails() {

            new Thread(this, "getPlaylistDetails").start();
        }

        @Override
        public void run() {
            try {
                fetchMusicVideos();
            } catch (Exception e) {
                mLastError = e;
                System.out.println(e.getMessage());
                canceled();
            }
            displayMusicVideos();
        }

        private void fetchMusicVideos() throws IOException {
            List<PlaylistItem> playlistItems = mService.playlistItems()
                    .list("contentDetails")
                    .setPlaylistId(playlistId)
                    .setMaxResults(50L)
                    .execute()
                    .getItems();

            String allVideoIds = "";
            for (PlaylistItem pli : playlistItems) {
                String videoId = pli.getContentDetails().getVideoId();
                allVideoIds += videoId + ",";
            }
            allVideoIds = allVideoIds.substring(0, allVideoIds.length() - 2);

            List<Video> videos = mService.videos()
                    .list("snippet,contentDetails")
                    .setId(allVideoIds)
                    .execute()
                    .getItems();

            for (Video video : videos) {
                String videoCategory = video.getSnippet().getCategoryId();
                if (videoCategory.equals("10")) {
                    musicVideos.add(video);
                }
            }
        }

        private void displayMusicVideos() {
            VideoListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VideoListAdapter playlistAdapter = new VideoListAdapter(musicVideos);
                    musicVideosList.setAdapter(playlistAdapter);
                    musicVideosList.setLayoutManager(new LinearLayoutManager(VideoListActivity.this));
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

        void showGooglePlayServicesAvailabilityErrorDialog(
                final int connectionStatusCode) {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog dialog = apiAvailability.getErrorDialog(
                    VideoListActivity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }

    }

    private class GetRecommendations implements Runnable {

        static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
        private Exception mLastError = null;

        GetRecommendations() {
            new Thread(this, "getRecommendations").start();
        }

        @Override
        public void run() {
            try {
                System.out.println("Suggestion button clicked");
                getRecommendedMusicVideos();
            } catch (Exception e) {
                mLastError = e;
                System.out.println(e.getMessage());
                canceled();
            }
        }

        private void getRecommendedMusicVideos() throws IOException {
            List<Video> musicVideos = VideoListActivity.this.musicVideos;
            if (musicVideos == null || musicVideos.size() == 0) {
                System.out.println("getRecommendedMusicVideos: No Music videos exist in playlist.");
                return;
            }

            String videoId = musicVideos.get((int)(Math.random() * musicVideos.size())).getId();

            List<SearchResult> results = mService.search()
                    .list("snippet")
                    .setRelatedToVideoId(videoId)
                    .setType("video")
                    .setMaxResults(50L)
                    .execute().getItems();

            for (SearchResult result : results) {
                System.out.println(result.toString());
            }
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

        void showGooglePlayServicesAvailabilityErrorDialog(
                final int connectionStatusCode) {
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            Dialog dialog = apiAvailability.getErrorDialog(
                    VideoListActivity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }

    }
}
