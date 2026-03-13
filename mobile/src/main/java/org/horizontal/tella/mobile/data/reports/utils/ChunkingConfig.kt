package org.horizontal.tella.mobile.data.reports.utils

object ChunkingConfig {
    private const val MB = 1024 * 1024L
    const val DEFAULT_CHUNK_SIZE = 1 * MB
    const val MIN_CHUNK_SIZE_WIFI = 5 * MB
    const val MAX_CHUNK_SIZE_WIFI = 10 * MB
    const val MIN_CHUNK_SIZE = 1 * MB
    const val MAX_CHUNK_SIZE = 2 * MB
    const val TARGET_UPLOAD_TIME = 3
    const val SAFETY_FACTOR = 0.6
}