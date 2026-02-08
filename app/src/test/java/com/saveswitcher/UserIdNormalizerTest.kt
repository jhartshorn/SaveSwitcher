package com.saveswitcher

import com.saveswitcher.util.UserIdNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class UserIdNormalizerTest {
    @Test
    fun normalize_removesUnsupportedCharsAndCollapsesSpaces() {
        val result = UserIdNormalizer.normalize("  Liz & James  ")
        assertEquals("liz_james", result)
    }

    @Test
    fun normalize_limitsLength() {
        val result = UserIdNormalizer.normalize("abcdefghijklmnopqrstuvwxyz0123456789")
        assertEquals(30, result.length)
    }
}
