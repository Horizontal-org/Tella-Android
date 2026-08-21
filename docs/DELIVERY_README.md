# Tella-Android v5 (Topic 5 — Secure In-App Browser) — Delivery Package

**Build date:** 2026-08-21 (audit rev 13)
**Build target:** `:mobile:assembleFdroidDebug` (no minification — debug build)
**APK size:** ~61 MB
**APK package:** `org.hzontal.tellaFOSS` v3.2.0 (versionCode 248)
**APK signature:** Android Debug (auto-signed by `gradlew assembleFdroidDebug`)

## What's in this package

```
Tella-Android-v5/
├── DELIVERY_README.md        ← this file
├── Tella-Android-5-Topic-Audit.md   ← full audit doc (Topic 5 + .bin fix)
├── local.properties.template ← rename to local.properties, set your SDK path
├── build.gradle
├── settings.gradle
├── gradle.properties         ← already configured for 4GB sandbox
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── mobile/                   ← main app module (with browser + vault fix)
├── tella-vault/              ← vault core (Vault, RxVault, CipherStreamUtils)
├── tella-keys/               ← MainKey, key wrapping, unlock registry
├── tella-database/           ← SQLCipher data sources
├── tella-locking-ui/         ← PIN/Pattern/Password unlock UI
├── shared-ui/                ← UI primitives
├── pdfviewer/                ← Compose-based PDF viewer module
├── docs/, fastlane/, scripts/
├── artifacts/
│   ├── tella-android-v3.2.0-fdroid-debug.apk   ← THE BUILT APK
│   └── tella-vault-debug.aar                  ← built vault AAR
└── PDF_READER_AUDIT_REV7/8/9.md  ← prior Topic 4 audit docs (PDF reader)
```

## Topic 5 — what was added (rev 12 → rev 13)

Files added/modified in this delivery:

| File | Change | Purpose |
|---|---|---|
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/SecureBrowserActivity.kt` | NEW (rev 12) + KDoc updated (rev 13) | Activity host — sandboxed WebView, SSL hard-cancel, forensic cleanup on destroy. |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/browser/VaultDownloadInterceptor.kt` | NEW (rev 12) + MAJOR REWRITE (rev 13) | Download pipeline (fetch → sniff → import → secure delete). 812 lines. |
| `mobile/src/main/res/layout/activity_secure_browser.xml` | NEW | Toolbar + URL bar + back/forward/home + progress + WebView. |
| `mobile/src/main/res/menu/attachments_menu.xml` | MODIFIED | Adds `menu_item_browser` to the Attachments toolbar. |
| `mobile/src/main/AndroidManifest.xml` | MODIFIED | Registers `SecureBrowserActivity` (line ~638). |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/fragment/vault/attachements/AttachmentsFragment.kt` | MODIFIED | `R.id.menu_item_browser` handler launches the browser. |
| `mobile/src/main/java/org/horizontal/tella/mobile/media/MediaFileHandler.java` | MODIFIED | Adds `importDownloadedFile(File, String, String, String)` — vault-ingestion hookpoint (rev 12). |
| `mobile/src/main/res/values/strings.xml` | MODIFIED | Adds `browser_*` strings + `browser_download_renamed` (rev 13). |
| `Tella-Android-5-Topic-Audit.md` | NEW | Comprehensive audit doc (rev 13). |

## The `.bin` extension bug fix (rev 13)

**Symptom:** PDFs downloaded from Google Drive were saved into the vault as `*.bin`,
while PDFs from GitHub kept their correct `.pdf` extension.

**Root cause:** The previous implementation delegated filename guessing to
`URLUtil.guessFileName()`, which falls back to `.bin` when:
1. The URL has no extension (Google Drive: `?id=…`),
2. The server sends `Content-Type: application/octet-stream`,
3. The `Content-Disposition` filename is missing or is itself `.bin`.

**Fix:** A four-stage resolver (`resolveFileNameAndMime`) parses Content-Disposition
per RFC 6266 + RFC 5987, extracts from URL path, maps MIME→extension — followed by
a magic-byte sniffer (`sniffMimeFromMagic`) that inspects the actual downloaded bytes
to determine the real file type (PDF, PNG, JPEG, MP3, MP4, ZIP, etc.). When the
first-pass filename has a "weak" extension (`.bin`, `.download`, none), the sniffed
type overrides both the filename extension and the MIME that gets passed to the vault.

See `Tella-Android-5-Topic-Audit.md` §3 for the full root-cause analysis and §9 for
the conclusion.

## Rebuilding the APK on your machine

1. Install Android Studio or just the Android SDK + JDK 17.
2. Copy `local.properties.template` to `local.properties` and set `sdk.dir=…`.
3. Run:
   ```bash
   ./gradlew :mobile:assembleFdroidDebug
   ```
   Output: `mobile/build/outputs/apk/fdroid/debug/mobile-fdroid-debug.apk`

For a release build (with minification):
```bash
./gradlew :mobile:assembleFdroidRelease
# (requires release keystore in ~/.gradle/gradle.properties — see audit doc §5.3)
```

For the Play Store flavor (with Firebase Crashlytics + Google Drive + Dropbox):
```bash
./gradlew :mobile:assemblePlaystoreDebug
# (requires a google-services.json in mobile/ — not included here)
```

## Security posture summary (per spec)

✅ Tech stack: Kotlin + native Android `WebView` only (no Chrome Custom Tabs, no GeckoView, no new third-party dependencies)
✅ Sandboxing: `allowFileAccess=false`, `allowContentAccess=false`, `allowFileAccessFromFileURLs=false`, `allowUniversalAccessFromFileURLs=false`
✅ SSL hard-cancel: `handler.cancel()` never `handler.proceed()` (no MITM risk)
✅ Forensic cleanup on `onDestroy()`: history, cache, cookies, DOM storage wiped
✅ No `DownloadManager` (uses `HttpURLConnection` directly)
✅ No public external storage (uses `context.noBackupFilesDir` only)
✅ Vault integration via existing `RxVault.builder(stream)` + `CipherStreamUtils.getEncryptedOutputStream` — no custom crypto
✅ Temp file securely deleted (overwrite with `SecureRandom` + delete) after vault ingestion

See `Tella-Android-5-Topic-Audit.md` §2 for the full security posture and §6 for the
forensic verification checklist (re-run before each release).
