package com.example.phonetosssurvival;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        if (activity == null) return;
        try {
            String leaderboardId = activity.getString(R.string.play_games_leaderboard_id);
            long scoreInUnits = (long) (magnitude * 100);
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(activity);
            leaderboardsClient.submitScore(leaderboardId, scoreInUnits);
            Log.d(TAG, "Submitted global high score to Play Games (" + leaderboardId + "): " + scoreInUnits);
        } catch (Exception e) {
            Log.e(TAG, "Error submitting score to Play Games Leaderboard", e);
        }
    }

    public static void showGlobalLeaderboardUI(Activity activity) {
        if (activity == null) return;

        try {
            String leaderboardId = activity.getString(R.string.play_games_leaderboard_id);
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(activity);
            leaderboardsClient.getLeaderboardIntent(leaderboardId)
                    .addOnSuccessListener(intent -> activity.startActivityForResult(intent, 9001))
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Play Games overlay unavailable with current ID", e);
                        showPlayGamesConfigDialog(activity);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error opening Play Games Leaderboard UI", e);
            showPlayGamesConfigDialog(activity);
        }
    }

    private static void showPlayGamesConfigDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("GOOGLE PLAY GAMES SERVICES")
                .setMessage("Google Play Games Services V2 SDK is active!\n\nGlobal leaderboards sync automatically with Google Play Games once your Play Console App ID is linked.\n\nLocal scores remain saved in your SQLite database.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public interface AuthCallback {
        void onAuthResult(boolean isAuthenticated);
    }
}
