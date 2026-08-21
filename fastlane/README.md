# Fastlane for Tella Android

Run all commands from the **repository root** (the directory that contains `fastlane/` and `mobile/`).

## Lanes

- **`fastlane playstore`** — Build the Play Store release Android App Bundle (AAB) for `org.hzontal.tella`.
- **`fastlane fdroid`** — Build the F-Droid release APK for `org.hzontal.tellaFOSS` with `-Pfdroid` (Firebase/Crashlytics not applied).
- **`fastlane test`** — Run unit tests.

## Signing

Release builds use the same signing configuration as Gradle. Do not commit keystores or passwords. Configure signing via **environment variables** or **project properties** (e.g. in `gradle.properties` or CI):

- `RELEASE_STORE_FILE` — path to the keystore file (e.g. `mobile/release/upload-keystore.jks`)
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

If these are set, `mobile/build.gradle` applies them to the release build type. Do not commit keystores or passwords; use CI secrets or a local `gradle.properties` that is not in version control.

## Metadata

The `metadata/android/` directory was copied from [Tella-Android-FOSS](https://github.com/Horizontal-org/Tella-Android-FOSS) and is aimed at the F-Droid listing (org.hzontal.tellaFOSS). For the Play Store app (org.hzontal.tella), update or maintain store listing content as needed (e.g. in Play Console or by adjusting the metadata here for `supply`).
