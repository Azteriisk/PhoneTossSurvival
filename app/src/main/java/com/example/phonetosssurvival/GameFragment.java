package com.example.phonetosssurvival;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class GameFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "PhoneTossGame";
    private static final String PREFS_NAME = "PhoneTossScores";
    private static final String KEY_SCORES = "LeaderboardScores";

    // SensorManager & Sensor references
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;

    // UI Components (Game Page)
    private TextView tvGameTitle;
    private TextView tvStateBadge;
    private TextView tvLiveAccel;
    private TextView tvPeakForce;
    private TextView tvCommentary;
    private TextView tvThresholdLabel;
    private SeekBar sbThreshold;

    private Button btnArmSensor;
    private Button btnSimToss;

    // Survival Prompt Panel
    private MaterialCardView cardSurvivalPrompt;
    private TextView tvImpactSummary;
    private TextView tvCountdown;
    private Button btnSurvived;
    private Button btnDestroyed;

    // Debug Controls
    private MaterialCardView cardDebugControls;
    private SwitchMaterial swDebugMode;
    private LinearLayout layoutDebugOptions;
    private Button btnAddFakeScore;
    private Button btnResetLeaderboardDebug;

    // Game Logic State
    private boolean isArmed = false;
    private boolean isWaitingForSurvivalCheck = false;
    private float triggerThreshold = 15.0f; // m/s²
    private float currentPeakMagnitude = 0.0f;
    private CountDownTimer survivalTimer;

    // Secret Tap Gate State
    private int titleTapCount = 0;
    private long lastTapTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize SensorManager
        Context context = requireContext();
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // 2. Bind UI Views
        bindViews(view);

        // 3. Setup Listeners
        setupListeners(view);

        // 4. Initialize Debug Gate
        initDebugModeGate();
    }

    private void bindViews(View root) {
        tvGameTitle = root.findViewById(R.id.tvGameTitle);
        tvStateBadge = root.findViewById(R.id.tvStateBadge);
        tvLiveAccel = root.findViewById(R.id.tvLiveAccel);
        tvPeakForce = root.findViewById(R.id.tvPeakForce);
        tvCommentary = root.findViewById(R.id.tvCommentary);
        tvThresholdLabel = root.findViewById(R.id.tvThresholdLabel);
        sbThreshold = root.findViewById(R.id.sbThreshold);

        btnArmSensor = root.findViewById(R.id.btnArmSensor);
        btnSimToss = root.findViewById(R.id.btnSimToss);

        cardSurvivalPrompt = root.findViewById(R.id.cardSurvivalPrompt);
        tvImpactSummary = root.findViewById(R.id.tvImpactSummary);
        tvCountdown = root.findViewById(R.id.tvCountdown);
        btnSurvived = root.findViewById(R.id.btnSurvived);
        btnDestroyed = root.findViewById(R.id.btnDestroyed);

        cardDebugControls = root.findViewById(R.id.cardDebugControls);
        swDebugMode = root.findViewById(R.id.swDebugMode);
        layoutDebugOptions = root.findViewById(R.id.layoutDebugOptions);
        btnAddFakeScore = root.findViewById(R.id.btnAddFakeScore);
        btnResetLeaderboardDebug = root.findViewById(R.id.btnResetLeaderboardDebug);
    }

    private void initDebugModeGate() {
        if (cardDebugControls == null) return;
        if (BuildConfig.DEBUG) {
            cardDebugControls.setVisibility(View.VISIBLE);
        } else {
            cardDebugControls.setVisibility(View.GONE);
        }
    }

    private void setupListeners(View root) {
        View logoView = root.findViewById(R.id.ivHeaderLogo);
        if (logoView != null) {
            logoView.setOnClickListener(v -> handleSecretTapGate());
        }
        if (tvGameTitle != null) {
            tvGameTitle.setOnClickListener(v -> handleSecretTapGate());
        }

        if (swDebugMode != null) {
            swDebugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (btnSimToss != null) btnSimToss.setVisibility(View.VISIBLE);
                    if (layoutDebugOptions != null) layoutDebugOptions.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Debug Mode ENABLED: Test Throw & Leaderboard editing active", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Debug Mode ENABLED.");
                } else {
                    if (btnSimToss != null) btnSimToss.setVisibility(View.GONE);
                    if (layoutDebugOptions != null) layoutDebugOptions.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Debug Mode DISABLED: Normal play mode active", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Debug Mode DISABLED.");
                }
            });
        }

        if (btnAddFakeScore != null) {
            btnAddFakeScore.setOnClickListener(v -> {
                Random rand = new Random();
                float fakeMag = 35.0f + rand.nextFloat() * 25.0f;
                float fakeG = fakeMag / 9.80665f;
                saveScoreToLeaderboard("DEBUG USER", fakeMag, fakeG);
                Toast.makeText(getContext(), String.format(Locale.US, "Added fake high score: %.1f m/s²", fakeMag), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnResetLeaderboardDebug != null) {
            btnResetLeaderboardDebug.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx != null) {
                    SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().remove(KEY_SCORES).apply();
                    Toast.makeText(ctx, "Leaderboard cleared via debug panel!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnArmSensor != null) {
            btnArmSensor.setOnClickListener(v -> {
                if (isWaitingForSurvivalCheck) {
                    Toast.makeText(getContext(), "Please resolve active survival check first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isArmed) {
                    isArmed = true;
                    btnArmSensor.setText("DISARM SENSOR");
                    if (tvStateBadge != null) tvStateBadge.setText("ARMED: TOSS PHONE NOW!");
                    if (tvCommentary != null) tvCommentary.setText("🚀 Launch protocol ready! Toss or drop your phone!");
                } else {
                    disarmSensor();
                }
            });
        }

        if (btnSimToss != null) {
            btnSimToss.setOnClickListener(v -> {
                if (isWaitingForSurvivalCheck) {
                    Toast.makeText(getContext(), "Please confirm active survival prompt!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Random rand = new Random();
                float simMagnitude = 24.0f + rand.nextFloat() * 24.0f;
                triggerThrowImpact(simMagnitude);
            });
        }

        if (sbThreshold != null) {
            sbThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    triggerThreshold = (float) progress;
                    if (tvThresholdLabel != null) {
                        tvThresholdLabel.setText(String.format(Locale.US, "Throw Trigger Sensitivity: %.1f m/s²", triggerThreshold));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (btnSurvived != null) {
            btnSurvived.setOnClickListener(v -> handleSurvivalConfirmation(true));
        }

        if (btnDestroyed != null) {
            btnDestroyed.setOnClickListener(v -> handleSurvivalConfirmation(false));
        }
    }

    private void handleSecretTapGate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTapTime < 1000) {
            titleTapCount++;
        } else {
            titleTapCount = 1;
        }
        lastTapTime = currentTime;

        if (titleTapCount >= 5) {
            titleTapCount = 0;
            if (cardDebugControls != null) {
                if (cardDebugControls.getVisibility() == View.VISIBLE) {
                    cardDebugControls.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Developer Debug Mode Hidden", Toast.LENGTH_SHORT).show();
                } else {
                    cardDebugControls.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Developer Auth Granted! Debug Mode Unlocked", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double magnitude = Math.sqrt(x * x + y * y + z * z);
        float magFloat = (float) magnitude;

        if (tvLiveAccel != null) {
            tvLiveAccel.setText(String.format(Locale.US, "Live Accelerometer: X:%.1f Y:%.1f Z:%.1f m/s²", x, y, z));
        }

        if (isArmed && !isWaitingForSurvivalCheck && magFloat >= triggerThreshold) {
            triggerThrowImpact(magFloat);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void triggerThrowImpact(float impactMagnitude) {
        currentPeakMagnitude = impactMagnitude;
        float gForce = impactMagnitude / 9.80665f;

        if (tvPeakForce != null) {
            tvPeakForce.setText(String.format(Locale.US, "%.1f m/s² (%.1f Gs)", impactMagnitude, gForce));
        }
        if (tvCommentary != null) {
            tvCommentary.setText(getFunnyCommentary(gForce));
        }

        isArmed = false;
        isWaitingForSurvivalCheck = true;
        if (btnArmSensor != null) btnArmSensor.setText("ARM FOR TOSS");
        if (tvStateBadge != null) tvStateBadge.setText("SURVIVAL CHECK ACTIVE");

        if (cardSurvivalPrompt != null) cardSurvivalPrompt.setVisibility(View.VISIBLE);
        if (tvImpactSummary != null) {
            tvImpactSummary.setText(String.format(Locale.US, "Recorded Impact Force: %.2f m/s² (%.2f Gs)", impactMagnitude, gForce));
        }

        start60SecondCountdown();
    }

    private String getFunnyCommentary(float gForce) {
        if (gForce < 2.0f) {
            return "🛏️ Did you toss it or gently set it down on a pillow?";
        } else if (gForce < 3.5f) {
            return "✈️ Nice aerial toss! The phone tasted brief freedom!";
        } else if (gForce < 5.0f) {
            return "🚀 MODERATE LAUNCH! That phone felt centrifugal gravity!";
        } else if (gForce < 7.5f) {
            return "🏙️ EXTREME DROP! Are you throwing off a balcony?!";
        } else {
            return "💥 ORBITAL LAUNCH! Absolute phone destruction risk!";
        }
    }

    private void start60SecondCountdown() {
        if (survivalTimer != null) {
            survivalTimer.cancel();
        }

        survivalTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (tvCountdown != null) {
                    tvCountdown.setText(String.format(Locale.US, "TIME REMAINING TO CONFIRM: %ds", secondsRemaining));
                }
            }

            @Override
            public void onFinish() {
                if (tvCountdown != null) tvCountdown.setText("NO RESPONSE IN 60s! PHONE ASSUMED BROKEN!");
                if (tvCommentary != null) tvCommentary.setText("💀 R.I.P. Phone destroyed on impact! No response in 60s.");

                Toast.makeText(getContext(), "60s Expired! Phone assumed destroyed. Run forfeited!", Toast.LENGTH_LONG).show();

                if (btnSurvived != null) btnSurvived.setEnabled(false);
                if (btnDestroyed != null) btnDestroyed.setEnabled(false);

                if (tvCountdown != null) {
                    tvCountdown.postDelayed(() -> {
                        if (btnSurvived != null) btnSurvived.setEnabled(true);
                        if (btnDestroyed != null) btnDestroyed.setEnabled(true);
                        resetPromptUI();
                    }, 3000);
                }
            }
        }.start();
    }

    private void handleSurvivalConfirmation(boolean phoneSurvived) {
        if (survivalTimer != null) {
            survivalTimer.cancel();
        }

        float gForce = currentPeakMagnitude / 9.80665f;

        if (phoneSurvived) {
            promptPlayerNameAndSave(currentPeakMagnitude, gForce);
        } else {
            if (tvCommentary != null) tvCommentary.setText("Phone Destroyed! Run discarded.");
            Toast.makeText(getContext(), "Run Discarded. Phone destroyed!", Toast.LENGTH_SHORT).show();
            resetPromptUI();
        }
    }

    private void promptPlayerNameAndSave(float magnitude, float gForce) {
        Context ctx = getContext();
        if (ctx == null) {
            resetPromptUI();
            return;
        }

        final android.widget.EditText input = new android.widget.EditText(ctx);
        input.setHint("Enter Player Name / Handle");
        input.setSingleLine(true);

        android.widget.FrameLayout container = new android.widget.FrameLayout(ctx);
        int paddingPx = (int) (16 * ctx.getResources().getDisplayMetrics().density);
        container.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);
        container.addView(input);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle("SURVIVAL CONFIRMED!")
                .setMessage(String.format(Locale.US, "Impact Force: %.2f m/s² (%.2f Gs)\nEnter your name for the Hall of Fame:", magnitude, gForce))
                .setView(container)
                .setPositiveButton("LOG HIGH SCORE", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "ANONYMOUS SURVIVOR";

                    saveScoreToLeaderboard(name, magnitude, gForce);
                    Toast.makeText(ctx, "High Score Logged for " + name + "!", Toast.LENGTH_SHORT).show();

                    resetPromptUI();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToLeaderboard();
                    }
                })
                .setNegativeButton("SKIP", (dialog, which) -> {
                    saveScoreToLeaderboard("ANONYMOUS SURVIVOR", magnitude, gForce);
                    resetPromptUI();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToLeaderboard();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void resetPromptUI() {
        isWaitingForSurvivalCheck = false;
        disarmSensor();
        if (cardSurvivalPrompt != null) cardSurvivalPrompt.setVisibility(View.GONE);
    }

    private void disarmSensor() {
        isArmed = false;
        if (btnArmSensor != null) btnArmSensor.setText("ARM FOR TOSS");
        if (tvStateBadge != null) tvStateBadge.setText("STATUS: STANDBY");
    }

    private void saveScoreToLeaderboard(String playerName, float magnitude, float gForce) {
        Context ctx = getContext();
        if (ctx == null) return;

        String timestamp = new SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(new Date());
        LeaderboardDatabaseHelper db = LeaderboardDatabaseHelper.getInstance(ctx);
        db.addScore(playerName, magnitude, gForce, timestamp);
    }
}
