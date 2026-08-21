# Tella Android — PDF Reader UI/UX Audit & Fix Log (Revision 9)

**Audit date:** 2026-08-20
**Scope:** PDF scrollbar, page-jump, share 0KB, settings blank area, rotation
**Revision:** audit-fix rev 9 (on top of rev 8)
**Previous audits:** see `Tella_PDF_Reader_Audit_Rev8.md` for rev 8

---

## 1. Executive Summary

The user reported four additional issues after rev 8. All were confirmed by code inspection and fixed:

| # | User report | Root cause | Fix |
|---|---|---|---|
| 1 | "No scrollbar on the right side for fast scroll" + "starting highlight/sticky picker first time jumps to page 1" | (a) `pdf_rendererview.xml` had `android:scrollbars="vertical"` but `fadeScrollbars` defaulted to `true` — the bar faded out 400ms after scrolling stopped. No custom thumb = barely visible. (b) `PdfRendererView.init()` scheduled a `postDelayed({ scrollToPosition(restoredScrollPosition) }, 300)` where `restoredScrollPosition` defaulted to 0 for first-open PDFs → scrolled to page 1 ~300ms after open. | (a) Added `fadeScrollbars="false"`, `scrollbarSize="10dp"`, custom `pdf_scrollbar_thumb` (orange rounded) + `pdf_scrollbar_track` (faint white) drawables. (b) Guarded both `postDelayed` scroll calls with `savedPage > 0` check — skip the scroll entirely for page 0 (the default start). |
| 2 | "Share with annotations produces 0KB file, social media says 'not a document'" | `EncryptedFileProvider.openFile()` intercepts ALL content URIs and spawns a `ReadThread` that calls `rxVault.getStream(filename)`. The flattened PDF lives in `cacheDir`, NOT in the encrypted vault → `VaultException` → thread bails with 0 bytes. Also: `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` was used instead of `FLAG_GRANT_READ_URI_PERMISSION` (the persistable flag doesn't grant read access). Also: `PdfAnnotationFlattener` tried to load `fonts/Helvetica.ttf` from assets (doesn't exist) → font was null → sticky note text never rendered. | (1) Created new `PlainFileProvider.java` (stock FileProvider, no `openFile()` override) with separate authority `${applicationId}.PlainFileProvider` + `plain_file_paths.xml`. (2) Registered in AndroidManifest. (3) Updated `sharePdfWithAnnotations` to use the new authority + `FLAG_GRANT_READ_URI_PERMISSION`. (4) Changed `PdfAnnotationFlattener` to use `PDType1Font.HELVETICA` (built-in standard 14 font, no asset needed). |
| 3 | "Settings > Security — blank/black area below Quick Delete PIN row, not visible" | `rounded_light_purple_background.xml` uses `<solid android:color="#10FFFFFF"/>` fill. The `InfoSettingsView` has 16dp internal bottom padding, so the LinearLayout's measured height exceeded its visible content → the solid fill drew a visible "blank card" rectangle. Also `android:animateLayoutChanges="true"` could leave animated gaps. | (1) Created new `audit_security_card_background.xml` with transparent fill + 1dp border (so any gap below the last row is invisible). (2) Applied to both `layout/` and `layout-hdpi/`. (3) Set `android:animateLayoutChanges="false"`. |
| 4 | "Rotation not working — app always stays portrait, PDF shows landscape 1 sec then reverts to portrait" + "add a rotate button" | `BaseActivity.onCreate()` unconditionally calls `requestedOrientation = SCREEN_ORIENTATION_PORTRAIT` unless `isManualOrientation == true`. `PDFReaderActivity` inherited this → forced back to portrait ~1 sec after opening (the `setRequestedOrientation` call is asynchronous). | (1) Set `isManualOrientation = true` before `super.onCreate()` in `PDFReaderActivity` → `BaseActivity` skips the portrait lock. (2) Added `menu_item_pdf_rotate` to the toolbar menu (uses existing `@drawable/rotate_image` icon). (3) Added `cycleOrientation()` method that cycles through Auto → Portrait → Landscape → Auto with a toast. |

