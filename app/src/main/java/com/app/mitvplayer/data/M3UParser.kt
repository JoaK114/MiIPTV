package com.app.mitvplayer.data

/**
 * M3U / M3U_PLUS / M3U8 parser with full compatibility:
 * - Standard #EXTINF attributes (tvg-id, tvg-name, tvg-logo, group-title)
 * - #EXTVLCOPT for http-referrer and http-user-agent
 * - #EXTGRP for alternative group assignment
 * - Double AND single quoted attributes
 * - Playlist-level attributes (url-tvg, x-tvg-url, tvg-name)
 * - catchup, catchup-source, tvg-country, tvg-language
 */
object M3UParser {

    data class ParseResult(
        val channels: List<ParsedChannel>,
        val epgUrl: String? = null,
        val playlistName: String? = null
    )

    data class ParsedChannel(
        val name: String,
        val url: String,
        val duration: Long = -1,
        val logoUrl: String? = null,
        val groupTitle: String? = null,
        val tvgId: String? = null,
        val tvgName: String? = null,
        val httpReferrer: String? = null,
        val httpUserAgent: String? = null
    )

    fun parse(content: String): ParseResult {
        val lines = content.lines()
        val channels = mutableListOf<ParsedChannel>()
        var epgUrl: String? = null
        var playlistName: String? = null

        // Current channel state
        var currentName = ""
        var currentDuration: Long = -1
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentReferrer: String? = null
        var currentUserAgent: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                // ── Header line: #EXTM3U ──
                trimmed.startsWith("#EXTM3U") -> {
                    epgUrl = extractAttribute(trimmed, "url-tvg")
                        ?: extractAttribute(trimmed, "x-tvg-url")
                        ?: extractAttribute(trimmed, "tvg-url")
                    playlistName = extractAttribute(trimmed, "tvg-name")
                        ?: extractAttribute(trimmed, "pltv-name")
                }

                // ── Channel info: #EXTINF ──
                trimmed.startsWith("#EXTINF:") -> {
                    val afterColon = trimmed.substringAfter("#EXTINF:")
                    val commaIndex = findTitleComma(afterColon)

                    if (commaIndex >= 0) {
                        val metaPart = afterColon.substring(0, commaIndex)
                        currentName = afterColon.substring(commaIndex + 1).trim()

                        // Parse duration
                        val durationStr = metaPart.trimStart().split(" ").firstOrNull()?.trim()
                        currentDuration = durationStr?.toLongOrNull() ?: -1

                        // Parse all known attributes
                        currentTvgId = extractAttribute(metaPart, "tvg-id")
                        currentTvgName = extractAttribute(metaPart, "tvg-name")
                        currentLogo = extractAttribute(metaPart, "tvg-logo")
                        currentGroup = extractAttribute(metaPart, "group-title")

                        // Fallback: some m3u_plus use tvg-group instead of group-title
                        if (currentGroup == null) {
                            currentGroup = extractAttribute(metaPart, "tvg-group")
                        }
                    } else {
                        currentName = afterColon.trim()
                    }
                }

                // ── VLC options: #EXTVLCOPT ──
                trimmed.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = trimmed.substringAfter(":", missingDelimiterValue = "")
                    when {
                        option.startsWith("http-referrer=", ignoreCase = true) ->
                            currentReferrer = option.substringAfter("=")
                        option.startsWith("http-user-agent=", ignoreCase = true) ->
                            currentUserAgent = option.substringAfter("=")
                        option.startsWith("http-origin=", ignoreCase = true) ->
                            if (currentReferrer == null) currentReferrer = option.substringAfter("=")
                    }
                }

                // ── Group override: #EXTGRP ──
                trimmed.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val grp = trimmed.substringAfter(":").trim()
                    if (grp.isNotBlank() && currentGroup == null) {
                        currentGroup = grp
                    }
                }

                // ── KODIPROP (alternative header source) ──
                trimmed.startsWith("#KODIPROP:", ignoreCase = true) -> {
                    val prop = trimmed.substringAfter(":", missingDelimiterValue = "")
                    when {
                        prop.startsWith("inputstream.adaptive.stream_headers=", ignoreCase = true) -> {
                            val headers = prop.substringAfter("=")
                            // Parse key=value pairs separated by &
                            headers.split("&").forEach { pair ->
                                val (key, value) = pair.split("=", limit = 2).let {
                                    it[0].lowercase() to (it.getOrNull(1) ?: "")
                                }
                                when (key) {
                                    "referer", "referrer" -> currentReferrer = value
                                    "user-agent" -> currentUserAgent = value
                                }
                            }
                        }
                    }
                }

                // ── Skip other comment lines ──
                trimmed.startsWith("#") -> { /* ignore other directives */ }

                // ── URL line — create channel ──
                else -> {
                    channels.add(
                        ParsedChannel(
                            name = currentName.ifEmpty { "Canal ${channels.size + 1}" },
                            url = trimmed,
                            duration = currentDuration,
                            logoUrl = currentLogo,
                            groupTitle = currentGroup,
                            tvgId = currentTvgId,
                            tvgName = currentTvgName,
                            httpReferrer = currentReferrer,
                            httpUserAgent = currentUserAgent
                        )
                    )

                    // Reset state for next channel
                    currentName = ""
                    currentDuration = -1
                    currentLogo = null
                    currentGroup = null
                    currentTvgId = null
                    currentTvgName = null
                    currentReferrer = null
                    currentUserAgent = null
                }
            }
        }

        return ParseResult(channels, epgUrl, playlistName)
    }

    /**
     * Extract attribute value supporting both double quotes and single quotes.
     * Handles: attribute="value" and attribute='value'
     */
    private fun extractAttribute(line: String, attribute: String): String? {
        // Try double quotes first
        val doubleQuote = Regex(
            """$attribute\s*=\s*"([^"]*?)"""",
            RegexOption.IGNORE_CASE
        )
        doubleQuote.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        // Try single quotes
        val singleQuote = Regex(
            """$attribute\s*=\s*'([^']*?)'""",
            RegexOption.IGNORE_CASE
        )
        return singleQuote.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    /**
     * Find the comma that separates metadata from the title.
     * Handles commas inside both double and single quoted attribute values.
     */
    private fun findTitleComma(text: String): Int {
        var inDoubleQuotes = false
        var inSingleQuotes = false
        for (i in text.indices) {
            when (text[i]) {
                '"' -> if (!inSingleQuotes) inDoubleQuotes = !inDoubleQuotes
                '\'' -> if (!inDoubleQuotes) inSingleQuotes = !inSingleQuotes
                ',' -> if (!inDoubleQuotes && !inSingleQuotes) return i
            }
        }
        return -1
    }
}
