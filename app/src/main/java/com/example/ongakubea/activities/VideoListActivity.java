package com.example.ongakubea.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ongakubea.R;
import com.example.ongakubea.adapters.VideoListAdapter;
import com.example.ongakubea.models.VideoItemsContract;
import com.example.ongakubea.utils.VideoItemsDbHelper;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class VideoListActivity extends Activity {
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PLAYLIST_ID_EXTRA_NAME = "playlistId";

    private com.google.api.services.youtube.YouTube mService = null;

    private String playlistId;
    private List<Video> musicVideos;
    BlockingQueue<List<Video>> blockingQueue;

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

        this.playlistId = intent.getStringExtra(PLAYLIST_ID_EXTRA_NAME);
        this.musicVideos = new ArrayList<>();
        this.blockingQueue = new LinkedBlockingDeque<>(1);

        this.musicVideosList = findViewById(R.id.videosRecyclerView);
        this.suggestButton = findViewById(R.id.suggestButton);

        setCredentials();
        setButtonOnClick();

        new GetVideoList();
        new InsertVideosToDatabase();
    }

    private void setCredentials() {
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
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
        this.suggestButton.setOnClickListener(view -> {
            if (VideoListActivity.this.musicVideos == null ||
                    VideoListActivity.this.musicVideos.size() == 0) {
                String message = "There is no music videos in this playlist :(";
                Toast.makeText(VideoListActivity.this, message, Toast.LENGTH_SHORT).show();
                return;
            }

            new GetRecommendations();
        });
    }

    private class GetVideoList implements Runnable {
        static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
        private Exception mLastError = null;

        GetVideoList() {
            new Thread(this, "getPlaylistDetails").start();
        }

        @Override
        public void run() {
            try {
                fetchMusicVideos();
                blockingQueue.put(musicVideos);
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
            VideoListActivity.this.runOnUiThread(() -> {
                VideoListAdapter playlistAdapter = new VideoListAdapter(musicVideos);
                musicVideosList.setAdapter(playlistAdapter);
                musicVideosList.setLayoutManager(new LinearLayoutManager(VideoListActivity.this));
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

        private SQLiteDatabase sqLiteDatabase;

        GetRecommendations() {
            VideoItemsDbHelper dbHelper = new VideoItemsDbHelper(VideoListActivity.this);
            sqLiteDatabase = dbHelper.getWritableDatabase();

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

//            for (int i = 0; i < 3; i++) {
//                String videoId = musicVideos.get((int)(Math.random() * musicVideos.size())).getId();
//                SearchListResponse results = mService.search()
//                        .list("snippet")
//                        .setRelatedToVideoId(videoId)
//                        .setType("video")
//                        .setMaxResults(50L)
//                        .execute();
//                insertSearchResults(results);
//            }
//
            List<String> recommendedVideoIds = fetchRandomVideosFromDatabase();
            System.out.println(recommendedVideoIds);
            String recommendedVideoIdsStr = String.join(", ", recommendedVideoIds);
            recommendedVideoIdsStr = recommendedVideoIdsStr.replace(" ", "");

            System.out.println(recommendedVideoIdsStr);
            VideoListResponse recommendedVideos = mService.videos()
                    .list("snippet,contentDetails")
                    .setId(recommendedVideoIdsStr)
                    .execute();

            System.out.println(recommendedVideos.toPrettyString());;


        }

        private void insertSearchResults(SearchListResponse response) {
            String allVideoIds = "";
            for(SearchResult sr : response.getItems()) {
                allVideoIds += sr.getId().getVideoId() + ",";
            }
            allVideoIds = allVideoIds.substring(0, allVideoIds.length() - 2);

            List<Video> videos;
            try {
                videos = mService.videos()
                        .list("snippet,contentDetails")
                        .setId(allVideoIds)
                        .execute()
                        .getItems();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (Video video : videos) {
                if (!video.getSnippet().getCategoryId().equals("10")) {
                    continue;
                }

                System.out.println("Inserting video " + video.getSnippet().getTitle());
                insertRecommendedVideoToDatabase(video);
            }
        }

        private void insertRecommendedVideoToDatabase(Video video) {
            ContentValues values = new ContentValues();

            values.put(VideoItemsContract.VideoItems.VIDEO_ID, video.getId());
            values.put(VideoItemsContract.VideoItems.VIDEO_TITLE, video.getSnippet().getTitle());
            values.put(VideoItemsContract.VideoItems.PLAYLIST_SUGGESTED_FROM, playlistId);
            values.put(VideoItemsContract.VideoItems.SUGGESTED, false);

            long newRowId = sqLiteDatabase.insertWithOnConflict(
                    VideoItemsContract.VideoItems.TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE);

            if (newRowId == -1) {
                System.out.println("Error for inserting recommended video!");
            }
        }


        private List<String> fetchRandomVideosFromDatabase() {

            Cursor cursor = sqLiteDatabase.query(
                    VideoItemsContract.VideoItems.TABLE_NAME + " Order BY RANDOM() LIMIT 20",
                    new String[] { "*" }, null, null, null, null, null
            );

            List<String> videoIds = new ArrayList<>();
            while(cursor.moveToNext()) {
                String videoId = cursor.getString(
                        cursor.getColumnIndexOrThrow(VideoItemsContract.VideoItems.VIDEO_ID));
                videoIds.add(videoId);
            }

            return videoIds;
        }

        private void createNewPlaylist() throws IOException {
            Playlist playlist = new Playlist();

            PlaylistSnippet snippet = new PlaylistSnippet();
            snippet.setDescription("This is a sample playlist description.");
            snippet.setTitle("Sample playlist created via API");
            playlist.setSnippet(snippet);

            // Add the status object property to the Playlist object.
            PlaylistStatus status = new PlaylistStatus();
            status.setPrivacyStatus("private");
            playlist.setStatus(status);

            // Define and execute the API request
            YouTube.Playlists.Insert request = mService.playlists()
                    .insert("snippet,status", playlist);
            Playlist response = request.execute();
            System.out.println(response);
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

    private class InsertVideosToDatabase implements Runnable {
        private SQLiteDatabase sqLiteDatabase;
        private List<Video> videos;

        InsertVideosToDatabase() {
            VideoItemsDbHelper dbHelper = new VideoItemsDbHelper(VideoListActivity.this);
            sqLiteDatabase = dbHelper.getWritableDatabase();

            new Thread(this, "InsertVideosToDatabase").start();;

        }

        @Override
        public void run() {
            try {
                if (musicVideos != null && musicVideos.size() != 0) {
                    videos = musicVideos;
                } else {
                    videos = blockingQueue.take();
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                return;
            }

            for (Video video : videos) {
                insertVideo(video);
            }
        }

        private void insertVideo(Video video) {
            // Create a new map of values, where column names are the keys

            ContentValues values = new ContentValues();

            values.put(VideoItemsContract.VideoItems.VIDEO_ID, video.getId());
            values.put(VideoItemsContract.VideoItems.VIDEO_TITLE, video.getSnippet().getTitle());

            long newRowId = sqLiteDatabase.insertWithOnConflict(
                    VideoItemsContract.VideoItems.TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE);

            if (newRowId == -1) {
                System.out.println("Error for inserting video!");
            }

            // TODO: error handling

            values = new ContentValues();
            values.put(VideoItemsContract.VideoPlaylistMappings.PLAYLIST_ID, playlistId);
            values.put(VideoItemsContract.VideoPlaylistMappings.VIDEO_ID, video.getId());
            newRowId = sqLiteDatabase.insertWithOnConflict(
                    VideoItemsContract.VideoPlaylistMappings.TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE);

            if (newRowId == -1) {
                System.out.println("Error for inserting mapping!");
                return;
            }

        }
    }
}
