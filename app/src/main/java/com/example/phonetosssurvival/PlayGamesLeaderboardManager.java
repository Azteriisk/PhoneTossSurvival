package com.example.phonetosssurvival;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;

public class PlayGamesLeaderboardManager {

    private static final String TAG = "PlayGamesLeaderboard";

    // Google Play Console Global Leaderboard ID
    public static final String GLOBAL_LEADERBOARD_ID = "CgkI_phone_toss_global_leaderboard";

    public static void initialize(Context context) {
        try {
            PlayGamesSdk.initialize(context);
            Log.d(TAG, "Google Play Games SDK initialized.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Play Games SDK", e);
        }
    }

    public static void checkAuthentication(Activity activity, AuthCallback callback) {
        try {
            GamesSignInClient signInClient = PlayGames.getGamesSignInClient(activity);
            signInClient.isAuthenticated().addOnCompleteListener(task -> {
                boolean isAuthenticated = task.isSuccessful() && task.getResult().isAuthenticated();
                Log.d(TAG, "Google Play Games Authenticated: " + isAuthenticated);
                if (callback != null) {
                    callback.onAuthResult(isAuthenticated);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Auth check failed", e);
            if (callback != null) callback.onAuthResult(false);
        }
    }

    public static void submitScoreToGlobalLeaderboard(Activity activity, float magnitude) {
        try {
            long scoreInUnits = (long) (magnitude * 100); // Scale by 100 for precision
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(activity);
            leaderboardsClient.submitScore(GLOBAL_LEADERBOARD_ID, scoreInUnits);
            Log.d(TAG, "Submitted global high score to Play Games: " + scoreInUnits);
        } catch (Exception e) {
            Log.e(TAG, "Error submitting score to Play Games Leaderboard", e);
        }
    }

    public static void showGlobalLeaderboardUI(Activity activity) {
        try {
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(activity);
            leaderboardsClient.getLeaderboardIntent(GLOBAL_LEADERBOARD_ID)
                    .addOnSuccessListener(intent -> activity.startActivityForResult(intent, 9001))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to open Play Games Leaderboard UI", e));
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Games Leaderboard UI", e);
        }
    }

    public interface AuthCallback {
        void onAuthResult(boolean isAuthenticated);
    }
}
