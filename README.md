# SONORA

A premium, offline-first Android music player built around the feeling of
physical records. Kotlin + Jetpack Compose. No accounts, no network, no
tracking.

## Milestones

- M0 — Brand and design system. (done)
- M1 — Shelf prototype (mock library, perspective stack, album detail). (done)
- M2 — Now Playing prototype (vinyl, tonearm, prism, simulated playback). (done)
- M3 — Liquid visualizer prototype. (planned)
- M4 — Audio Studio and gestures. (planned)
- M5 — Real local music functionality. (planned)
- M6 — Android integration and polish. (planned)

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
- `app/src/main/java/com/sonora/data/` — models, mock library, artwork pipeline.
- `app/src/main/java/com/sonora/shelf/` — Shelf layout, sleeves, state.
- `app/src/main/java/com/sonora/detail/` — album detail.
- `app/src/main/java/com/sonora/nowplaying/` — Now Playing stage and mock transport.
- `app/src/main/java/com/sonora/browse/` — compact list/grid browsing.
- `app/src/main/java/com/sonora/components/` — shared composables.
