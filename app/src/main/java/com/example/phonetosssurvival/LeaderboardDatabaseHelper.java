package com.example.phonetosssurvival;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LeaderboardDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "LeaderboardDB";
    private static final String DATABASE_NAME = "phonetoss_leaderboard.db";
    private static final int DATABASE_VERSION = 2; // Incremented for player_name column

    public static final String TABLE_LEADERBOARD = "leaderboard";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAYER_NAME = "player_name";
    public static final String COLUMN_MAGNITUDE = "magnitude";
    public static final String COLUMN_G_FORCE = "g_force";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static LeaderboardDatabaseHelper instance;

    public static synchronized LeaderboardDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LeaderboardDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private LeaderboardDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_LEADERBOARD + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PLAYER_NAME + " TEXT NOT NULL DEFAULT 'ANONYMOUS', "
                + COLUMN_MAGNITUDE + " REAL NOT NULL, "
                + COLUMN_G_FORCE + " REAL NOT NULL, "
                + COLUMN_TIMESTAMP + " TEXT NOT NULL"
                + ");";
        db.execSQL(createTableQuery);
        Log.d(TAG, "SQLite Leaderboard table created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_LEADERBOARD + " ADD COLUMN " + COLUMN_PLAYER_NAME + " TEXT NOT NULL DEFAULT 'ANONYMOUS'");
            Log.d(TAG, "Migrated SQLite database to version 2 (added player_name).");
        }
    }

    public long addScore(String playerName, float magnitude, float gForce, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYER_NAME, (playerName == null || playerName.trim().isEmpty()) ? "ANONYMOUS" : playerName.trim());
        values.put(COLUMN_MAGNITUDE, magnitude);
        values.put(COLUMN_G_FORCE, gForce);
        values.put(COLUMN_TIMESTAMP, timestamp);

        long id = db.insert(TABLE_LEADERBOARD, null, values);
        Log.d(TAG, "Added new score for " + playerName + " to SQLite DB with ID: " + id);
        return id;
    }

    public List<ScoreEntry> getAllScoresSorted() {
        List<ScoreEntry> scores = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT " + COLUMN_ID + ", " + COLUMN_PLAYER_NAME + ", " + COLUMN_MAGNITUDE + ", " + COLUMN_G_FORCE + ", " + COLUMN_TIMESTAMP
                + " FROM " + TABLE_LEADERBOARD
                + " ORDER BY " + COLUMN_MAGNITUDE + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
                    int nameIndex = cursor.getColumnIndexOrThrow(COLUMN_PLAYER_NAME);
                    int magIndex = cursor.getColumnIndexOrThrow(COLUMN_MAGNITUDE);
                    int gIndex = cursor.getColumnIndexOrThrow(COLUMN_G_FORCE);
                    int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);

                    do {
                        long id = cursor.getLong(idIndex);
                        String name = cursor.getString(nameIndex);
                        float magnitude = cursor.getFloat(magIndex);
                        float gForce = cursor.getFloat(gIndex);
                        String timestamp = cursor.getString(timeIndex);

                        scores.add(new ScoreEntry(id, name, magnitude, gForce, timestamp));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        return scores;
    }

    public void clearAllScores() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LEADERBOARD, null, null);
        Log.d(TAG, "All scores cleared from SQLite database.");
    }

    public void migrateFromSharedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhoneTossScores", Context.MODE_PRIVATE);
        Set<String> legacyScores = prefs.getStringSet("LeaderboardScores", null);

        if (legacyScores != null && !legacyScores.isEmpty()) {
            for (String scoreStr : legacyScores) {
                String[] parts = scoreStr.split("\\|");
                if (parts.length >= 3) {
                    try {
                        float mag = Float.parseFloat(parts[0]);
                        float g = Float.parseFloat(parts[1]);
                        String time = parts[2];
                        addScore("LEGACY USER", mag, g, time);
                    } catch (Exception ignored) {}
                }
            }
            prefs.edit().remove("LeaderboardScores").apply();
            Log.d(TAG, "Migrated legacy SharedPreferences scores to SQLite Database.");
        }
    }
}
