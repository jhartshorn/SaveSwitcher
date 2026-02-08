package com.saveswitcher.util

import java.util.Locale

object UserIdNormalizer {
    private val disallowedRegex = Regex("[^a-z0-9_]")
    private val spacesRegex = Regex("\\s+")

    fun normalize(displayName: String): String {
        val collapsed = displayName
            .trim()
            .lowercase(Locale.US)
            .replace(spacesRegex, "_")
            .replace(disallowedRegex, "")
            .replace(Regex("_+"), "_")
            .trim('_')

        return collapsed.take(30)
    }
}
