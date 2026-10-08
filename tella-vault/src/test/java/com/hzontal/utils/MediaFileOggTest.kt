package com.hzontal.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileOggTest {

    @Test
    fun oggContainerTypesAreAudio() {
        assertTrue(MediaFile.isAudioFileType("audio/ogg"))
        assertTrue(MediaFile.isAudioFileType("application/ogg"))
        assertTrue(MediaFile.isAudioFileType("audio/opus"))
        assertTrue(MediaFile.isAudioFileType("audio/vorbis"))
        assertTrue(MediaFile.isAudioFileType("audio/oga"))
        assertTrue(MediaFile.isAudioFileType("audio/ogg; codecs=opus"))
        assertTrue(MediaFile.isAudioFileType("AUDIO/OPUS"))
    }

    @Test
    fun oggAliasesAreStoredAsAudioOgg() {
        assertEquals("audio/ogg", MediaFile.normalizedMimeType("audio/opus"))
        assertEquals("audio/ogg", MediaFile.normalizedMimeType("audio/vorbis"))
        assertEquals("audio/ogg", MediaFile.normalizedMimeType("application/ogg"))
        assertEquals("audio/ogg", MediaFile.normalizedMimeType("audio/ogg; codecs=opus"))
        assertEquals("audio/mpeg", MediaFile.normalizedMimeType("audio/mpeg"))
    }

    @Test
    fun otherAudioTypesStayUnchanged() {
        assertTrue(MediaFile.isAudioFileType("audio/mpeg"))
        assertTrue(MediaFile.isAudioFileType("audio/aac"))
        assertFalse(MediaFile.isAudioFileType("video/mp4"))
        assertEquals("audio/ogg", MediaFile.getMimeTypeForFile("voice.ogg"))
        assertEquals("audio/ogg", MediaFile.getMimeTypeForFile("voice.opus"))
    }
}