---

## 2. Files Changed in Rev 9

### 2.1 New files

| File | Purpose |
|---|---|
| `pdfviewer/src/main/res/drawable/pdf_scrollbar_thumb.xml` | Orange rounded rectangle (10dp wide, `#CCD6933B`) for the PDF scrollbar thumb. |
| `pdfviewer/src/main/res/drawable/pdf_scrollbar_track.xml` | Faint white track (`#22FFFFFF`) for the PDF scrollbar. |
| `mobile/src/main/java/org/horizontal/tella/mobile/data/provider/PlainFileProvider.java` | Stock `FileProvider` subclass with NO `openFile()` override — serves cache files directly from disk. Used for sharing the flattened (annotation-baked) PDF. |
| `mobile/src/main/res/xml/plain_file_paths.xml` | FileProvider paths config exposing only the cache dir. |
| `mobile/src/main/res/drawable/audit_security_card_background.xml` | Transparent-fill + 1dp border card background for the audit_security_layout — eliminates the "blank area" below the last row. |
| `mobile/src/main/java/org/horizontal/tella/mobile/mvp/presenter/CheckTUSServerPresenter.java` | Stub implementation of the presenter (the original was broken — imported a non-existent `TUSClient` class). |

### 2.2 Modified files

| File | Change |
|---|---|
| `pdfviewer/src/main/res/layout/pdf_rendererview.xml` | Added `fadeScrollbars="false"`, `scrollbarFadeDuration="0"`, `scrollbarDefaultDelayBeforeFade="0"`, `scrollbarSize="10dp"`, `scrollbarThumbVertical="@drawable/pdf_scrollbar_thumb"`, `scrollbarTrackVertical="@drawable/pdf_scrollbar_track"`, `scrollbarStyle="outsideOverlay"`. |
| `pdfviewer/src/main/java/com/horizontal/pdfviewer/PdfRendererView.kt` | Guarded the `postDelayed` scroll calls in `init()` and `restoreFromPersistentState()` with `savedPage > 0` check — no more jump to page 1 on first open. |
| `pdfviewer/src/main/java/com/horizontal/pdfviewer/annotations/PdfAnnotationFlattener.kt` | Changed font from `PDType0Font.load(doc, assets.open("fonts/Helvetica.ttf"))` (asset doesn't exist → null font) to `PDType1Font.HELVETICA` (built-in standard 14 font). Updated `drawStickyNote` + `wrapText` parameter types from `PDType0Font?` to `PDFont?` (base class). Removed unused `PDType0Font` import. |
| `mobile/src/main/AndroidManifest.xml` | Registered the new `PlainFileProvider` with authority `${applicationId}.PlainFileProvider` + `@xml/plain_file_paths`. |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PDFReaderActivity.kt` | (1) Set `isManualOrientation = true` before `super.onCreate()`. (2) Added `cycleOrientation()` method. (3) Added `menu_item_pdf_rotate` handler. (4) Updated `sharePdfWithAnnotations` to use `PlainFileProvider` authority + `FLAG_GRANT_READ_URI_PERMISSION` + check `flattenedFile.length() > 0`. |
| `mobile/src/main/res/menu/pdf_annotation_menu.xml` | Added `menu_item_pdf_rotate` item with `@drawable/rotate_image` icon. |
| `mobile/src/main/res/layout/fragment_security_settings.xml` | Changed `audit_security_layout` background from `rounded_light_purple_background` to `audit_security_card_background`. Set `animateLayoutChanges="false"`. |
| `mobile/src/main/res/layout-hdpi/fragment_security_settings.xml` | Same changes as `layout/`. |
| `mobile/src/main/res/values/strings.xml` | Added `pdf_annot_title_rotate`, `pdf_rotate_portrait`, `pdf_rotate_landscape`, `pdf_rotate_auto`. |

---

## 3. Root-Cause Deep Dives

### 3.1 The scrollbar was invisible

`pdf_rendererview.xml` already had `android:scrollbars="vertical"` on the `PinchZoomRecyclerView`. But:

1. `android:fadeScrollbars` defaults to `true` — the bar disappears ~400ms after the user stops scrolling. The user only sees it during active dragging.
2. The default thumb is 5dp wide with a default-color (light gray) that's nearly invisible on the dark purple window background.
3. `PinchZoomRecyclerView` is a custom `RecyclerView` that handles pinch-zoom — its `onInterceptTouchEvent` may prevent the framework from continuing to render the scrollbar thumb once the user lifts their finger.

**Fix:** Added `fadeScrollbars="false"` (always visible), `scrollbarSize="10dp"` (thicker), custom `pdf_scrollbar_thumb` (orange `#CCD6933B`, 5dp radius), custom `pdf_scrollbar_track` (faint white `#22FFFFFF`), and `scrollbarStyle="outsideOverlay"` (doesn't shrink the page content).

### 3.2 The page-1 jump on first picker open

**Trace:**
1. `PDFReaderActivity.onCreate` → `displayFromUri` → `initWithStream(stream, fileId)`.
2. `PdfRendererView.initWithStream` calls `init(fileDescriptor)` then `restoreFromPersistentState()`.
3. `init()` schedules `postDelayed({ scrollToPosition(restoredScrollPosition) }, 300)`.
4. For a first-open PDF, `PdfReadingStateStore.getLastPage()` returns 0 → `restoredScrollPosition = 0`.
5. At ~300ms, `scrollToPosition(0)` fires — the PDF jumps to page 1.
6. This happens exactly when the user is tapping the toolbar to open the highlight/sticky picker for the first time.

**Fix:** Guarded both `postDelayed` calls:
- In `init()`: only scroll if `restoredScrollPosition != NO_POSITION && restoredScrollPosition > 0`.
- In `restoreFromPersistentState()`: only post the scroll if `savedPage > 0` — return early for page 0.

### 3.3 The 0KB share file

**Primary bug — `EncryptedFileProvider.openFile()` intercepts all URIs:**

```java
@Override
public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    ParcelFileDescriptor pfd = super.openFile(uri, mode);
    ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
    if ("r".equals(mode)) {
        new ReadThread(uri.getLastPathSegment(),
                new AutoCloseInputStream(pfd),
                new AutoCloseOutputStream(pipe[1])).start();
        return pipe[0];
    }
    ...
}

private static class ReadThread extends Thread {
    public void run() {
        try {
            RxVault rxVault = MyApplication.keyRxVault.getRxVault().blockingFirst();
            cipherInputStream = rxVault.getStream(filename);  // looks up file IN THE ENCRYPTED VAULT
        } catch (VaultException | RuntimeException e) {
            return;  // ← BAILS OUT: zero bytes written to the pipe
        }
        ...
    }
}
```

The flattened PDF lives in `context.cacheDir`, NOT in the vault. `rxVault.getStream("document_annotated.pdf")` throws `VaultException` → the thread returns having written 0 bytes → the recipient app sees a 0-byte file.

**Fix:** Created a new `PlainFileProvider` that is a stock `FileProvider` with NO `openFile()` override. It serves files directly from disk via the framework's default content:// machinery. Registered with a separate authority `${applicationId}.PlainFileProvider` and `@xml/plain_file_paths` (cache-path only).

**Secondary bug — wrong permission flag:**

```kotlin
chooser.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
```

`FLAG_GRANT_PERSISTABLE_URI_PERMISSION` only marks the URI as persistable — it does NOT grant read access. Changed to `FLAG_GRANT_READ_URI_PERMISSION`.

**Tertiary bug — missing font:**

```kotlin
val font = try {
    PDType0Font.load(doc, context.assets.open("fonts/Helvetica.ttf"))
} catch (_: Throwable) { null }
```

The `fonts/Helvetica.ttf` asset doesn't exist in the project → the catch block returned null → sticky note text never rendered. Changed to `PDType1Font.HELVETICA` (one of the 14 standard PDF fonts, always available, no asset needed).

### 3.4 The settings blank area

**Root cause:** `rounded_light_purple_background.xml` uses `<solid android:color="#10FFFFFF"/>` — a semi-transparent white fill. The `InfoSettingsView` (used for `quickDeletePinSetting`) has 16dp internal bottom padding (from `settings_info_view.xml`). So the LinearLayout's measured height = content height + 16dp, and the solid fill drew a visible "blank card" rectangle in that 16dp gap.

**Fix:** Created `audit_security_card_background.xml` with:
- `<solid android:color="@android:color/transparent"/>` — no fill, so any gap is invisible (shows the parent's dark_purple background).
- `<stroke android:width="1dp" android:color="#22FFFFFF"/>` — subtle 1dp border so the card still has a visible edge.

Applied to both `layout/` and `layout-hdpi/`. Also set `android:animateLayoutChanges="false"` to prevent animated gaps.

### 3.5 The rotation lock

**Root cause:** `BaseActivity.onCreate()` lines 90-92:

```kotlin
if (!isManualOrientation && !resources.getBoolean(R.bool.isTablet)) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
```

Every activity extending `BaseActivity` (including `PDFReaderActivity`) is locked to portrait at runtime. `isManualOrientation` defaults to `false` and only `SignatureActivity` sets it to `true`.

The manifest for `PDFReaderActivity` declares `android:configChanges` with `orientation` — so the activity isn't recreated on rotation. But `BaseActivity.onCreate()` calls `setRequestedOrientation(PORTRAIT)` which is asynchronous — the system rotates back to portrait ~1 sec after the activity opens.

**Fix:** Set `isManualOrientation = true` before `super.onCreate()` in `PDFReaderActivity`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    isManualOrientation = true  // ← stops BaseActivity from forcing portrait
    super.onCreate(savedInstanceState)
    ...
}
```

Now the PDF reader follows the device's actual orientation. The existing `onConfigurationChanged` handler reflows the pages on rotation.

**Manual rotate button:** Added `menu_item_pdf_rotate` to the toolbar menu (icon: `@drawable/rotate_image`, an existing white 24dp vector). The `cycleOrientation()` method cycles through:
- `SCREEN_ORIENTATION_UNSPECIFIED` (auto-rotate, follow device)
- `SCREEN_ORIENTATION_PORTRAIT`
- `SCREEN_ORIENTATION_LANDSCAPE`

with a toast showing the current mode.

---

## 4. Build Notes

### 4.1 Pre-existing broken source fixed

The original `CheckTUSServerPresenter.java` imported `org.horizontal.tella.mobile.data.upload.TUSClient` which does not exist anywhere in the source tree. Both the fdroid and playstore flavors of `TellaUploadServerDialogFragment.kt` reference this presenter. Created a stub implementation that satisfies the contract (no-op `checkServer` that reports failure, no-op `destroy`) so the project compiles. The TUS server-check feature won't work, but the app builds and all other features are unaffected.

### 4.2 Debug APK built successfully

- **Flavor:** fdroid (no Firebase/Google Services dependency)
- **Build type:** debug
- **Minification:** disabled (`minifyEnabled false`, `shrinkResources false`)
- **APK size:** 59 MB
- **Location:** `/home/z/my-project/download/Tella-debug-rev9.apk`

Build environment:
- Android SDK 36 + build-tools 36.0.0
- JDK 17 (Eclipse Temurin 17.0.20)
- 4GB RAM sandbox (build required staged compilation due to memory constraints)

---

## 5. Delivery

### 5.1 APK

`/home/z/my-project/download/Tella-debug-rev9.apk` (59 MB) — ready to install.

### 5.2 Codebase zip

`/home/z/my-project/download/Tella-Android-pdf-reader-fixes-rev9.zip` — the full patched codebase with all rev 7 + rev 8 + rev 9 fixes applied.

### 5.3 This audit document

`/home/z/my-project/download/Tella_PDF_Reader_Audit_Rev9.md` (this file).

---

End of audit rev 9.
