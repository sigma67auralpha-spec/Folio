package com.example.engine

import android.util.Base64
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object DocxRtfFb2Parser {

    private const val MAX_DOC_BYTES = 32 * 1024 * 1024

    private fun newSecureXmlPullParser(): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        return factory.newPullParser()
    }

    private fun readBoundedStream(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            total += read
            if (total > maxBytes) error("Document XML exceeds safe size limit.")
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    fun parseDocx(file: File, fallbackTitle: String): ParsedBookContent {
        return runCatching {
            ZipFile(file).use { zip ->
                val docEntry = zip.getEntry("word/document.xml")
                    ?: return ParsedBookContent.Error("Invalid DOCX: missing word/document.xml")
                val docBytes = zip.getInputStream(docEntry).use { readBoundedStream(it, MAX_DOC_BYTES) }
                var title = fallbackTitle
                var author = "Unknown Author"
                zip.getEntry("docProps/core.xml")?.let { core ->
                    val coreBytes = zip.getInputStream(core).use { readBoundedStream(it, 2 * 1024 * 1024) }
                    val (t, a) = parseDocxCoreProps(coreBytes)
                    if (!t.isNullOrBlank()) title = t
                    if (!a.isNullOrBlank()) author = a
                }

                val blocks = parseDocxXml(docBytes)
                val chapters = TextHtmlMdParser.splitBlocksIntoChapters(blocks, title)
                ParsedBookContent.Reflowable(
                    metadata = ParsedMetadata(title = title, author = author, totalUnits = chapters.size),
                    chapters = chapters
                )
            }
        }.getOrElse {
            ParsedBookContent.Error("Failed to read DOCX: ${it.localizedMessage ?: "invalid file"}")
        }
    }

    private fun parseDocxCoreProps(bytes: ByteArray): Pair<String?, String?> {
        var title: String? = null
        var author: String? = null
        val parser = newSecureXmlPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name.lowercase()) {
                    "title" -> title = parser.nextText().trim()
                    "creator" -> author = parser.nextText().trim()
                }
            }
            event = parser.next()
        }
        return title to author
    }

    private fun parseDocxXml(bytes: ByteArray): List<ParagraphBlock> {
        val blocks = mutableListOf<ParagraphBlock>()
        val parser = newSecureXmlPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        val textBuilder = StringBuilder()
        val runs = mutableListOf<TextRun>()
        var currentStyle = BlockStyle.BODY
        var inParagraph = false
        var bold = false
        var italic = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "p" -> {
                            inParagraph = true
                            textBuilder.clear()
                            runs.clear()
                            currentStyle = BlockStyle.BODY
                        }
                        "pStyle" -> {
                            val valAttr = parser.getAttributeValue(null, "val")
                                ?: parser.getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")
                                ?: ""
                            currentStyle = when {
                                valAttr.contains("Title", true) || valAttr.contains("Heading1", true) -> BlockStyle.HEADING_1
                                valAttr.contains("Heading2", true) -> BlockStyle.HEADING_2
                                valAttr.contains("Heading3", true) -> BlockStyle.HEADING_3
                                valAttr.contains("Quote", true) -> BlockStyle.BLOCKQUOTE
                                else -> BlockStyle.BODY
                            }
                        }
                        "r" -> {
                            bold = false
                            italic = false
                        }
                        "b" -> bold = true
                        "i" -> italic = true
                        "t" -> if (inParagraph) {
                            val chunk = parser.nextText()
                            if (chunk.isNotEmpty()) {
                                val start = textBuilder.length
                                textBuilder.append(chunk)
                                if (bold || italic) {
                                    runs.add(TextRun(start, textBuilder.length, bold = bold, italic = italic))
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "p" && inParagraph) {
                        inParagraph = false
                        val text = textBuilder.toString().trim()
                        if (text.isNotEmpty()) {
                            blocks.add(ParagraphBlock(text, currentStyle, runs.toList()))
                        }
                    }
                }
            }
            event = parser.next()
        }
        return blocks
    }

    fun parseRtf(rawBytes: ByteArray, fallbackTitle: String): ParsedBookContent.Reflowable {
        val raw = String(rawBytes, Charsets.ISO_8859_1)
        val out = StringBuilder()
        var skipDepth = 0
        var groupDepth = 0
        val skipKeywords = setOf("fonttbl", "colortbl", "stylesheet", "info", "pict", "object", "header", "footer")
        var i = 0
        while (i < raw.length) {
            when (val ch = raw[i]) {
                '{' -> {
                    groupDepth++
                    i++
                }
                '}' -> {
                    if (skipDepth == groupDepth) skipDepth = 0
                    groupDepth = (groupDepth - 1).coerceAtLeast(0)
                    i++
                }
                '\\' -> {
                    if (i + 1 >= raw.length) break
                    val next = raw[i + 1]
                    if (next == '\\' || next == '{' || next == '}') {
                        if (skipDepth == 0) out.append(next)
                        i += 2
                    } else if (next == '\'') {
                        val hex = raw.substring((i + 2).coerceAtMost(raw.length), (i + 4).coerceAtMost(raw.length))
                        val code = hex.toIntOrNull(16)
                        if (skipDepth == 0 && code != null) out.append(code.toChar())
                        i += 4
                    } else if (next == '*') {
                        if (skipDepth == 0) skipDepth = groupDepth
                        i += 2
                    } else {
                        var j = i + 1
                        while (j < raw.length && raw[j].isLetter()) j++
                        val word = raw.substring(i + 1, j)
                        var k = j
                        if (k < raw.length && (raw[k] == '-' || raw[k].isDigit())) {
                            if (raw[k] == '-') k++
                            while (k < raw.length && raw[k].isDigit()) k++
                        }
                        val param = raw.substring(j, k)
                        if (k < raw.length && raw[k] == ' ') k++

                        if (word in skipKeywords && skipDepth == 0) {
                            skipDepth = groupDepth
                        } else if (skipDepth == 0) {
                            when (word) {
                                "par", "line" -> out.append("\n\n")
                                "tab" -> out.append("    ")
                                "u" -> param.toIntOrNull()?.let { u ->
                                    out.append((if (u < 0) u + 65536 else u).toChar())
                                    if (k < raw.length && raw[k] == '?') k++
                                }
                                "emdash" -> out.append("—")
                                "endash" -> out.append("–")
                                "lquote" -> out.append("‘")
                                "rquote" -> out.append("’")
                                "ldblquote" -> out.append("“")
                                "rdblquote" -> out.append("”")
                            }
                        }
                        i = k
                    }
                }
                '\r', '\n' -> i++
                else -> {
                    if (skipDepth == 0) out.append(ch)
                    i++
                }
            }
        }
        return TextHtmlMdParser.parsePlainText(out.toString(), fallbackTitle)
    }

    fun parseFb2(file: File, fallbackTitle: String): ParsedBookContent {
        return runCatching {
            val xmlBytes = if (file.name.lowercase().endsWith(".zip")) {
                ZipInputStream(file.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && entry.isDirectory) entry = zis.nextEntry
                    if (entry == null) error("Empty FB2 archive.")
                    readBoundedStream(zis, MAX_DOC_BYTES)
                }
            } else {
                file.inputStream().use { readBoundedStream(it, MAX_DOC_BYTES) }
            }
            val parser = newSecureXmlPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), null)

            var title = fallbackTitle
            val authorParts = mutableListOf<String>()
            val description = StringBuilder()
            var coverId: String? = null
            val binaries = mutableMapOf<String, String>()
            val chapters = mutableListOf<BookChapter>()

            var inTitleInfo = false
            var inAnnotation = false
            var inBody = false
            var currentBlocks = mutableListOf<ParagraphBlock>()
            var currentSectionTitle: String? = null
            var inSectionTitle = false
            var inCite = false
            var currentTag = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.lowercase()
                        when (currentTag) {
                            "title-info" -> inTitleInfo = true
                            "annotation" -> if (inTitleInfo) inAnnotation = true
                            "body" -> inBody = true
                            "section" -> if (inBody && currentBlocks.isNotEmpty()) {
                                chapters.add(
                                    BookChapter(
                                        chapters.size,
                                        currentSectionTitle ?: "Chapter ${chapters.size + 1}",
                                        currentBlocks.toList()
                                    )
                                )
                                currentBlocks = mutableListOf()
                                currentSectionTitle = null
                            }
                            "title" -> if (inBody) inSectionTitle = true
                            "cite" -> if (inBody) inCite = true
                            "image" -> if (inTitleInfo && coverId == null) {
                                val href = parser.getAttributeValue(null, "href")
                                    ?: parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                                coverId = href?.removePrefix("#")
                            }
                            "binary" -> {
                                val id = parser.getAttributeValue(null, "id")
                                val base64 = parser.nextText().trim()
                                if (!id.isNullOrBlank()) binaries[id] = base64
                            }
                            "book-title" -> if (inTitleInfo) title = parser.nextText().trim()
                            "first-name", "middle-name", "last-name" -> if (inTitleInfo) {
                                val part = parser.nextText().trim()
                                if (part.isNotEmpty()) authorParts.add(part)
                            }
                            "p", "v" -> {
                                val txt = readTagText(parser).trim()
                                if (txt.isNotEmpty()) {
                                    when {
                                        inAnnotation -> description.append(txt).append(' ')
                                        inSectionTitle -> {
                                            if (currentSectionTitle == null) currentSectionTitle = txt
                                            currentBlocks.add(ParagraphBlock(txt, BlockStyle.HEADING_2))
                                        }
                                        inBody -> currentBlocks.add(
                                            ParagraphBlock(txt, if (inCite) BlockStyle.BLOCKQUOTE else BlockStyle.BODY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name.lowercase()) {
                            "title-info" -> inTitleInfo = false
                            "annotation" -> inAnnotation = false
                            "title" -> inSectionTitle = false
                            "cite" -> inCite = false
                            "body" -> {
                                inBody = false
                                if (currentBlocks.isNotEmpty()) {
                                    chapters.add(
                                        BookChapter(
                                            chapters.size,
                                            currentSectionTitle ?: "Chapter ${chapters.size + 1}",
                                            currentBlocks.toList()
                                        )
                                    )
                                    currentBlocks = mutableListOf()
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }

            val coverBytes = coverId?.let { binaries[it] }?.let { b64 ->
                runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
            }
            val finalChapters = chapters.ifEmpty {
                listOf(BookChapter(0, title, listOf(ParagraphBlock("(Empty FB2 book)", BlockStyle.BODY))))
            }
            ParsedBookContent.Reflowable(
                metadata = ParsedMetadata(
                    title = title,
                    author = authorParts.joinToString(" ").ifBlank { "Unknown Author" },
                    description = description.toString().trim(),
                    coverBytes = coverBytes,
                    totalUnits = finalChapters.size
                ),
                chapters = finalChapters
            )
        }.getOrElse {
            ParsedBookContent.Error("Failed to read FB2: ${it.localizedMessage ?: "invalid XML"}")
        }
    }

    private fun readTagText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString()
    }
}
