package com.app.mitvplayer.data

/**
 * M3U / M3U_PLUS / M3U8 parser with full compatibility:
 * - Standard #EXTINF attributes (tvg-id, tvg-name, tvg-logo, group-title)
 * - #EXTVLCOPT for http-referrer and http-user-agent
 * - #EXTGRP for alternative group assignment
 * - Double AND single quoted attributes
 * - Playlist-level attributes (url-tvg, x-tvg-url, tvg-name)
 * - catchup, catchup-source, tvg-country, tvg-language
 * - Auto-classification: TV / Movie / Series
 * - Series name & episode extraction for folder grouping
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
        val httpUserAgent: String? = null,
        val contentType: String = "tv",       // "tv", "movie", "series"
        val seriesName: String? = null,
        val seasonNum: Int = 0,
        val episodeNum: Int = 0
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
                    // Classify content type based on group title
                    val contentType = classifyGroup(currentGroup ?: "")

                    // Extract series info if it's a series
                    val episodeInfo = if (contentType == "series") {
                        parseEpisodeInfo(currentName)
                    } else null

                    val seriesName = if (contentType == "series") {
                        extractSeriesName(currentName, episodeInfo)
                    } else null

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
                            httpUserAgent = currentUserAgent,
                            contentType = contentType,
                            seriesName = seriesName,
                            seasonNum = episodeInfo?.season ?: 0,
                            episodeNum = episodeInfo?.episode ?: 0
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

    // ═══════════════════════════════════════════════════════
    // Content classification
    // ═══════════════════════════════════════════════════════

    /**
     * Classify a group title into content type: "tv", "movie", or "series".
     */
    fun classifyGroup(group: String): String {
        val lower = group.lowercase()
        return when {
            // Movies
            lower.contains("pelicula") || lower.contains("película") ||
            lower.contains("movie") || lower.contains("cine") ||
            lower.contains("film") || (lower.contains("vod") && lower.contains("movie")) ||
            lower.contains("peli ") || lower.contains("peliculas") ||
            lower.contains("películas") -> "movie"

            // Series
            lower.contains("serie") || lower.contains("novela") ||
            lower.contains("temporada") || lower.contains("season") ||
            (lower.contains("show") && !lower.contains("en vivo")) ||
            lower.contains("series") -> "series"

            // TV (everything else)
            else -> "tv"
        }
    }

    // ═══════════════════════════════════════════════════════
    // Episode info extraction
    // ═══════════════════════════════════════════════════════

    data class EpisodeInfo(
        val season: Int,
        val episode: Int,
        val label: String,
        val matchStart: Int  // Position where the pattern starts in the name
    )

    /**
     * Parse episode info from channel name.
     * Handles: S01E05, s1e5, 1x05, T1E5, Temporada 1 Episodio 5,
     * Cap 5, Capitulo 5, Ep 5, E05, Episode 5
     */
    fun parseEpisodeInfo(name: String): EpisodeInfo? {
        val lower = name.lowercase().trim()

        // Pattern: S01E05, s1e5
        Regex("""s(\d{1,3})\s*e(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val s = it.groupValues[1].toInt()
                val e = it.groupValues[2].toInt()
                return EpisodeInfo(s, e, "T${s}:E${e}", it.range.first)
            }

        // Pattern: 1x05
        Regex("""(\d{1,2})x(\d{1,4})""")
            .find(lower)?.let {
                val s = it.groupValues[1].toInt()
                val e = it.groupValues[2].toInt()
                return EpisodeInfo(s, e, "T${s}:E${e}", it.range.first)
            }

        // Pattern: T1E5, T01E05
        Regex("""t(\d{1,3})\s*e(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val s = it.groupValues[1].toInt()
                val e = it.groupValues[2].toInt()
                return EpisodeInfo(s, e, "T${s}:E${e}", it.range.first)
            }

        // Pattern: Temporada 1 ... Episodio 5 / Capitulo 5
        Regex("""temporada\s*(\d{1,3}).*?(?:episodio|capitulo|cap|ep)\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val s = it.groupValues[1].toInt()
                val e = it.groupValues[2].toInt()
                return EpisodeInfo(s, e, "T${s}:E${e}", it.range.first)
            }

        // Pattern: Season 1 Episode 5
        Regex("""season\s*(\d{1,3}).*?episode\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val s = it.groupValues[1].toInt()
                val e = it.groupValues[2].toInt()
                return EpisodeInfo(s, e, "T${s}:E${e}", it.range.first)
            }

        // Pattern: Ep 5, Ep. 5, Ep05, Episode 5, Episodio 5, Cap 5, Capitulo 5
        Regex("""(?:ep\.?\s*|episode\s*|episodio\s*|cap(?:itulo)?\.?\s*)(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val e = it.groupValues[1].toInt()
                return EpisodeInfo(1, e, "Ep $e", it.range.first)
            }

        // Pattern: just "E05" or "E5" standalone
        Regex("""\be(\d{1,4})\b""", RegexOption.IGNORE_CASE)
            .find(lower)?.let {
                val e = it.groupValues[1].toInt()
                return EpisodeInfo(1, e, "Ep $e", it.range.first)
            }

        return null
    }

    /**
     * Extract the clean series name from a channel name by removing the episode pattern.
     *
     * Examples:
     *   "Breaking Bad S01E05" → "Breaking Bad"
     *   "La Casa de Papel T1E3" → "La Casa de Papel"
     *   "Stranger Things 1x01 - Chapter One" → "Stranger Things"
     *   "Game of Thrones - Season 2 Episode 5" → "Game of Thrones"
     */
    fun extractSeriesName(channelName: String, episodeInfo: EpisodeInfo?): String {
        if (episodeInfo == null) {
            // No episode pattern found — use the full name as series name
            // But try to clean common suffixes
            return cleanSeriesName(channelName)
        }

        // Get everything before the episode pattern match position
        val name = channelName.trim()

        // Find the pattern in the original name (case insensitive)
        val patterns = listOf(
            Regex("""[\s\-–—.|]*[Ss]\d{1,3}\s*[Ee]\d{1,4}.*$"""),
            Regex("""[\s\-–—.|]*\d{1,2}x\d{1,4}.*$"""),
            Regex("""[\s\-–—.|]*[Tt]\d{1,3}\s*[Ee]\d{1,4}.*$"""),
            Regex("""[\s\-–—.|]*[Tt]emporada\s*\d.*$""", RegexOption.IGNORE_CASE),
            Regex("""[\s\-–—.|]*[Ss]eason\s*\d.*$""", RegexOption.IGNORE_CASE),
            Regex("""[\s\-–—.|]*[Ee]p(?:isode|isodio)?\.?\s*\d+.*$""", RegexOption.IGNORE_CASE),
            Regex("""[\s\-–—.|]*[Cc]ap(?:itulo)?\.?\s*\d+.*$""", RegexOption.IGNORE_CASE),
            Regex("""[\s\-–—.|]*\bE\d{1,4}\b.*$""")
        )

        for (pattern in patterns) {
            val result = pattern.find(name)
            if (result != null && result.range.first > 0) {
                return cleanSeriesName(name.substring(0, result.range.first))
            }
        }

        return cleanSeriesName(name)
    }

    /**
     * Clean up a series name: trim, remove trailing separators, normalize whitespace.
     */
    private fun cleanSeriesName(name: String): String {
        return name
            .trim()
            .trimEnd('-', '–', '—', '.', '|', ':', ' ', '_')
            .trim()
            .replace(Regex("""\s+"""), " ")
            .ifBlank { name.trim() }
    }

    // ═══════════════════════════════════════════════════════
    // Attribute extraction helpers
    // ═══════════════════════════════════════════════════════

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
