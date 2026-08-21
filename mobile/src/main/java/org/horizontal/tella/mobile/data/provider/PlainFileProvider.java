package org.horizontal.tella.mobile.data.provider;

import androidx.core.content.FileProvider;

/**
 * 2025-08-20 (audit-fix rev 9): Plain FileProvider for sharing NON-encrypted
 * files written to the app's cache dir.
 *
 * ## Why a separate provider?
 *
 * The existing [EncryptedFileProvider] overrides `openFile()` to intercept
 * every URI and spawn a `ReadThread` that calls
 * `MyApplication.keyRxVault.getRxVault().getStream(filename)` — i.e. it
 * assumes every URI points to a file IN THE ENCRYPTED VAULT.
 *
 * The flattened (annotation-baked) PDF written by
 * `PdfAnnotationFlattener.flatten()` lives in `context.cacheDir`, NOT in
 * the vault. When the share intent resolves, the recipient app opens the
 * content URI via `EncryptedFileProvider.openFile()` → the `ReadThread`
 * tries `rxVault.getStream("document_annotated.pdf")` → `VaultException`
 * (no such file in the vault) → the thread bails out having written ZERO
 * bytes to the pipe. The recipient app sees a 0-byte file and reports
 * "not a document".
 *
 * This [PlainFileProvider] is a stock `FileProvider` with NO `openFile()`
 * override. It just serves the file on disk directly via the framework's
 * default content:// machinery. We register it in the manifest with a
 * separate authority (`${applicationId}.PlainFileProvider`) and a separate
 * paths config (`@xml/plain_file_paths`) that only exposes the cache dir.
 *
 * The flatten/share path in `PDFReaderActivity.sharePdfWithAnnotations`
 * now uses THIS authority instead of `EncryptedFileProvider`.
 */
public class PlainFileProvider extends FileProvider {
    // Authority is `${applicationId}.PlainFileProvider` — declared in AndroidManifest.xml.
    // No openFile() override — the framework default reads the file from disk.
}
