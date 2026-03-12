package org.horizontal.tella.mobile.data.reports.utils

object ChunkingConfig {
    const val DEFAULT_CHUNK_SIZE = 1 * 1024 * 1024L
    const val MIN_CHUNK_SIZE_WIFI = 5 * 1024 * 1024L
    const val MAX_CHUNK_SIZE_WIFI = 10 * 1024 * 1024L
    const val MIN_CHUNK_SIZE = 1 * 1024 * 1024L
    const val MAX_CHUNK_SIZE = 2 * 1024 * 1024L
    const val TARGET_UPLOAD_TIME = 3
    const val SAFETY_FACTOR = 0.6
}