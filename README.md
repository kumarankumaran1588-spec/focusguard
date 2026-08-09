# FocusGuard — Android App Blocker with Uninstall Protection

A self-control app that blocks apps you choose and refuses to be uninstalled
until you enter your password. Built in Kotlin.

## What it does
- Pick any launchable app to block.
- Opening a blocked app shows a full-screen "blocked" wall.
- Password-gated 5-minute temporary unlock (your escape hatch for real needs).
- Uninstall protection via Device Admin — the app can't be removed until you
  enter the password and disable protection.
- Tamper protection — opening this app's system "App info" / uninstall / device
  admin screen triggers a password prompt first.
- Password stored as a salted SHA-256 hash inside EncryptedSharedPreferences.

## How it works (the three Android mechanisms)
1. **Accessibility Service** watches which app comes to the foreground. This is
   the only reliable, modern way to detect app launches. (`AppBlockerService.kt`)
2. **SYSTEM_ALERT_WINDOW / full-screen activity** draws the block wall.
3. **Device Admin** is what stops Android from uninstalling the app while active.

## Build it
1. Install **Android Studio** (Hedgehog or newer).
2. `File > Open` this folder. Let Gradle sync (it downloads the Gradle 8.7
   wrapper and dependencies automatically — needs internet the first time).
3. Plug in your phone with **USB debugging** on, or use an emulator.
4. Press **Run**. On first launch, grant in this order:
   set password -> enable accessibility -> allow display-over-other-apps ->
   enable uninstall protection -> select apps to block.

> The project ships without the `gradlew` wrapper JAR (a binary). Android Studio
> regenerates it on first sync. If you build purely from the command line, run
> `gradle wrapper` once first (needs a local Gradle install).

## Ship it to Google Play — read this before you start
This app type triggers **three** review flags. None are blockers, but each needs
a declaration, and skipping them gets the app rejected:

1. **Accessibility API use.** Google requires a "Permissions declaration" plus an
   in-app **prominent disclosure** explaining that the Accessibility Service is
   used only to enforce your blocks. The description string is already written in
   `strings.xml` (`accessibility_description`). Record a short demo video for the
   review team showing the block in action — they almost always ask for one.

2. **QUERY_ALL_PACKAGES.** Listing every installed app so the user can choose one
   requires a declaration form justifying it. "Core functionality: user selects
   which installed apps to block" is the accepted justification for blockers.

3. **Device Admin for anti-uninstall.** This is the sensitive one. Google
   restricts device-admin apps and has removed some that block their own
   uninstall. Published blockers (e.g. Lock Me Out) do ship this, but be ready
   to: clearly state it's a user-consented self-control feature, make removal
   possible via the in-app password (this app does), and expect extra scrutiny.
   If review pushes back, you can ship a first version *without* device admin and
   rely on accessibility-based tamper blocking alone.

Also required for any new listing: a privacy policy URL, the Data Safety form
(this app collects/transmits nothing — declare exactly that), target API level
compliance (currently 34), and a signed release **AAB**:
`Build > Generate Signed Bundle / APK > Android App Bundle`. Keep your keystore
safe — losing it means you can't update the app.

## Honest limits of "uninstall protection"
No app-level lock is unbreakable. A determined user can boot into **Safe Mode**
(third-party services are disabled there), use **ADB**, or **factory reset**.
Device Admin + accessibility tamper-blocking defeats casual/impulsive removal,
which is the actual goal of a self-control tool — not a hostile attacker.

## File map
- `service/AppBlockerService.kt` — foreground detection + enforcement (core)
- `admin/BlockerDeviceAdminReceiver.kt` — uninstall protection
- `data/PrefsManager.kt` — encrypted password + block list
- `ui/MainActivity.kt` — setup dashboard
- `ui/AppSelectionActivity.kt` + `AppListAdapter.kt` — pick apps
- `ui/PasswordActivity.kt` — set/change/verify/disable-admin
- `ui/BlockOverlayActivity.kt` — the block wall

## Build the APK in the cloud (no Android Studio needed)
This repo includes `.github/workflows/build-apk.yml`, which builds the app on
GitHub's servers and hands you a ready-to-install APK.

1. Create a free GitHub account and a new repository.
2. Upload this project to it (drag-and-drop the files in the GitHub web UI, or
   `git push` if you use git).
3. Open the **Actions** tab. The build starts automatically on upload; you can
   also press **Run workflow**.
4. When it finishes (green tick, ~3–5 min), open the run and download the
   **FocusGuard-debug-apk** artifact from the "Artifacts" section.
5. Copy the `.apk` to your phone, tap it, and allow "install from unknown
   sources" when prompted.

This produces a **debug** APK — perfect for installing on your own phone. To
publish on the Play Store you still need a **signed release AAB** (see the Play
Store section above), which requires your own keystore.
