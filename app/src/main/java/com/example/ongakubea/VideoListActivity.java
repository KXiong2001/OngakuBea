package com.example.ongakubea;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoListActivity extends Activity {
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";

    private String playlistId;
    private List<Video> musicVideos;
    RecyclerView musicVideosList;

    public static void start(Context context, String playlistId) {
        Intent intent = new Intent(context, VideoListActivity.class);
        intent.putExtra("playlistId", playlistId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_list);

        Intent intent = this.getIntent();
        String playlistId = intent.getStringExtra("playlistId");
        this.playlistId = playlistId;
        this.musicVideos = new ArrayList<>();
        this.musicVideosList = findViewById(R.id.videosRecyclerView);

        showMusicVideos();
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

        new GetPlaylistItemDetails(mCredential);
    }

    private class GetPlaylistItemDetails implements Runnable {
        private com.google.api.services.youtube.YouTube mService = null;
        static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
        private Exception mLastError = null;

        GetPlaylistItemDetails(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("YouTube Data API Android Quickstart")
                    .build();


            new Thread(this, "getPlaylistDetails").start();
        }

        @Override
        public void run() {
            try {
                List<PlaylistItem> playlistItems = mService.playlistItems()
                        .list("contentDetails")
                        .setPlaylistId(playlistId)
                        .setMaxResults(50L)
                        .execute()
                        .getItems();

                System.out.println("Playlist items:" + playlistItems.toString());

                String allVideoIds = "";
                for (PlaylistItem pli : playlistItems) {
                    String videoId = pli.getContentDetails().getVideoId();
                    allVideoIds += videoId + ",";
                }
                allVideoIds = allVideoIds.substring(0, allVideoIds.length() - 2);
                System.out.println(allVideoIds);

                List<Video> videos = mService.videos()
                        .list("snippet,contentDetails")
                        .setId(allVideoIds)
                        .execute()
                        .getItems();

                System.out.println("Videos: " + videos.toString());

                for (Video video : videos) {
                    String videoCategory = video.getSnippet().getCategoryId();
                    if (videoCategory.equals("10")) {
                        System.out.println("Is a music video!");
                        musicVideos.add(video);
                    } else {
                        System.out.println(String.format("%s is not a music video. Category: %s",
                                video.getSnippet().getTitle(), videoCategory));
                    }
                }


            } catch (Exception e) {
                mLastError = e;
                System.out.println(e.getMessage());
                canceled();
            }


            VideoListActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("1111111" + musicVideos);
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
                    VideoListActivity.this,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES);
            dialog.show();
        }

    }
}
