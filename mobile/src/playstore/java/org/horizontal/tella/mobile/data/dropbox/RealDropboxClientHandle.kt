package org.horizontal.tella.mobile.data.dropbox

import com.dropbox.core.v2.DbxClientV2
import org.horizontal.tella.mobile.domain.repository.dropbox.DropboxClientHandle

/**
 * Playstore implementation of [DropboxClientHandle] that holds the Dropbox SDK client.
 */
class RealDropboxClientHandle(val client: DbxClientV2) : DropboxClientHandle
