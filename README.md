# Phone Toss Survival Challenge

<div align="center">
  <img src="app/src/main/res/drawable/ic_death_metal_logo.png" width="250" alt="Phone Toss Survival Artwork" />
</div>

## Overview

Phone Toss Survival is an Android application built for real-time motion telemetry. It uses the native Android `SensorManager` framework (`Sensor.TYPE_ACCELEROMETER`).

The app measures 3-axis acceleration forces during phone tosses or drops. It calculates peak impact G-forces and triggers a 60-second survival verification protocol. High scores are logged to a native SQLite database and Google Play Games Services V2.

---

## Release v1.0 Download

- **Release Package:** [PhoneTossSurvival-v1.0-release.apk](PhoneTossSurvival-v1.0-release.apk)
- **GitHub Release Tag:** [v1.0 Release Page](https://github.com/Azteriisk/PhoneTossSurvival/releases/tag/v1.0)
- **Package Size:** 4.22 MB
- **Target SDK:** 35 (Android 15)
- **Min SDK:** 24 (Android 7.0+)

---

## Technical Architecture

The application adopts a modular, component-based Android architecture utilizing Jetpack libraries, Google Play Services, and native hardware APIs.

- **Activity Host:** `MainActivity` hosts a `ViewPager2` container. A custom `FragmentStateAdapter` delivers an endless carousel swipe interface between views.
- **Hardware Telemetry:** `GameFragment` implements `SensorEventListener` to process accelerometer updates from `SensorManager`.
- **Global Leaderboard Integration:** `PlayGamesLeaderboardManager` integrates Google Play Games Services V2 SDK (`com.google.android.gms:play-services-games-v2:22.1.0`) for global score tracking.
- **Database Persistence:** `LeaderboardDatabaseHelper` provides a native SQLite database (`phonetoss_leaderboard.db`) to store and query scores locally.
- **Navigation:** Page transitions use a 3-dot indicator bar with real-time scaling and glow pulse feedback.

---

## Hardware Telemetry and Mathematics

The continuous 3-axis linear acceleration vector $(X, Y, Z)$ reported by the accelerometer is processed asynchronously in `onSensorChanged()`:

### 1. Vector Magnitude Calculation
$$\text{Magnitude} = \sqrt{X^2 + Y^2 + Z^2}$$

### 2. Equivalent G-Force Conversion
$$G = \frac{\text{Magnitude}}{9.80665}$$

Where $9.80665 \text{ m/s}^2$ represents standard Earth gravity ($1.0 \text{ G}$).

---

## Key Features

- **Google Play Games Services V2 Integration:** Official Google Play Games SDK integration for global score submission and overlay leaderboard UI.
- **Real-Time Accelerometer Telemetry:** Live display of $X, Y, Z$ acceleration axes in meters per second squared.
- **Adjustable Sensitivity:** Interactive slider to set the throw trigger threshold ($15.0 \text{ m/s}^2$ default).
- **Arming Protocol:** Safety toggle button to arm the sensor prior to throwing.
- **Post-Impact Survival Check:** Automated 60-second countdown timer activated upon detecting high-G impact shocks.
- **Player Name Customization:** Interactive Material dialog prompting users to enter a player name or tag upon confirming survival.
- **SQLite Database Persistence:** Structured SQL storage (`_id`, `player_name`, `magnitude`, `g_force`, `timestamp`) sorted automatically by maximum force magnitude.
- **Infinite Carousel Navigation:** Continuous horizontal page swiping between Game and Leaderboard views.
- **Adaptive Icon and Vector Assets:** Modern adaptive launcher icons and vector assets optimized for all screen densities.

---

## SQLite Database Schema

High scores are persisted on-device in `phonetoss_leaderboard.db` using the following schema:

```sql
CREATE TABLE leaderboard (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT NOT NULL DEFAULT 'ANONYMOUS',
    magnitude REAL NOT NULL,
    g_force REAL NOT NULL,
    timestamp TEXT NOT NULL
);
```

---

## Build and Requirements

- **Android Studio:** Android Studio Jellyfish / Ladybug or newer
- **Compile SDK:** 35
- **Target SDK:** 35
- **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Language:** Java (JDK 11)
- **Gradle Version:** AGP 8.x / 9.x compatible

### Compilation Command

Execute the following command in the project root to compile the debug build:

```bash
./gradlew app:assembleDebug
```

---

## Testing Procedures

1. **Hardware Telemetry Verification:** Test using the Android Emulator or physical device. Force inputs can be tested via `adb shell`:
   ```bash
   adb emu sensor set acceleration 0:25:0
   ```
2. **Survival Timer Verification:** Verify that detecting an impact forces a 60-second countdown timer. Confirming survival prompts for player name entry; expiring without response forfeits the run.
3. **Database & Global Leaderboard Verification:** Verify that logged high scores appear in sorted order on the Leaderboard tab and submit to Google Play Games Services V2 API.

---

## License and Author

Developed as an Android hardware engineering demonstration. All rights reserved.
