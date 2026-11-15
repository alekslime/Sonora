# SONORA

A premium, offline-first Android music player built around the feeling of
physical records. Kotlin + Jetpack Compose. No accounts, no network, no
tracking.

## Milestones

- M0 — Brand and design system. (done)
- M1 — Shelf prototype (mock library, perspective stack, album detail). (done)
- M2 — Now Playing: clean player (artwork, shuffle/repeat, transport). (done)
- M3 — Liquid visualizer (AGSL shader + Canvas fallback, presets, touch-to-exit). (done)
- M4 — Audio Studio (10-band EQ, presets, ReplayGain, gapless/crossfade, output info) + three-finger gesture system. (done)
- M5 — Real local music: MediaStore scan, ExoPlayer engine, embedded art, real EQ/preamp/gapless, real visualizer FFT, saved prefs, demo fallback. (done)
- M6 — Background service, notification/lock-screen controls, ReplayGain parsing, crossfade, format details. (done)

## Build

Requirements: JDK 17, Android SDK with platform 34 and build-tools 34.0.0.

```sh
./gradlew assembleDebug
```

Install on a connected device or emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run unit tests:

```sh
./gradlew testDebugUnitTest
```

## Project layout

- `app/src/main/java/com/sonora/theme/` — M0 design system.
- `app/src/main/java/com/sonora/data/` — models, mock + MediaStore libraries, artwork pipeline, prefs.
- `app/src/main/java/com/sonora/shelf/` — Shelf layout, sleeves, state.
- `app/src/main/java/com/sonora/detail/` — album detail.
- `app/src/main/java/com/sonora/nowplaying/` — Now Playing screen and transport state.
- `app/src/main/java/com/sonora/playback/` — M6 background service, controller bridge, orchestrator, crossfade, formats.
- `app/src/main/java/com/sonora/liquid/` — Liquid visualizer, presets, mock + real spectrum.
- `app/src/main/java/com/sonora/studio/` — Audio Studio config, screen, M5 effects bridge.
- `app/src/main/java/com/sonora/gesture/` — three-finger state machine, layer and config.
- `app/src/main/java/com/sonora/browse/` — compact list/grid browsing, library gate.
- `app/src/main/java/com/sonora/components/` — shared composables.
