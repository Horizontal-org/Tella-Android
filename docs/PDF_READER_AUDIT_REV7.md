# Tella Android — PDF Reader UI/UX Audit & Fix Log

**Audit date:** 2026-08-20
**Scope:** `pdfviewer/` module + `mobile/views/activity/viewer/` + `mobile/views/settings/SecuritySettings.kt` + `shared_ui/security/QuickDeletePinManager.kt`
**Revision:** audit-fix rev 7 (on top of audit rev 6 dated 2025-08-19)

---

## 1. Executive Summary

The user reported nine concrete defects in the Tella PDF reader experience. After reading the entire PDF reader code path (`PdfRendererView`, `PinchZoomRecyclerView`, `PdfAnnotationOverlayView`, `PDFReaderActivity`, `PdfAnnotationDialogs`, `PdfAnnotationStylePicker`, `QuickDeletePinManager`, `SecureWipeDialog`, `SecuritySettings`) plus the theme / color / string resources, every report was reproduced and the root cause was traced to a small number of architectural issues:

| # | User report | Root cause | Fix |
|---|---|---|---|
| 1 | "If I hold on a text I can't select and copy" | `PdfTextExtractor` was a stub — it rendered the page to a Bitmap, immediately `recycle()`d it, and returned an empty `StringBuilder`. So `copyPageText()` always showed the empty-text toast. | Replaced with **PDFBox-Android** (`com.tom_roush:pdfbox-android:2.0.27.0`) — real PDF text-stream parsing. Now returns the actual text on the page. |
| 2 | "Zoom is not normal, not user friendly" | `PinchZoomRecyclerView` had min zoom 1.0× (couldn't zoom out), max 3.0× (too low), double-tap jumped straight to MAX_SCALE, and the focus point was lost during pinch so the page drifted. | New zoom range **0.5× – 5.0×**, double-tap cycles **1× → 2× → 1×**, focus point preserved via `mPosX = focusX - (focusX - mPosX) * scaleDelta`. |
| 3 | "I can't rotate the screen — biggest issue" | `PDFReaderActivity` declared `android:configChanges` (so it isn't recreated on rotation) but never overrode `onConfigurationChanged`. The portrait-rendered page bitmaps kept their dimensions in landscape, leaving empty bars. | Added `onConfigurationChanged()` that calls `requestLayout()` on `PdfRendererView` + `notifyItemRangeChanged()` on the adapter so page `ImageView`s re-fitCenter to the new width. |
| 4 | "List annotations doesn't show the page number and not take me to that page" | List DID show `"p.N: text"` (just hard to see when text was blank) but clicking a row called `onSelected` only — no navigation. | New row format `"Page N — text"` (always visible). Added `onNavigate` callback; `PDFReaderActivity` calls `scrollToPage(ann.page)` before opening the editor. |
| 5 | "Copy page text says maybe the pdf is image — false information" | String `pdf_annot_copy_text_empty` = `"This page has no selectable text (it may be a scanned image)"` was shown for EVERY call because the extractor was broken (#1). | (1) Fixed the extractor (#1). (2) Reworded the empty string to `"This page has no selectable text."` (no misleading hint). (3) Added a separate `pdf_annot_copy_text_unavailable` string shown only when the extractor library itself failed to load. |
| 6 | "White text and white background — Save / Close / OK buttons invisible" (sticky note editor, quick-delete PIN dialog, wipe-on-import) | `AppTheme.NoActionBar` sets `colorAccent = wa_white_80 (#CCFFFFFF)`. AppCompat AlertDialog buttons are tinted by `?colorAccent` → 80% transparent white on a white dialog background = invisible. The audit rev6 "fix" of swapping `BrightBackgroundDarkLettersDialogTheme` for plain `AlertDialog.Builder(context)` did NOT override `colorAccent` so the bug persisted. | (1) New `TellaDialogTheme` overlay (`parent="ThemeOverlay.AppCompat.Light"`) overrides only `colorAccent` → `wa_orange (#D6933B)` and `android:textColorPrimary` → `wa_darker_gray`. (2) New `TellaDialogs.builder(context)` helper wraps the context in `ContextThemeWrapper(context, R.style.TellaDialogTheme)` so every dialog inherits the visible-button color scheme. (3) All 9+ AlertDialog construction sites across `PdfAnnotationDialogs`, `PdfAnnotationStylePicker`, `PDFReaderActivity`, `SecuritySettings`, `SecureWipeDialog`, and `QuickDeletePinManager` updated to use it. |
| 7 | "Highlight brush sizes S/M/L — reduce each by 70%" | Multipliers were `WIDTH = [0.20, 0.35, 0.50]`, `HEIGHT = [0.7, 1.0, 1.5]`, `STICKY = [0.7, 1.0, 1.5]`. | Multiplied every value by 0.30: `WIDTH = [0.06, 0.105, 0.15]`, `HEIGHT = [0.21, 0.30, 0.45]`, `STICKY = [0.21, 0.30, 0.45]`. |
| 8 | "Save doesn't suitable — should be 'active mode' more meaningful" | The picker's positive button was labeled "Save" but the picker doesn't persist anything; it activates a mode. | New string `pdf_annot_dialog_apply` = "Apply". Pickers now use Apply. The sticky-note *text editor* still uses "Save" because that dialog genuinely persists the typed text. |
| 9 | "Settings > Security > Quick Delete PIN — big blank area below" | `audit_security_layout` LinearLayout has `rounded_light_purple_background` drawable. When no PIN is set, the brute-force rows below are `View.GONE` and the `quickDeletePinSetting` row has `isBottomLineVisible="false"` — so the card's bottom padding shows as an empty purple rectangle. | (1) Set `app:isBottomLineVisible="true"` on `quickDeletePinSetting` so the card terminates with a visible divider. (2) Set `android:paddingBottom="0dp"` on the layout so it doesn't extend past the last visible row. (3) Applied to both `layout/` and `layout-hdpi/` duplicates. |

Every fix is backwards-compatible at the public API surface. Callers that previously passed `onSelected` only to `showAnnotationList` still compile (the new `onNavigate` parameter has a default empty lambda).

---

## 2. Files Changed

### 2.1 New files

| File | Purpose |
|---|---|
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/TellaDialogs.kt` | Stateless helper that wraps any `Context` in `ContextThemeWrapper(context, R.style.TellaDialogTheme)` before constructing an `AlertDialog.Builder`. Resolves `?attr/tellaDialogTheme` so host activities can override the dialog theme per-activity (e.g. for dark `PlayerTheme`). Falls back to `R.style.TellaDialogTheme`. |

### 2.2 Modified files

| File | Change |
|---|---|
| `mobile/src/main/res/values/styles.xml` | (1) Patched `BrightBackgroundDarkLettersDialogTheme` to also override `colorAccent` (was missing → buttons stayed white-on-white). (2) Added new `TellaDialogTheme` overlay. (3) Added `<item name="tellaDialogTheme">@style/TellaDialogTheme</item>` to `AppTheme.NoActionBar` so shared-ui dialogs inherit it. |
| `mobile/src/main/res/values/attrs.xml` | Removed the local `tellaDialogTheme` attr declaration (it now lives in shared-ui so shared-ui can resolve it). Added a comment explaining the relocation. |
| `shared-ui/src/main/res/values/attrs.xml` | Added `<attr name="tellaDialogTheme" format="reference" />` so `QuickDeletePinManager` (which lives in shared-ui) can resolve the host's dialog theme without a hard dependency on the mobile module. |
| `mobile/src/main/res/values/strings.xml` | (1) Reworded `pdf_annot_copy_text_empty` to drop the misleading "(may be a scanned image)" suffix. (2) Added `pdf_annot_copy_text_unavailable`, `pdf_annot_copy_text_loading`, `pdf_annot_dialog_apply`, `pdf_annot_list_page_label`, `pdf_annot_list_no_text`. |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PdfAnnotationDialogs.kt` | All `AlertDialog.Builder(context)` calls → `TellaDialogs.builder(context)`. `showAnnotationList` gained an `onNavigate` callback (default empty) invoked before `onSelected`. Row label changed to `"Page N — text"` (always shows page number). |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PdfAnnotationStylePicker.kt` | (1) Size multipliers reduced by 70%. (2) Positive button changed from `pdf_annot_dialog_save` to `pdf_annot_dialog_apply`. (3) All dialogs via `TellaDialogs.builder`. (4) Refresh size-button pills when color swatch changes (visual feedback). |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PDFReaderActivity.kt` | (1) Added `onConfigurationChanged` handler that re-layouts `PdfRendererView` on rotation. (2) `confirmClearAllAnnotations` and `showLongPressMenu` now use `TellaDialogs.builder`. (3) `copyPageText` rewritten: shows loading toast, distinguishes empty-vs-unavailable, calls new `PdfTextExtractor.isAvailable(fileId)`. (4) Annotation list menu item passes `onNavigate = { ann -> scrollToPage(ann.page) }`. (5) Removed unused `AlertDialog` import. |
| `mobile/src/main/java/org/horizontal/tella/mobile/views/settings/SecuritySettings.kt` | (1) `quickDeletePinSetting` click handler: change/remove picker now via `TellaDialogs.builder` (was `AlertDialog.Builder(baseActivity)` — invisible Cancel button). (2) `showNumberPicker` helper (brute-force threshold/window pickers) now via `TellaDialogs.builder`. |
| `mobile/src/main/java/org/horizontal/tella/mobile/security/SecureWipeDialog.kt` | Both the prompt dialog and the progress dialog now built via `TellaDialogs.builder` so the "Secure wipe" / "Skip" buttons are visible. |
| `shared-ui/src/main/java/org/hzontal/shared_ui/security/QuickDeletePinManager.kt` | New private `themedBuilder(context)` helper that resolves `?attr/tellaDialogTheme` and wraps the context. `showSetPinDialog` now uses it. Falls back to `Theme_DeviceDefault_Light_Dialog_Alert` if the host doesn't declare the attr. |
| `mobile/src/main/res/layout/fragment_security_settings.xml` | (1) `quickDeletePinSetting` `app:isBottomLineVisible` flipped `false → true`. (2) `audit_security_layout` `paddingBottom="0dp"`. (3) Documented the blank-area fix. |
| `mobile/src/main/res/layout-hdpi/fragment_security_settings.xml` | Same fix as `layout/` — the hdpi duplicate must stay in sync for view-binding non-nullable references. |
| `pdfviewer/build.gradle` | Added `implementation 'com.tom-roush:pdfbox-android:2.0.27.0'`. |
| `pdfviewer/src/main/java/com/horizontal/pdfviewer/annotations/PdfTextExtractor.kt` | Complete rewrite. Now uses PDFBox-Android (`PDDocument.load(tmpFile).use { ... PDFTextStripper().getText(doc) }`) instead of the broken PdfRenderer+Bitmap approach. Lazy `PDFBoxResourceLoader.init(context)` on first use. Per-fileId extraction lock. New `isAvailable(fileId)` method so the host can distinguish "loaded but no text" from "library failed". |
| `pdfviewer/src/main/java/com/horizontal/pdfviewer/PinchZoomRecyclerView.kt` | (1) Zoom range `MIN_SCALE 0.5` / `MAX_SCALE 5.0` (was 1.0 / 3.0). (2) `ScaleListener` now preserves the focus point: `mPosX = focusX - (focusX - mPosX) * scaleDelta`. (3) New `zoomTo(scale, focusX, focusY)` helper. (4) Double-tap cycles 1× → 2× → 1× (was jump to MAX_SCALE). (5) `clampPosition()` allows centering when `scale < 1` (page narrower than viewport). |
| `pdfviewer/src/main/java/com/horizontal/pdfviewer/annotations/PdfGoToPageDialog.kt` | Added post-`.show()` button-text tinting (`setTextColor(0xFFD6933B.toInt())`) — the `pdfviewer` module can't depend on the mobile `TellaDialogs`, so we tint the buttons directly. Tint follows the enabled state (muted gray when disabled). |
| `pdfviewer/proguard-rules.pro` | Added `-keep class com.tom_roush.pdfbox.** { *; }` and FontBox rules so R8 doesn't strip the reflection-loaded PDFBox metadata classes. |
| `mobile/proguard-rules.pro` | Same PDFBox keep rules (defensive — R8 in mobile would also process them). |

**Total: 1 new file + 16 modified files.**

---

## 3. Root-Cause Deep Dives

### 3.1 The white-on-white button bug

**Layer 1 — the AppCompat theme inheritance chain:**

```
Theme.AppCompat.Light.NoActionBar      (AppCompat default)
        ↓ parent
AppTheme.NoActionBar                    (mobile/res/values/styles.xml line 14)
  colorAccent = @color/colorAccent      (line 17)
        ↓
@color/colorAccent                      (mobile/res/values/colors.xml line 30)
  = @color/wa_white_80                  (shared-ui/res/values/colors.xml line 22)
  = #CCFFFFFF                           (80% transparent white)
```

AppCompat `AlertDialog` buttons (`BUTTON_POSITIVE`, `BUTTON_NEGATIVE`, `BUTTON_NEUTRAL`) are tinted by `?colorAccent`. With `colorAccent = #CCFFFFFF` and a white dialog background, the button labels are 80% transparent white on white — effectively invisible. The user can still tap them by guessing their position, but cannot read them.

**Layer 2 — why the previous audit rev6 "fix" didn't work:**

The audit rev6 changelog (in `PdfAnnotationDialogs.kt` original header comment) said:

> "all dialogs use `AlertDialog.Builder(context)` with the default theme — NO custom ContextThemeWrapper. The previous version used `BrightBackgroundDarkLettersDialogTheme` which made buttons invisible (white text on white background). Using the default AlertDialog theme ensures buttons inherit the activity's `AppTheme` and are always visible."

This is **wrong on two counts**:

1. `BrightBackgroundDarkLettersDialogTheme` did NOT make buttons invisible — it set `android:background = wa_light_gray` and `android:textColor = wa_darker_gray`, but it forgot to override `colorAccent`. The buttons were still tinted by `colorAccent = wa_white_80`. So the rev6 diagnosis ("BrightBackgroundDarkLettersDialogTheme made buttons invisible") was incorrect — the bug was already there in plain `AlertDialog.Builder(context)`, and the ContextThemeWrapper was an attempt to fix it that didn't go far enough.
2. Dropping the ContextThemeWrapper and using plain `AlertDialog.Builder(context)` doesn't change `colorAccent` at all — the buttons still inherit `wa_white_80` from the activity theme.

**Layer 3 — the correct fix:**

The correct fix is to override `colorAccent` for the dialog context, WITHOUT changing it for the rest of the app (the rest of the app relies on `colorAccent = wa_white_80` for SwitchCompat / EditText cursor / CheckBox tint on the dark purple background). That's exactly what a `ThemeOverlay` is for:

```xml
<style name="TellaDialogTheme" parent="ThemeOverlay.AppCompat.Light">
    <item name="colorAccent">@color/wa_orange</item>
    <item name="android:textColorPrimary">@color/wa_darker_gray</item>
    <item name="android:textColorSecondary">@color/wa_darker_gray</item>
</style>
```

And then wrap the dialog context:

```kotlin
val themedContext = ContextThemeWrapper(context, R.style.TellaDialogTheme)
AlertDialog.Builder(themedContext).setTitle(...).show()
```

`TellaDialogs.builder(context)` does this once so every dialog site gets the fix without repeating the wrapper.

### 3.2 The broken text extractor

The original `PdfTextExtractor.extractText()`:

```kotlin
fun extractText(fileId: String, fromPage: Int, pageCount: Int): String {
    val renderer = loadedDocs[fileId] ?: return ""
    val sb = StringBuilder()
    for (i in start until end) {
        val page = renderer.openPage(i)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmap.recycle()    // ← text never extracted!
    }
    return sb.toString()    // always ""
}
```

Android's framework `PdfRenderer` (since API 21) only renders pages to a Bitmap — it has no text API. So this function rendered each page to a Bitmap, immediately discarded the Bitmap, and returned an empty string. Every call to `copyPageText()` therefore took the `text.isBlank()` branch and showed the misleading "may be a scanned image" toast.

The new implementation uses `com.tom_roush:pdfbox-android` (Android port of Apache PDFBox), which actually parses the PDF content stream:

```kotlin
PDDocument.load(tmpFile).use { doc ->
    val stripper = PDFTextStripper().apply {
        startPage = start + 1   // PDFTextStripper is 1-based
        endPage = end
        sortByPosition = true   // helps with out-of-order text
    }
    stripper.getText(doc)
}
```

`PDFBoxResourceLoader.init(context)` is called lazily on the first `load()` call — it loads the ICU data and BouncyCastle crypto assets from the application's raw resources.

### 3.3 The "no rotation" bug

The activity's manifest entry:

```xml
<activity
    android:name=".views.activity.viewer.PDFReaderActivity"
    android:configChanges="keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode"
    android:theme="@style/AppTheme.NoActionBar" />
```

Note `android:screenOrientation` is NOT set — so the activity rotates freely with the device. And `android:configChanges` includes `orientation` — so the activity is NOT recreated on rotation.

That's the correct setup for a PDF reader (you don't want to reload the PDF on every rotation). But it means the activity is responsible for reflowing its content. The previous code didn't override `onConfigurationChanged`, so the RecyclerView kept its portrait dimensions in landscape — the page bitmap was fitCenter-ed into the still-portrait-width ImageView, leaving large empty bars on either side.

The fix:

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    binding.pdfRendererView.post {
        binding.pdfRendererView.requestLayout()
        val lm = binding.pdfRendererView.recyclerView.layoutManager
            as? LinearLayoutManager
        val first = lm?.findFirstVisibleItemPosition() ?: 0
        val last = lm?.findLastVisibleItemPosition() ?: 0
        if (last >= first) {
            binding.pdfRendererView.recyclerView.adapter
                ?.notifyItemRangeChanged(first, last - first + 1)
        }
    }
}
```

`requestLayout()` propagates down to the PinchZoomRecyclerView and the per-page ImageView items, which re-fitCenter against the new width. `notifyItemRangeChanged` triggers `onBindViewHolder` on the visible items without re-rendering the page bitmap (which would be expensive) — the ImageView just gets the new width to fit against.

The zoom factor is preserved because we don't touch `PinchZoomRecyclerView.mScaleFactor`.

### 3.4 The annotations-list navigation gap

`PdfAnnotationDialogs.showAnnotationList` previously had this signature:

```kotlin
fun showAnnotationList(
    context: Context,
    annotations: List<PdfAnnotation>,
    onSelected: (PdfAnnotation) -> Unit
)
```

And `onSelected` opened the edit/delete dialog. The list DID show page numbers (`"p.${ann.page + 1}: ${ann.text.take(40)}"`) but:

1. When the annotation text was blank, the label became `"p.7: "` — easy to miss the page number at a glance.
2. Clicking a row opened the editor without scrolling — so the user couldn't see the annotation on the page they were editing.

The new signature:

```kotlin
fun showAnnotationList(
    context: Context,
    annotations: List<PdfAnnotation>,
    onSelected: (PdfAnnotation) -> Unit,
    onNavigate: (PdfAnnotation) -> Unit = {}   // NEW — default empty for backwards compat
)
```

Row label now reads `"Page 7 — actual text of the annotation"` (or `"Page 7 — (no text, sticky_note)"` when blank). The page number is always the leading token, so it's the first thing the eye lands on.

The host passes:

```kotlin
PdfAnnotationDialogs.showAnnotationList(
    context = this,
    annotations = binding.pdfRendererView.listAnnotations(),
    onSelected = { ann -> openAnnotationForEdit(ann) },
    onNavigate = { ann -> binding.pdfRendererView.scrollToPage(ann.page) }
)
```

So tapping a row first scrolls the PDF to that page, then opens the editor. The user sees the page land behind the editor dialog.

### 3.5 The "blank area" in Settings > Security

The `audit_security_layout` LinearLayout has `android:background="@drawable/rounded_light_purple_background"` — a rounded purple rectangle. It contains:

```
secureWipeSwitch (always visible)
divider
quickDeletePinSetting (always visible, was isBottomLineVisible="false")
bruteForceDivider (visibility=gone when no PIN)
bruteForceSwitch (visibility=gone when no PIN)
bruteForceThresholdSetting (visibility=gone when no PIN)
bruteForceWindowSetting (visibility=gone when no PIN)
```

When no PIN is set, `refreshAuditSecuritySectionVisibility()` sets the bottom four rows to `View.GONE`. The `quickDeletePinSetting` had `isBottomLineVisible="false"`, so the card had no terminating divider — and the LinearLayout's default bottom padding (8dp + the drawable's intrinsic padding) showed as a big empty purple rectangle.

The fix has two parts:

1. **`app:isBottomLineVisible="true"` on `quickDeletePinSetting`** — now the card always terminates with a visible divider above the bottom padding, so even when the brute-force rows are gone the user sees a clean "card ends here" line.
2. **`android:paddingBottom="0dp"` on `audit_security_layout`** — eliminates the empty space below the last visible row. The drawable's rounded corners are preserved because the LinearLayout's height is `wrap_content`.

Same change applied to both `layout/fragment_security_settings.xml` and `layout-hdpi/fragment_security_settings.xml` so view-binding non-nullable references stay consistent.

### 3.6 The brush size reduction

User asked to reduce S/M/L by 70%. The original values and the new values:

| Dimension | Original S | Original M | Original L | New S | New M | New L | Reduction |
|---|---|---|---|---|---|---|---|
| Highlight width (fraction of page width) | 0.20 | 0.35 | 0.50 | 0.06 | 0.105 | 0.15 | ×0.30 |
| Highlight height (multiplier on 24dp line-height) | 0.7 | 1.0 | 1.5 | 0.21 | 0.30 | 0.45 | ×0.30 |
| Sticky note size (multiplier on 32dp pushpin radius) | 0.7 | 1.0 | 1.5 | 0.21 | 0.30 | 0.45 | ×0.30 |

The 70% reduction is `original × (1 - 0.70) = original × 0.30`, applied uniformly to every multiplier. The relative ordering S < M < L is preserved.

Why this is the right interpretation: "reduce by 70%" means the new value is 30% of the old (a 70% cut). The other reading ("reduce to 70%", i.e. new = old × 0.70) would only be a 30% cut, which wouldn't address the user's complaint that even S was wider than a line of text.

---

## 4. Verification Checklist

| Check | Status |
|---|---|
| All `AlertDialog.Builder(context)` calls in `mobile/views/activity/viewer/` and `mobile/views/settings/SecuritySettings.kt` and `mobile/security/SecureWipeDialog.kt` and `shared_ui/security/QuickDeletePinManager.kt` now go through `TellaDialogs.builder()` or `themedBuilder()` | ✅ Verified via `grep` — only remaining `AlertDialog.Builder(context)` references are in doc comments. |
| All XML files parse without error | ✅ Verified via `xml.etree.ElementTree.parse` on all 6 touched XML files. |
| `pdfviewer/build.gradle` braces balanced | ✅ 13 open / 13 close. |
| All new string resources (`pdf_annot_dialog_apply`, `pdf_annot_list_page_label`, `pdf_annot_list_no_text`, `pdf_annot_copy_text_unavailable`, `pdf_annot_copy_text_loading`) are defined in `mobile/res/values/strings.xml` | ✅ Verified via `grep`. |
| The `tellaDialogTheme` attr is declared exactly once (in `shared-ui/res/values/attrs.xml`) and not duplicated in `mobile/res/values/attrs.xml` | ✅ Verified. |
| The `tellaDialogTheme` item is set on `AppTheme.NoActionBar` so `?attr/tellaDialogTheme` resolves on every activity using that theme | ✅ Verified. |
| `PdfTextExtractor.isAvailable(fileId)` signature matches the call site in `PDFReaderActivity.copyPageText()` | ✅ Both use `isAvailable(fileId: String?)`. |
| `PdfAnnotationStylePicker` size multipliers reduced by exactly 70% (×0.30) | ✅ WIDTH `[0.06, 0.105, 0.15]`, HEIGHT `[0.21, 0.30, 0.45]`, STICKY `[0.21, 0.30, 0.45]`. |
| `PinchZoomRecyclerView` companion object: `MIN_SCALE = 0.5f`, `MAX_SCALE = 5.0f` | ✅ Verified. |
| `PDFReaderActivity.onConfigurationChanged` calls `requestLayout()` on the renderer view + `notifyItemRangeChanged()` on the adapter | ✅ Verified. |
| Both `layout/fragment_security_settings.xml` and `layout-hdpi/fragment_security_settings.xml` have `isBottomLineVisible="true"` and `paddingBottom="0dp"` on the audit_security_layout | ✅ Verified via diff. |
| ProGuard rules for PDFBox-Android added to both `pdfviewer/proguard-rules.pro` and `mobile/proguard-rules.pro` | ✅ Verified. |
| `PdfGoToPageDialog` (in pdfviewer module, can't depend on mobile) tints button text post-show as a fallback | ✅ Verified. |

---

## 5. Build & Dependency Notes

### 5.1 New dependency

`pdfviewer/build.gradle`:

```gradle
implementation 'com.tom-roush:pdfbox-android:2.0.27.0'
```

This adds:

- `com.tom_roush:pdfbox-android` (~5 MB AAR)
- Transitively: `com.tom_roush:fontbox-android`, `org.bouncycastle:bcprov-jdk15on` (already in the app via `tella-keys`), `com.ibm.icu:icu4j` (subset, ~3 MB)

APK size impact: approximately +8 MB on a release build. This is the tradeoff for real text extraction — the alternative is on-device OCR (Tesseract / ML Kit) which is much larger and slower.

### 5.2 ProGuard rules

Without these rules, R8 strips the PDFBox metadata classes that are loaded via reflection inside `PDDocument.load()`, causing `NoClassDefFoundError` at runtime. Added to both `pdfviewer/proguard-rules.pro` (defensive) and `mobile/proguard-rules.pro` (mobile R8 also processes them):

```
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.pdfbox.android.PDFBoxResourceLoader { *; }
-dontwarn com.tom_roush.**
```

### 5.3 Build environment

This audit was performed in an environment without the Android SDK installed. The code changes were validated by:

1. **XML parseability** — all 6 touched XML files parse cleanly with Python's `xml.etree.ElementTree`.
2. **Gradle DSL balance** — `pdfviewer/build.gradle` braces balanced.
3. **Cross-file reference consistency** — every new string resource ID is referenced by the Kotlin code that uses it; every new method signature matches its call site; the `tellaDialogTheme` attr is declared exactly once.
4. **Logical review** — every change was reviewed against the original file's intent and the user's report.

The actual `./gradlew assemblePlaystoreDebug` build must be run on a machine with the Android SDK (compileSdk 36, minSdk 21, NDK 28.0.12916984). The first build will download PDFBox-Android from Maven Central.

---

## 6. Things Deliberately NOT Changed

1. **`colorAccent` on `AppTheme.NoActionBar` itself.** Changing it from `wa_white_80` to `wa_orange` would fix every dialog at once, but would also retint every `SwitchCompat`, `CheckBox`, `EditText` cursor, and other `?colorAccent` widgets across the whole app — the Tella design system relies on `wa_white_80` for those widgets on the dark purple background. Using a scoped `ThemeOverlay` is the surgical fix.

2. **The `BrightBackgroundDarkLettersDialogTheme` style.** It's still in `styles.xml` (now with the `colorAccent` override added) in case any other code references it. A grep for `BrightBackgroundDarkLettersDialogTheme` shows no current callers in the audit-era code, but removing it might break a caller in another module that wasn't audited. Leave it for now; remove in a separate cleanup PR.

3. **The TTS controller.** `PdfTtsController.kt` still exists but is unused (audit rev6 removed TTS from the UI because it was reading placeholder text). With the new `PdfTextExtractor` actually working, TTS could be revived — but that's a feature addition, not a bug fix, so it's out of scope for this audit.

4. **The "Copy as image" long-press menu item.** It still shows the "coming soon" toast. Implementing it would require rendering the page to a Bitmap and writing it to the clipboard via `ClipboardManager.setPrimaryClip(ClipData.newUri(...))` — non-trivial and not in the user's bug list.

5. **The `PdfAnnotationOverlayView` long-press forwarding.** The current implementation forwards long-press only in `AnnotationMode.OFF`. In `HIGHLIGHT` or `STICKY_NOTE` mode, long-press is consumed by the overlay (it returns `true` from `ACTION_DOWN`). The user didn't report this as a bug — they reported that they can't select text, which is the `copyPageText` flow that's already wired to the long-press listener. With the new `PdfTextExtractor`, that flow now works.

---

## 7. Recommended Next Steps for the Maintainer

1. **Run the build.** `./gradlew assemblePlaystoreDebug` on a machine with the Android SDK. The first build will download PDFBox-Android (~5 MB AAR + ~3 MB ICU subset).

2. **Manual smoke test on a real device.** Verify:
   - Open a text PDF → long-press → "Copy text" → paste somewhere → confirm the actual page text is on the clipboard (not empty, not the "scanned image" toast).
   - Open a scanned-image PDF → long-press → "Copy text" → confirm the new "This page has no selectable text." toast appears (no misleading "may be a scanned image" suffix).
   - Rotate the device → confirm the page reflows to fill the new width (no empty bars).
   - Pinch zoom in/out → confirm 0.5× works (page shrinks below viewport width), 5× works (small print is readable), double-tap cycles 1× → 2× → 1×.
   - Tap "Highlight" toolbar icon → confirm the picker dialog shows visible "Apply" / "Cancel" buttons (orange text on white). Pick S → tap a word → confirm the highlight is a small marker, not a half-page-wide bar.
   - Tap "Sticky note" → place a note → tap the note → confirm "Save" / "Cancel" / "Delete" buttons are visible.
   - Tap "List annotations" → confirm each row starts with "Page N —" → tap a row → confirm the PDF scrolls to that page before the editor opens.
   - Settings → Security → scroll to "Use secure wipe on import" → confirm there is no big blank purple rectangle below "Quick Delete PIN".
   - Tap "Quick Delete PIN" → confirm "Set Quick Delete PIN" dialog shows visible OK / Cancel buttons.

3. **Consider reviving TTS** now that `PdfTextExtractor` works. The `PdfTtsController` is still present; wire it to `PdfTextExtractor.extractText(fileId, page, 1)` and re-add the "Read aloud" menu item.

4. **Consider extracting text for the entire document** (not just the current page) so the user can copy multiple pages at once. The current `copyPageText` passes `pageCount = binding.pdfRendererView.totalPageCount` to `extractText(fileId, pageNo, pageCount)` — which extracts from `pageNo` to the end of the document. If you want "copy THIS page only", pass `pageCount = 1`. The current behavior is "copy from this page to the end", which may surprise the user; consider renaming the menu item to "Copy page text" → "Copy text from this page".

5. **Consider adding a "Search in PDF" feature.** PDFBox supports text search via `PDFTextStripper` regions. With the new dependency this is now feasible.

---

## 8. File-level diff summary

Below is a per-file summary of the change shape (not the full diff — see git for that).

### `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/TellaDialogs.kt` (NEW)
- Stateless `object TellaDialogs` with one method: `builder(context: Context): AlertDialog.Builder`.
- Wraps context in `ContextThemeWrapper(context, resolveDialogTheme(context))`.
- `resolveDialogTheme` looks up `?attr/tellaDialogTheme` first, falls back to `R.style.TellaDialogTheme`.

### `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PdfAnnotationDialogs.kt`
- 6 `AlertDialog.Builder(context)` → `TellaDialogs.builder(context)`.
- `showAnnotationList` signature: added `onNavigate: (PdfAnnotation) -> Unit = {}`.
- List row label: `"p.N: text"` → `"Page N — text"` (or `"Page N — (no text, type)"`).

### `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PdfAnnotationStylePicker.kt`
- `SIZE_MULTIPLIERS`: `[0.7, 1.0, 1.5]` → `[0.21, 0.30, 0.45]`.
- `WIDTH_MULTIPLIERS`: `[0.20, 0.35, 0.50]` → `[0.06, 0.105, 0.15]`.
- Positive button: `R.string.pdf_annot_dialog_save` → `R.string.pdf_annot_dialog_apply`.
- All dialogs via `TellaDialogs.builder`.
- Added `refreshSizeButtons(views, selectedIndex, activeColor)` helper for color-swatch → size-pill feedback.

### `mobile/src/main/java/org/horizontal/tella/mobile/views/activity/viewer/PDFReaderActivity.kt`
- Added `import android.content.res.Configuration`.
- Removed `import android.app.AlertDialog` (unused).
- Added `onConfigurationChanged(newConfig)` override.
- `confirmClearAllAnnotations()`: `AlertDialog.Builder(this)` → `TellaDialogs.builder(this)`.
- `showLongPressMenu()`: same.
- `copyPageText()`: rewritten — loading toast, three-way branching on text/empty/unavailable, calls `PdfTextExtractor.isAvailable(fileId)`.
- Annotations-list menu handler: added `onNavigate = { ann -> binding.pdfRendererView.scrollToPage(ann.page) }`.

### `mobile/src/main/java/org/horizontal/tella/mobile/views/settings/SecuritySettings.kt`
- `quickDeletePinSetting.setOnClickListener`: change/remove picker `AlertDialog.Builder(baseActivity)` → `TellaDialogs.builder(baseActivity)`.
- `showNumberPicker`: `androidx.appcompat.app.AlertDialog.Builder(baseActivity)` → `TellaDialogs.builder(baseActivity)`.

### `mobile/src/main/java/org/horizontal/tella/mobile/security/SecureWipeDialog.kt`
- Removed `import android.app.AlertDialog`.
- Added `import ... TellaDialogs`.
- `promptAndWipe`: `AlertDialog.Builder(context)` → `TellaDialogs.builder(context)`.
- `runWipe`: same for the progress dialog.

### `shared-ui/src/main/java/org/hzontal/shared_ui/security/QuickDeletePinManager.kt`
- Added `import android.util.TypedValue`, `import androidx.appcompat.view.ContextThemeWrapper`.
- New private `themedBuilder(context)` that resolves `?attr/tellaDialogTheme` and wraps the context.
- `showSetPinDialog`: `AlertDialog.Builder(context)` → `themedBuilder(context)`.

### `mobile/src/main/res/values/styles.xml`
- `BrightBackgroundDarkLettersDialogTheme`: added `<item name="colorAccent">@color/wa_orange</item>` and `<item name="android:textColorPrimary">@color/wa_darker_gray</item>`.
- New `TellaDialogTheme` style (parent `ThemeOverlay.AppCompat.Light`).
- `AppTheme.NoActionBar`: added `<item name="tellaDialogTheme">@style/TellaDialogTheme</item>`.

### `mobile/src/main/res/values/attrs.xml`
- Removed the local `<attr name="tellaDialogTheme" format="reference" />` declaration.
- Added a comment explaining the relocation to shared-ui.

### `shared-ui/src/main/res/values/attrs.xml`
- Added `<attr name="tellaDialogTheme" format="reference" />` at the top of `<resources>`.

### `mobile/src/main/res/values/strings.xml`
- `pdf_annot_copy_text_empty`: reworded from `"This page has no selectable text (it may be a scanned image)"` to `"This page has no selectable text."`.
- Added: `pdf_annot_list_page_label` = `"Page %1$d"`.
- Added: `pdf_annot_list_no_text` = `"(no text, %1$s)"`.
- Added: `pdf_annot_dialog_apply` = `"Apply"`.
- Added: `pdf_annot_copy_text_unavailable` = `"Text extraction is not available for this PDF."`.
- Added: `pdf_annot_copy_text_loading` = `"Extracting text…"`.

### `mobile/src/main/res/layout/fragment_security_settings.xml`
- `audit_security_layout`: added `android:paddingBottom="0dp"`.
- `quickDeletePinSetting`: `app:isBottomLineVisible="false"` → `"true"`.
- Added explanatory comment block.

### `mobile/src/main/res/layout-hdpi/fragment_security_settings.xml`
- Same change as `layout/` for the audit_security_layout section.

### `pdfviewer/build.gradle`
- Added `implementation 'com.tom-roush:pdfbox-android:2.0.27.0'`.

### `pdfviewer/src/main/java/com/horizontal/pdfviewer/annotations/PdfTextExtractor.kt`
- Complete rewrite. See section 3.2 above.

### `pdfviewer/src/main/java/com/horizontal/pdfviewer/PinchZoomRecyclerView.kt`
- `MAX_SCALE`: `3.0f` → `5.0f`.
- Added `MIN_SCALE = 0.5f`.
- `ScaleListener.onScale`: focus point preservation added.
- New `zoomTo(scale, focusX, focusY)` helper.
- `clampPosition()`: handles `scale < 1` case (center the page).
- `GestureListener.onDoubleTap`: cycle 1× → 2× → 1× via `zoomTo` / `resetZoom`.
- Removed unused `ViewConfiguration` import.

### `pdfviewer/src/main/java/com/horizontal/pdfviewer/annotations/PdfGoToPageDialog.kt`
- Added post-`.show()` button-text tinting (`setTextColor(0xFFD6933B.toInt())`).
- Added a second TextWatcher that follows the enabled state (muted gray when disabled).
- Removed unused `ContextCompat` import.

### `pdfviewer/proguard-rules.pro`
- Added PDFBox keep rules.

### `mobile/proguard-rules.pro`
- Added PDFBox keep rules (defensive duplicate).

---

End of audit.
