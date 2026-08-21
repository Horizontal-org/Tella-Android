# F-Droid release verification

Before shipping a build to F-Droid, run these checks on the **fdroidRelease** APK.

The strongest setup is: (1) correct flavor-specific dependencies and `-Pfdroid` in the F-Droid recipe, (2) this script as an extra guard, (3) one real device sanity test. Passing the script does **not** automatically prove there are no runtime references to Firebase, GMS, or Dropbox—it only checks that those package paths are not visibly present in the APK archive (e.g. after shrinking/repackaging, some cases could differ). Treat it as a verification helper, not absolute proof.

## 1. Automated checks (script)

From the repo root:

```bash
./scripts/verify-fdroid-release.sh
```

This builds `assembleFdroidRelease` with `-Pfdroid` and inspects the APK(s) for:

- **No Firebase** — no `com/google/firebase`
- **No Google Play Services** — no `com/google/android/gms`
- **No Dropbox** — no `com/dropbox` (package-based matching)

If any match is found, the script exits with status 1. If no APK is found after build, the script also fails. To skip the build and only inspect existing APKs:

```bash
./scripts/verify-fdroid-release.sh --no-build
```

The script uses `unzip -l` to check archive entry paths; it is a first-pass check. For deeper inspection, use APK Analyzer (see below).

## 2. Optional: APK Analyzer

Use Android Studio’s **Build > Analyze APK** or the SDK `apkanalyzer` tool to confirm the same. Open the fdroid release APK and ensure no Firebase, GMS, or Dropbox packages are present.

## 3. Sanity test on device

Install the fdroid release build on a device and:

- Open vault, connections, and settings.
- Confirm there are no crashes.

This helps ensure no shared code path in the F-Droid variant loads Firebase, GMS, or Dropbox at runtime.

## Build note

The F-Droid recipe must pass **-Pfdroid** so that Firebase/Crashlytics plugins are not applied and the correct dependencies are used. The script and Fastlane `fdroid` lane use this property.
