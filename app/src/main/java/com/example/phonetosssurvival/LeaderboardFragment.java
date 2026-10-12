package com.example.phonetosssurvival;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

public class LeaderboardFragment extends Fragment {

    private LinearLayout layoutScoresContainer;
    private View layoutEmptyState;
    private ScrollView scrollLeaderboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutScoresContainer = view.findViewById(R.id.layoutScoresContainer);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        scrollLeaderboard = view.findViewById(R.id.scrollLeaderboard);

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

        Button btnPlayGames = view.findViewById(R.id.btnPlayGamesLeaderboard);
        if (btnPlayGames != null) {
            btnPlayGames.setOnClickListener(v -> {
                if (getActivity() != null) {
                    PlayGamesLeaderboardManager.showGlobalLeaderboardUI(getActivity());
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
        if (layoutScoresContainer == null || ctx == null) return;

        LeaderboardDatabaseHelper db = LeaderboardDatabaseHelper.getInstance(ctx);
        db.migrateFromSharedPreferences(ctx);

        List<ScoreEntry> entries = db.getAllScoresSorted();
        layoutScoresContainer.removeAllViews();

        if (entries.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (scrollLeaderboard != null) scrollLeaderboard.setVisibility(View.GONE);
            return;
        }

        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (scrollLeaderboard != null) scrollLeaderboard.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(ctx);
        int rank = 1;
        for (ScoreEntry score : entries) {
            View itemView = inflater.inflate(R.layout.item_leaderboard_score, layoutScoresContainer, false);

            TextView tvRank = itemView.findViewById(R.id.tvRank);
            TextView tvPlayerName = itemView.findViewById(R.id.tvPlayerName);
            TextView tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            TextView tvImpactForce = itemView.findViewById(R.id.tvImpactForce);

            if (tvRank != null) tvRank.setText(String.format(Locale.US, "#%d", rank));
            if (tvPlayerName != null) tvPlayerName.setText(score.getPlayerName());
            if (tvTimestamp != null) tvTimestamp.setText(score.getTimestamp());
            if (tvImpactForce != null) {
                tvImpactForce.setText(String.format(Locale.US, "%.1f m/s² (%.1f Gs)", score.getMagnitude(), score.getGForce()));
            }

            layoutScoresContainer.addView(itemView);
            rank++;
        }
    }
}
