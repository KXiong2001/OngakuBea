package com.example.ongakubea.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ongakubea.models.VideoItemsContract;

public class VideoItemsDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "VideoItems.db";

    public VideoItemsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTables(sqLiteDatabase);
        setIndexes(sqLiteDatabase);
    }

    private void createTables(SQLiteDatabase sqLiteDatabase) {
        String createVideoItemTable = String.format(
                "CREATE TABLE IF NOT EXISTS %s (%s TEXT PRIMARY KEY,%s TEXT)",
                VideoItemsContract.VideoItems.TABLE_NAME,
                VideoItemsContract.VideoItems.VIDEO_ID,
                VideoItemsContract.VideoItems.VIDEO_TITLE);
        sqLiteDatabase.execSQL(createVideoItemTable);

        String createVideoPlaylistMappingTable = String.format(
                "CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT,%s TEXT,%s TEXT)",
                VideoItemsContract.VideoPlaylistMappings.TABLE_NAME,
                VideoItemsContract.VideoPlaylistMappings.MAPPING_ID,
                VideoItemsContract.VideoPlaylistMappings.VIDEO_ID,
                VideoItemsContract.VideoPlaylistMappings.PLAYLIST_ID);
        sqLiteDatabase.execSQL(createVideoPlaylistMappingTable);
    }

    private void setIndexes(SQLiteDatabase sqLiteDatabase) {
         String videoItemTableIndex = String.format(
                "CREATE UNIQUE INDEX IF NOT EXISTS %s ON %s(%s)",
                VideoItemsContract.VideoItems.VIDEO_ID_INDEX,
                 VideoItemsContract.VideoItems.TABLE_NAME,
                VideoItemsContract.VideoItems.VIDEO_ID );
        sqLiteDatabase.execSQL(videoItemTableIndex);

        String videoPlaylistMappingTableIndex = String.format(
                "CREATE INDEX IF NOT EXISTS %s ON %s(%s)",
                VideoItemsContract.VideoPlaylistMappings.PLAYLIST_ID_INDEX,
                VideoItemsContract.VideoPlaylistMappings.TABLE_NAME,
                VideoItemsContract.VideoPlaylistMappings.PLAYLIST_ID );
        sqLiteDatabase.execSQL(videoPlaylistMappingTableIndex);

        System.out.println(videoItemTableIndex);
        System.out.println(videoPlaylistMappingTableIndex);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String VideoItemsEntries = "DROP TABLE IF EXISTS " + VideoItemsContract.VideoItems.TABLE_NAME;
        sqLiteDatabase.execSQL(VideoItemsEntries);

        String VideoPlaylistMappingEntries = "DROP TABLE IF EXISTS " + VideoItemsContract.VideoItems.TABLE_NAME;
        sqLiteDatabase.execSQL(VideoPlaylistMappingEntries);

        System.out.println(VideoItemsEntries);
        System.out.println(VideoPlaylistMappingEntries);

        onCreate(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        onUpgrade(sqLiteDatabase, oldVersion, newVersion);
    }
}
