# SaveSwitcher
SaveSwitcher is an Android app for managing per-user emulator saves by swapping the active save file for a game. Designed to work on small screens e.g. the secondary screen on the Ayn Thor.

> [!WARNING]
> **This app was vibe-coded with Codex and performs real file operations on emulator saves.**
> **Always back up your saves before using it.** Save switching can overwrite, rename, or remove files.

I’m happy to accept issues and pull requests, but this is a quick spare-time project. I can’t guarantee active maintenance. It may be better to fork the project.

This app relies on save files being named after the game/rom title (as Retroarch does), so won't work for emulators like Citra that do something else.



## What It Does

- Register multiple emulators (name + save folder + extensions)
- Register multiple users
- Recursively scan save directories via Android SAF
- Detect game save groups:
  - base save: `game.ext`
  - user variant: `game_user.ext`
- Switch active user per game:
  - archives current base save to the current owner variant
  - restores target user variant as base if present
  - otherwise removes base so emulator can create a fresh save
- Store emulator/user/history data in Room (persists across app restarts)
- Order games by most recently switched first

## Current Status

This is an MVP and still rough around the edges.

Implemented:
- Emulator setup via folder picker (SAF)
- User setup
- Save scanning
- Save switching flow
- History tracking
- Persistent local database for core app data

Not yet implemented / limited:
- Advanced conflict handling and rollback UI
- Emulator-specific bundle/file-family handling
- Import/export/backup helper inside app
- Full test coverage

## Platform

- Android min SDK: 29 (Android 10+)
- Works on Android 13 (API 33)
- Built with Kotlin + Jetpack Compose + Room

## Build (Local)

Prerequisites:
- JDK 17
- Android SDK installed and configured (`sdk.dir` in `local.properties`)

Example:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
GRADLE_USER_HOME=$PWD/.gradle-home \
./gradlew assembleDebug
```

Debug APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

## Usage Notes

1. Add at least one user.
2. Add emulator profile(s) and choose each save folder.
3. Open `Games` and tap `Scan Saves`.
4. Choose a game and tap `Switch User`.
5. Confirm owner/target user.

Before using with real saves:
- Make a full backup copy of your save folders.
- Test with throwaway saves first.

## Data and Privacy

- No cloud sync in MVP
- No network service required for app functionality
- Metadata is stored locally in SQLite (Room)
- Save files remain in your selected storage location

## Contributing

Issues and PRs are welcome.

Please keep in mind:
- This repository is not guaranteed to be actively maintained.
- If you need guaranteed progress/features, forking may be the best option.

If you open a PR, include:
- what changed
- why it changed
- how you tested it
