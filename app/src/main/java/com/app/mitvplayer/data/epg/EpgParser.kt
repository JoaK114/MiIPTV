package com.app.mitvplayer.data.epg

import com.app.mitvplayer.data.models.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

/**
 * Streaming XMLTV parser that processes EPG data asynchronously.
 * Supports both .xml and .xml.gz files.
 * Uses XmlPullParser for memory-efficient SAX-style parsing.
 */
object EpgParser {

    /**
     * Parse an XMLTV stream. Emits batches of parsed programs.
     * The flow runs on Dispatchers.IO.
     *
     * @param inputStream raw input (will detect gzip automatically)
     * @param isGzipped whether the stream is GZip compressed
     * @param batchSize number of programs to buffer before emitting
     */
    fun parse(
        inputStream: InputStream,
        isGzipped: Boolean = false,
        batchSize: Int = 200
    ): Flow<List<EpgProgram>> = flow {
        val stream = if (isGzipped) GZIPInputStream(inputStream) else inputStream

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream.bufferedReader())

        val batch = mutableListOf<EpgProgram>()

        // Current programme state
        var inProgramme = false
        var channelId = ""
        var startTime = 0L
        var stopTime = 0L
        var currentTag = ""
        var title = ""
        var description = ""
        var category = ""
        var icon = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            inProgramme = true
                            channelId = parser.getAttributeValue(null, "channel") ?: ""
                            startTime = parseXmltvDate(parser.getAttributeValue(null, "start") ?: "")
                            stopTime = parseXmltvDate(parser.getAttributeValue(null, "stop") ?: "")
                            title = ""
                            description = ""
                            category = ""
                            icon = ""
                        }
                        "icon" -> {
                            if (inProgramme) {
                                icon = parser.getAttributeValue(null, "src") ?: ""
                            }
                        }
                        else -> {
                            if (inProgramme) {
                                currentTag = parser.name
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inProgramme) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "title" -> title = text
                            "desc" -> description = text
                            "category" -> {
                                if (category.isEmpty()) category = text
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && inProgramme) {
                        inProgramme = false
                        currentTag = ""

                        if (channelId.isNotBlank() && startTime > 0 && title.isNotBlank()) {
                            batch.add(
                                EpgProgram(
                                    channelEpgId = channelId,
                                    startTime = startTime,
                                    stopTime = stopTime,
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    category = category.takeIf { it.isNotBlank() },
                                    icon = icon.takeIf { it.isNotBlank() }
                                )
                            )

                            if (batch.size >= batchSize) {
                                emit(batch.toList())
                                batch.clear()
                            }
                        }
                    } else {
                        currentTag = ""
                    }
                }
            }
            eventType = parser.next()
        }

        // Emit remaining
        if (batch.isNotEmpty()) {
            emit(batch.toList())
        }

        stream.close()
    }.flowOn(Dispatchers.IO)

    /**
     * Parse from a URL (supports both .xml and .xml.gz).
     */
    fun parseFromUrl(url: String): Flow<List<EpgProgram>> = flow {
        val connection = java.net.URL(url).openConnection().apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "MiTVPlayer/1.0 (Android)")
            setRequestProperty("Accept-Encoding", "gzip")
        }

        val isGzip = url.endsWith(".gz", ignoreCase = true) ||
            connection.contentEncoding?.equals("gzip", ignoreCase = true) == true

        val inputStream = connection.getInputStream()

        parse(inputStream, isGzip).collect { batch ->
            emit(batch)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse XMLTV date format: "20240115120000 +0000"
     * Returns epoch millis, or 0 if parsing fails.
     */
    private fun parseXmltvDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0

        val formats = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmss",
            "yyyyMMddHHmm Z",
            "yyyyMMddHHmm"
        )

        // Normalize: insert space before timezone sign if not already there
        val normalized = dateStr.trim().let { s ->
            if (s.length >= 15 && s[14] != ' ' && (s[14] == '+' || s[14] == '-')) {
                s.substring(0, 14) + " " + s.substring(14)
            } else s
        }

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(normalized)?.time ?: continue
            } catch (_: Exception) {
                continue
            }
        }

        return 0
    }
}
