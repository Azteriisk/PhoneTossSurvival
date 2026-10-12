package com.example.phonetosssurvival;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

public class LeaderboardFragment extends Fragment {

    private TextView tvLeaderboardList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvLeaderboardList = view.findViewById(R.id.tvLeaderboardList);
        Button btnClearLeaderboard = view.findViewById(R.id.btnClearLeaderboard);

        if (btnClearLeaderboard != null) {
            btnClearLeaderboard.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx != null) {
                    LeaderboardDatabaseHelper.getInstance(ctx).clearAllScores();
                    loadLeaderboard();
                    Toast.makeText(ctx, "Leaderboard reset!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadLeaderboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLeaderboard();
    }

    public void loadLeaderboard() {
        Context ctx = getContext();
        if (tvLeaderboardList == null || ctx == null) return;

        LeaderboardDatabaseHelper db = LeaderboardDatabaseHelper.getInstance(ctx);
        db.migrateFromSharedPreferences(ctx);

        List<ScoreEntry> entries = db.getAllScoresSorted();

        if (entries.isEmpty()) {
            tvLeaderboardList.setText("1. No survived throws recorded yet.\nArm sensor & toss phone to log score!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (ScoreEntry score : entries) {
            sb.append(String.format(Locale.US, "%d. %s - %.2f m/s² (%.2f Gs)\n   Logged: %s\n\n",
                    rank, score.getPlayerName(), score.getMagnitude(), score.getGForce(), score.getTimestamp()));
            rank++;
        }

        tvLeaderboardList.setText(sb.toString().trim());
    }
}
