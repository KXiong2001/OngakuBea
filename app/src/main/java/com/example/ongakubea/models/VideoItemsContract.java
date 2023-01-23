package com.example.ongakubea.models;

public class VideoItemsContract {
    private VideoItemsContract() {}

    public static class VideoItems {
        public static final String TABLE_NAME = "video_items";
        public static final String VIDEO_ID = "video_id";
        public static final String VIDEO_TITLE = "video_title";
        public static final String VIDEO_ID_INDEX = "video_id_index";
        public static final String PLAYLIST_SUGGESTED_FROM = "playlist_suggested_from";
        public static final String SUGGESTED = "suggested";

    }

    public static class VideoPlaylistMappings {
        public static final String TABLE_NAME = "video_playlist_mappings";
        public static final String VIDEO_ID = "video_id";
        public static final String PLAYLIST_ID = "playlist_id";
        public static final String PLAYLIST_ID_INDEX = "playlist_id_index";
    }
}
