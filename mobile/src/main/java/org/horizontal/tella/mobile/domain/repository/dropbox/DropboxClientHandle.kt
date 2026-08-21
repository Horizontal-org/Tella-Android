package org.horizontal.tella.mobile.domain.repository.dropbox

/**
 * Opaque handle for a Dropbox client. Used in main/fdroid so that no Dropbox SDK types are referenced.
 * Playstore provides the real implementation that holds the SDK client.
 */
interface DropboxClientHandle
